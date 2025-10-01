/**
 * Solora RESTful API
 * Firebase Cloud Functions for solar quote calculations and data management
 */

const functions = require("firebase-functions");
const admin = require("firebase-admin");
const axios = require("axios");

admin.initializeApp();
const db = admin.firestore();

// ============================================
// QUOTE CALCULATION WITH NASA API
// ============================================
exports.calculateQuote = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  const {address, usageKwh, billRands, tariff, panelWatt, latitude, longitude} = data;

  console.log(`Calculate quote request: lat=${latitude}, lon=${longitude}, panelWatt=${panelWatt}`);

  try {
    // Fetch NASA solar data if coordinates provided
    let nasaData = null;
    let avgSunHours = 5.0; // Default

    if (latitude && longitude) {
      try {
        const nasaUrl = `https://power.larc.nasa.gov/api/temporal/monthly/point` +
          `?parameters=ALLSKY_SFC_SW_DWN&community=RE` +
          `&latitude=${latitude}&longitude=${longitude}` +
          `&start=2023&end=2023&format=JSON`;

        console.log(`Fetching NASA data from: ${nasaUrl}`);
        const nasaResponse = await axios.get(nasaUrl);

        const solarIrradiance = nasaResponse.data &&
          nasaResponse.data.properties &&
          nasaResponse.data.properties.parameter &&
          nasaResponse.data.properties.parameter.ALLSKY_SFC_SW_DWN;
        if (solarIrradiance) {
          const monthlyValues = Object.values(solarIrradiance).filter((v) => typeof v === "number");
          const avgIrradiance = monthlyValues.reduce((a, b) => a + b, 0) / monthlyValues.length;
          avgSunHours = avgIrradiance / 1.0;

          nasaData = {
            averageAnnualIrradiance: avgIrradiance,
            averageAnnualSunHours: avgSunHours,
            latitude,
            longitude,
          };
          console.log(`NASA data fetched: irradiance=${avgIrradiance}, sunHours=${avgSunHours}`);
        }
      } catch (nasaError) {
        console.warn(`NASA API failed: ${nasaError.message}`);
      }
    }

    // Calculate quote
    const monthlyUsage = usageKwh || (billRands / tariff);
    const dailyUsage = monthlyUsage / 30;
    const systemKw = dailyUsage / avgSunHours;
    const panelKw = panelWatt / 1000;
    const panels = Math.ceil(systemKw / panelKw);
    const inverterKw = systemKw * 0.8;
    const monthlySavings = monthlyUsage * tariff * 0.8; // 80% performance ratio
    const estimatedGeneration = systemKw * avgSunHours * 30;
    const installationCost = systemKw * 15000;
    const paybackMonths = monthlySavings > 0 ? Math.round(installationCost / monthlySavings) : 0;

    const calculation = {
      panels,
      systemKwp: parseFloat(systemKw.toFixed(2)),
      inverterKw: parseFloat(inverterKw.toFixed(2)),
      monthlySavings: parseFloat(monthlySavings.toFixed(2)),
      estimatedGeneration: parseFloat(estimatedGeneration.toFixed(2)),
      paybackMonths,
      nasaData,
      usageKwh: monthlyUsage,
      billRands,
      tariff,
      panelWatt,
    };

    console.log(`Calculation complete: ${systemKw}kW system, ${panels} panels, R${monthlySavings} savings`);
    return {success: true, calculation};
  } catch (error) {
    console.error(`Calculate quote error: ${error.message}`);
    throw new functions.https.HttpsError("internal", error.message);
  }
});

// ============================================
// SAVE QUOTE TO FIRESTORE
// ============================================
exports.saveQuote = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  const userId = context.auth.uid;

  // Get user settings for company info
  let companyInfo = {};
  try {
    const settingsDoc = await db.collection("user_settings").doc(userId).get();
    if (settingsDoc.exists) {
      const settings = settingsDoc.data();
      companyInfo = {
        companyName: settings.companyName || "",
        companyPhone: settings.companyPhone || "",
        companyEmail: settings.companyEmail || "",
        consultantName: settings.consultantName || "",
        consultantPhone: settings.consultantPhone || "",
        consultantEmail: settings.consultantEmail || "",
      };
    }
  } catch (err) {
    console.warn(`Failed to fetch settings: ${err.message}`);
  }

  const quote = {
    ...data,
    ...companyInfo,
    userId,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };

  try {
    const docRef = await db.collection("quotes").add(quote);
    console.log(`Quote saved: ${docRef.id} for user: ${userId}`);
    return {success: true, quoteId: docRef.id};
  } catch (error) {
    console.error(`Save quote error: ${error.message}`);
    throw new functions.https.HttpsError("internal", error.message);
  }
});

// ============================================
// SAVE LEAD TO FIRESTORE
// ============================================
exports.saveLead = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  const userId = context.auth.uid;
  const lead = {
    ...data,
    userId,
    createdAt: admin.firestore.FieldValue.serverTimestamp(),
    updatedAt: admin.firestore.FieldValue.serverTimestamp(),
  };

  try {
    const docRef = await db.collection("leads").add(lead);
    console.log(`Lead saved: ${docRef.id} for user: ${userId}`);
    return {success: true, leadId: docRef.id};
  } catch (error) {
    console.error(`Save lead error: ${error.message}`);
    throw new functions.https.HttpsError("internal", error.message);
  }
});

// ============================================
// GET QUOTE BY ID FROM FIRESTORE
// ============================================
exports.getQuoteById = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  const userId = context.auth.uid;
  const {quoteId} = data;

  if (!quoteId) {
    throw new functions.https.HttpsError("invalid-argument", "Quote ID is required");
  }

  try {
    console.log(`Getting quote ${quoteId} for user ${userId}`);

    const quoteDoc = await db.collection("quotes").doc(quoteId).get();

    if (quoteDoc.exists) {
      const quoteData = quoteDoc.data();

      // Verify the quote belongs to the current user
      if (quoteData.userId === userId) {
        console.log(`Quote ${quoteId} found and belongs to user ${userId}`);
        return {
          success: true,
          quote: {...quoteData, id: quoteDoc.id},
        };
      } else {
        console.log(`Quote ${quoteId} does not belong to user ${userId}`);
        throw new functions.https.HttpsError("permission-denied", "Quote not found or access denied");
      }
    } else {
      console.log(`Quote ${quoteId} not found`);
      return {success: true, quote: null};
    }
  } catch (error) {
    console.error(`Get quote by ID error: ${error.message}`);
    throw new functions.https.HttpsError("internal", error.message);
  }
});

// ============================================
// GET LEADS (with search/filter support)
// ============================================
exports.getLeads = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  const userId = context.auth.uid;
  const {search, status, limit = 50} = data || {};

  try {
    console.log(`Getting leads for user ${userId}, search: ${search}, status: ${status}`);

    let query = db.collection("leads")
        .where("userId", "==", userId)
        .orderBy("createdAt", "desc")
        .limit(limit);

    // Apply status filter if provided
    if (status && status !== "all") {
      query = query.where("status", "==", status);
    }

    const snapshot = await query.get();
    let leads = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...doc.data(),
    }));

    // Apply search filter if provided
    if (search && search.trim()) {
      const searchTerm = search.toLowerCase();
      leads = leads.filter((lead) =>
        lead.name.toLowerCase().includes(searchTerm) ||
        lead.email.toLowerCase().includes(searchTerm) ||
        lead.phone.includes(searchTerm) ||
        (lead.address && lead.address.toLowerCase().includes(searchTerm)),
      );
    }

    console.log(`Found ${leads.length} leads for user ${userId}`);
    return {success: true, leads};
  } catch (error) {
    console.error(`Get leads error: ${error.message}`);
    throw new functions.https.HttpsError("internal", error.message);
  }
});

// ============================================
// GET QUOTES (with search/filter support)
// ============================================
exports.getQuotes = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  const userId = context.auth.uid;
  const {search, limit = 50} = data || {};

  try {
    console.log(`Getting quotes for user ${userId}, search: ${search}`);

    const query = db.collection("quotes")
        .where("userId", "==", userId)
        .orderBy("createdAt", "desc")
        .limit(limit);

    const snapshot = await query.get();
    let quotes = snapshot.docs.map((doc) => ({
      id: doc.id,
      ...doc.data(),
    }));

    // Apply search filter if provided
    if (search && search.trim()) {
      const searchTerm = search.toLowerCase();
      quotes = quotes.filter((quote) =>
        quote.reference.toLowerCase().includes(searchTerm) ||
        quote.clientName.toLowerCase().includes(searchTerm) ||
        (quote.address && quote.address.toLowerCase().includes(searchTerm)),
      );
    }

    console.log(`Found ${quotes.length} quotes for user ${userId}`);
    return {success: true, quotes};
  } catch (error) {
    console.error(`Get quotes error: ${error.message}`);
    throw new functions.https.HttpsError("internal", error.message);
  }
});

// ============================================
// UPDATE USER SETTINGS
// ============================================
exports.updateSettings = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  const userId = context.auth.uid;
  const {settings} = data;

  if (!settings) {
    throw new functions.https.HttpsError("invalid-argument", "Settings data is required");
  }

  try {
    console.log(`Updating settings for user ${userId}`);

    await db.collection("user_settings").doc(userId).set(settings, {merge: true});

    console.log(`Settings updated successfully for user ${userId}`);
    return {success: true, message: "Settings updated successfully"};
  } catch (error) {
    console.error(`Update settings error: ${error.message}`);
    throw new functions.https.HttpsError("internal", error.message);
  }
});

// ============================================
// SYNC DATA (merge offline records with cloud)
// ============================================
exports.syncData = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  const userId = context.auth.uid;
  const {offlineData} = data;

  if (!offlineData) {
    throw new functions.https.HttpsError("invalid-argument", "Offline data is required");
  }

  try {
    console.log(`Syncing data for user ${userId}`);

    const syncResults = {
      leads: {created: 0, updated: 0, errors: []},
      quotes: {created: 0, updated: 0, errors: []},
      settings: {updated: false, error: null},
    };

    // Sync leads
    if (offlineData.leads && Array.isArray(offlineData.leads)) {
      for (const lead of offlineData.leads) {
        try {
          if (lead.id) {
            // Update existing lead
            await db.collection("leads").doc(lead.id).set(lead, {merge: true});
            syncResults.leads.updated++;
          } else {
            // Create new lead
            const docRef = await db.collection("leads").add({
              ...lead,
              userId,
              createdAt: admin.firestore.FieldValue.serverTimestamp(),
              updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            });
            syncResults.leads.created++;
          }
        } catch (error) {
          syncResults.leads.errors.push({lead: lead.name || "Unknown", error: error.message});
        }
      }
    }

    // Sync quotes
    if (offlineData.quotes && Array.isArray(offlineData.quotes)) {
      for (const quote of offlineData.quotes) {
        try {
          if (quote.id) {
            // Update existing quote
            await db.collection("quotes").doc(quote.id).set(quote, {merge: true});
            syncResults.quotes.updated++;
          } else {
            // Create new quote
            const docRef = await db.collection("quotes").add({
              ...quote,
              userId,
              createdAt: admin.firestore.FieldValue.serverTimestamp(),
              updatedAt: admin.firestore.FieldValue.serverTimestamp(),
            });
            syncResults.quotes.created++;
          }
        } catch (error) {
          syncResults.quotes.errors.push({quote: quote.reference || "Unknown", error: error.message});
        }
      }
    }

    // Sync settings
    if (offlineData.settings) {
      try {
        await db.collection("user_settings").doc(userId).set(offlineData.settings, {merge: true});
        syncResults.settings.updated = true;
      } catch (error) {
        syncResults.settings.error = error.message;
      }
    }

    console.log(`Sync completed for user ${userId}:`, syncResults);
    return {success: true, results: syncResults};
  } catch (error) {
    console.error(`Sync data error: ${error.message}`);
    throw new functions.https.HttpsError("internal", error.message);
  }
});

// ============================================
// GET USER SETTINGS
// ============================================
exports.getSettings = functions.https.onCall(async (data, context) => {
  if (!context.auth) {
    throw new functions.https.HttpsError("unauthenticated", "Must be logged in");
  }

  const userId = context.auth.uid;

  try {
    const settingsDoc = await db.collection("user_settings").doc(userId).get();

    if (settingsDoc.exists) {
      return {success: true, settings: settingsDoc.data()};
    } else {
      return {success: true, settings: null};
    }
  } catch (error) {
    console.error(`Get settings error: ${error.message}`);
    throw new functions.https.HttpsError("internal", error.message);
  }
});

// ============================================
// HEALTH CHECK
// ============================================
exports.healthCheck = functions.https.onRequest((req, res) => {
  res.json({
    status: "ok",
    timestamp: new Date().toISOString(),
    service: "Solora RESTful API",
    version: "1.0.0",
    endpoints: [
      "calculateQuote",
      "saveQuote",
      "saveLead",
      "getQuoteById",
      "getLeads",
      "getQuotes",
      "getSettings",
      "updateSettings",
      "syncData",
    ],
  });
});
