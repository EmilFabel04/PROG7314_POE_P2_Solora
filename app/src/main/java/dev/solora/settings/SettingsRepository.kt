package dev.solora.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    
    val settings: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                calculationSettings = CalculationSettings(
                    defaultTariff = preferences[SettingsKeys.DEFAULT_TARIFF] ?: 2.50,
                    defaultPanelWatt = preferences[SettingsKeys.DEFAULT_PANEL_WATT] ?: 420,
                    panelCostPerWatt = preferences[SettingsKeys.PANEL_COST_PER_WATT] ?: 15.0,
                    inverterCostPerWatt = preferences[SettingsKeys.INVERTER_COST_PER_WATT] ?: 12.0,
                    installationCostPerKw = preferences[SettingsKeys.INSTALLATION_COST_PER_KW] ?: 15000.0,
                    panelEfficiency = preferences[SettingsKeys.PANEL_EFFICIENCY] ?: 0.20,
                    performanceRatio = preferences[SettingsKeys.PERFORMANCE_RATIO] ?: 0.80,
                    inverterSizingRatio = preferences[SettingsKeys.INVERTER_SIZING_RATIO] ?: 0.80,
                    defaultSunHours = preferences[SettingsKeys.DEFAULT_SUN_HOURS] ?: 5.0,
                    systemLifetime = preferences[SettingsKeys.SYSTEM_LIFETIME] ?: 25,
                    panelDegradationRate = preferences[SettingsKeys.PANEL_DEGRADATION_RATE] ?: 0.005,
                    co2PerKwh = preferences[SettingsKeys.CO2_PER_KWH] ?: 0.5
                ),
                companySettings = CompanySettings(
                    companyName = preferences[SettingsKeys.COMPANY_NAME] ?: "",
                    companyAddress = preferences[SettingsKeys.COMPANY_ADDRESS] ?: "",
                    companyPhone = preferences[SettingsKeys.COMPANY_PHONE] ?: "",
                    companyEmail = preferences[SettingsKeys.COMPANY_EMAIL] ?: "",
                    companyWebsite = preferences[SettingsKeys.COMPANY_WEBSITE] ?: "",
                    consultantName = preferences[SettingsKeys.CONSULTANT_NAME] ?: "",
                    consultantPhone = preferences[SettingsKeys.CONSULTANT_PHONE] ?: "",
                    consultantEmail = preferences[SettingsKeys.CONSULTANT_EMAIL] ?: "",
                    consultantLicense = preferences[SettingsKeys.CONSULTANT_LICENSE] ?: "",
                    companyLogo = preferences[SettingsKeys.COMPANY_LOGO] ?: "",
                    quoteFooter = preferences[SettingsKeys.QUOTE_FOOTER] ?: "",
                    termsAndConditions = preferences[SettingsKeys.TERMS_AND_CONDITIONS] ?: ""
                ),
                currency = preferences[SettingsKeys.CURRENCY] ?: "ZAR",
                language = preferences[SettingsKeys.LANGUAGE] ?: "en",
                theme = preferences[SettingsKeys.THEME] ?: "light"
            )
        }
    
    suspend fun updateCalculationSettings(settings: CalculationSettings) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.DEFAULT_TARIFF] = settings.defaultTariff
            preferences[SettingsKeys.DEFAULT_PANEL_WATT] = settings.defaultPanelWatt
            preferences[SettingsKeys.PANEL_COST_PER_WATT] = settings.panelCostPerWatt
            preferences[SettingsKeys.INVERTER_COST_PER_WATT] = settings.inverterCostPerWatt
            preferences[SettingsKeys.INSTALLATION_COST_PER_KW] = settings.installationCostPerKw
            preferences[SettingsKeys.PANEL_EFFICIENCY] = settings.panelEfficiency
            preferences[SettingsKeys.PERFORMANCE_RATIO] = settings.performanceRatio
            preferences[SettingsKeys.INVERTER_SIZING_RATIO] = settings.inverterSizingRatio
            preferences[SettingsKeys.DEFAULT_SUN_HOURS] = settings.defaultSunHours
            preferences[SettingsKeys.SYSTEM_LIFETIME] = settings.systemLifetime
            preferences[SettingsKeys.PANEL_DEGRADATION_RATE] = settings.panelDegradationRate
            preferences[SettingsKeys.CO2_PER_KWH] = settings.co2PerKwh
        }
    }
    
    suspend fun updateCompanySettings(settings: CompanySettings) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.COMPANY_NAME] = settings.companyName
            preferences[SettingsKeys.COMPANY_ADDRESS] = settings.companyAddress
            preferences[SettingsKeys.COMPANY_PHONE] = settings.companyPhone
            preferences[SettingsKeys.COMPANY_EMAIL] = settings.companyEmail
            preferences[SettingsKeys.COMPANY_WEBSITE] = settings.companyWebsite
            preferences[SettingsKeys.CONSULTANT_NAME] = settings.consultantName
            preferences[SettingsKeys.CONSULTANT_PHONE] = settings.consultantPhone
            preferences[SettingsKeys.CONSULTANT_EMAIL] = settings.consultantEmail
            preferences[SettingsKeys.CONSULTANT_LICENSE] = settings.consultantLicense
            preferences[SettingsKeys.COMPANY_LOGO] = settings.companyLogo
            preferences[SettingsKeys.QUOTE_FOOTER] = settings.quoteFooter
            preferences[SettingsKeys.TERMS_AND_CONDITIONS] = settings.termsAndConditions
        }
    }
    
    suspend fun updateAppSettings(settings: AppSettings) {
        context.dataStore.edit { preferences ->
            preferences[SettingsKeys.CURRENCY] = settings.currency
            preferences[SettingsKeys.LANGUAGE] = settings.language
            preferences[SettingsKeys.THEME] = settings.theme
        }
    }
    
    suspend fun resetToDefaults() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
