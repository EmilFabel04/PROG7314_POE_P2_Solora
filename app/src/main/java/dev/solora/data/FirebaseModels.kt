package dev.solora.data

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp

// Firebase Firestore data models for Part 2
data class FirebaseQuote(
    @DocumentId
    val id: String? = null,
    val reference: String = "",
    val clientName: String = "",
    val address: String = "",
    val usageKwh: Double? = null,
    val billRands: Double? = null,
    val tariff: Double = 0.0,
    val panelWatt: Int = 0,
    val sunHours: Double = 0.0,
    val systemKwp: Double = 0.0,
    val estimatedGeneration: Double = 0.0,
    val paybackMonths: Int = 0,
    val savingsFirstYear: Double = 0.0,
    val dateEpoch: Long = 0L,
    val userId: String = "", // Link to Firebase Auth user
    // NASA API data
    val latitude: Double? = null,
    val longitude: Double? = null,
    val averageAnnualIrradiance: Double? = null,
    val averageAnnualSunHours: Double? = null,
    val optimalMonth: Int? = null,
    val optimalMonthIrradiance: Double? = null,
    val temperature: Double? = null,
    val windSpeed: Double? = null,
    val humidity: Double? = null,
    // Company information
    val companyName: String = "",
    val companyAddress: String = "",
    val companyPhone: String = "",
    val companyEmail: String = "",
    val companyWebsite: String = "",
    val consultantName: String = "",
    val consultantPhone: String = "",
    val consultantEmail: String = "",
    val consultantLicense: String = "",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

data class FirebaseLead(
    @DocumentId
    val id: String? = null,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val status: String = "new",
    val notes: String? = null,
    val quoteId: String? = null, // Link to quote if applicable
    val userId: String = "", // Link to Firebase Auth user
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

data class FirebaseUser(
    @DocumentId
    val id: String? = null,
    val name: String = "",
    val surname: String = "",
    val email: String = "",
    val phone: String? = null,
    val company: String? = null,
    val role: String = "sales_consultant",
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

data class FirebaseConfiguration(
    @DocumentId
    val id: String? = null,
    val nasaApiEnabled: Boolean = true,
    val nasaApiUrl: String = "https://power.larc.nasa.gov/api/",
    val defaultTariff: Double = 2.50,
    val defaultPanelWatt: Int = 450,
    val companyInfo: CompanyInfo = CompanyInfo(),
    @ServerTimestamp
    val updatedAt: Timestamp? = null
)

data class CompanyInfo(
    val name: String = "Solora Solar",
    val contactEmail: String = "info@solora.dev",
    val contactPhone: String = "+27 11 123 4567",
    val address: String = "Johannesburg, South Africa"
)
