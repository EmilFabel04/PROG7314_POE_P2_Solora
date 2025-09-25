package dev.solora.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leads")
data class Lead(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reference: String,
    val name: String,
    val address: String,
    val contact: String
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
