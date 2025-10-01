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
      "getSettings",
    ],
  });
});
