package dev.solora.settings

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

// Settings data classes
data class CalculationSettings(
    val defaultTariff: Double = 2.50, // R/kWh
    val defaultPanelWatt: Int = 420, // Watts
    val panelCostPerWatt: Double = 15.0, // R/W
    val inverterCostPerWatt: Double = 12.0, // R/W
    val installationCostPerKw: Double = 15000.0, // R/kW
    val panelEfficiency: Double = 0.20, // 20% efficiency
    val performanceRatio: Double = 0.80, // 80% performance ratio
    val inverterSizingRatio: Double = 0.80, // 80% of system size
    val defaultSunHours: Double = 5.0, // Hours per day
    val systemLifetime: Int = 25, // Years
    val panelDegradationRate: Double = 0.005, // 0.5% per year
    val co2PerKwh: Double = 0.5 // kg CO2 per kWh
)

data class CompanySettings(
    val companyName: String = "",
    val companyAddress: String = "",
    val companyPhone: String = "",
    val companyEmail: String = "",
    val companyWebsite: String = "",
    val consultantName: String = "",
    val consultantPhone: String = "",
    val consultantEmail: String = "",
    val consultantLicense: String = "",
    val companyLogo: String = "", // URL or base64
    val quoteFooter: String = "",
    val termsAndConditions: String = ""
)

data class AppSettings(
    val calculationSettings: CalculationSettings = CalculationSettings(),
    val companySettings: CompanySettings = CompanySettings(),
    val currency: String = "ZAR",
    val language: String = "en",
    val theme: String = "light"
)

// Preference keys for DataStore
object SettingsKeys {
    // Calculation Settings
    val DEFAULT_TARIFF = doublePreferencesKey("default_tariff")
    val DEFAULT_PANEL_WATT = intPreferencesKey("default_panel_watt")
    val PANEL_COST_PER_WATT = doublePreferencesKey("panel_cost_per_watt")
    val INVERTER_COST_PER_WATT = doublePreferencesKey("inverter_cost_per_watt")
    val INSTALLATION_COST_PER_KW = doublePreferencesKey("installation_cost_per_kw")
    val PANEL_EFFICIENCY = doublePreferencesKey("panel_efficiency")
    val PERFORMANCE_RATIO = doublePreferencesKey("performance_ratio")
    val INVERTER_SIZING_RATIO = doublePreferencesKey("inverter_sizing_ratio")
    val DEFAULT_SUN_HOURS = doublePreferencesKey("default_sun_hours")
    val SYSTEM_LIFETIME = intPreferencesKey("system_lifetime")
    val PANEL_DEGRADATION_RATE = doublePreferencesKey("panel_degradation_rate")
    val CO2_PER_KWH = doublePreferencesKey("co2_per_kwh")
    
    // Company Settings
    val COMPANY_NAME = stringPreferencesKey("company_name")
    val COMPANY_ADDRESS = stringPreferencesKey("company_address")
    val COMPANY_PHONE = stringPreferencesKey("company_phone")
    val COMPANY_EMAIL = stringPreferencesKey("company_email")
    val COMPANY_WEBSITE = stringPreferencesKey("company_website")
    val CONSULTANT_NAME = stringPreferencesKey("consultant_name")
    val CONSULTANT_PHONE = stringPreferencesKey("consultant_phone")
    val CONSULTANT_EMAIL = stringPreferencesKey("consultant_email")
    val CONSULTANT_LICENSE = stringPreferencesKey("consultant_license")
    val COMPANY_LOGO = stringPreferencesKey("company_logo")
    val QUOTE_FOOTER = stringPreferencesKey("quote_footer")
    val TERMS_AND_CONDITIONS = stringPreferencesKey("terms_and_conditions")
    
    // App Settings
    val CURRENCY = stringPreferencesKey("currency")
    val LANGUAGE = stringPreferencesKey("language")
    val THEME = stringPreferencesKey("theme")
}
