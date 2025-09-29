package dev.solora.data

import androidx.room.Entity
import androidx.room.PrimaryKey

data class UserInfo(
    val id: String,
    val name: String,
    val surname: String,
    val email: String,
    val phone: String? = null,
    val occupation: String? = null,
    val birthday: String? = null,
    val gender: String? = null,
    val race: String? = null
)

@Entity(tableName = "leads")
data class Lead(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    val name: String,
    val address: String,
    val contact: String,
    val status: String = "new", // new, contacted, qualified, proposal, closed, lost
    val source: String = "manual", // manual, website, referral, marketing
    val notes: String = "",
    val consultantId: String? = null, // Firebase Auth userId of the consultant who created this lead
    val quoteId: Long? = null, // Optional - if this lead was created from a quote
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// User Profile data class (not stored in Room, only Firebase)
data class UserProfile(
    val userId: String,
    val name: String,
    val email: String,
    val companyName: String? = null,
    val phone: String? = null,
    val jobTitle: String? = null,
    val address: String? = null,
    val profileImageUrl: String? = null,
    val preferences: Map<String, Any> = emptyMap(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

// Analytics data classes
data class UserStats(
    val totalQuotes: Int,
    val totalLeads: Int,
    val totalSystemKw: Double,
    val totalMonthlySavings: Double,
    val totalAnnualSavings: Double,
    val averageSystemSize: Double,
    val conversionRate: Double, // leads to quotes ratio
    val generatedAt: Long
)

data class QuoteStats(
    val monthlyQuotes: Int,
    val averageQuoteValue: Double,
    val topClientType: String,
    val mostPopularSystemSize: String,
    val totalCo2Savings: Double
)

@Entity(tableName = "quotes")
data class Quote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    val clientName: String,
    val address: String,
    val monthlyUsageKwh: Double?,
    val monthlyBillRands: Double?,
    val tariff: Double,
    val panelWatt: Int,
    val sunHours: Double,
    val panels: Int,
    val systemKw: Double,
    val inverterKw: Double,
    val savingsRands: Double,
    val consultantId: String? = null, // Firebase Auth userId of the consultant who created this quote
    val dateEpoch: Long = System.currentTimeMillis(),
    // Location and NASA API data
    val latitude: Double? = null,
    val longitude: Double? = null,
    val averageAnnualIrradiance: Double? = null,
    val averageAnnualSunHours: Double? = null,
    val optimalMonth: Int? = null,
    val optimalMonthIrradiance: Double? = null,
    val temperature: Double? = null,
    val windSpeed: Double? = null,
    val humidity: Double? = null,
    // Financial calculations
    val systemCostRands: Double? = null,
    val paybackYears: Double? = null,
    val annualSavingsRands: Double? = null,
    val co2SavingsKgPerYear: Double? = null
)
