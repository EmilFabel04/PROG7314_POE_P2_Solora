package dev.solora.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SoloraApiService(
    private val baseUrl: String = "https://api.solora.dev/v1/",
    private val enableLogging: Boolean = true
) {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                encodeDefaults = true
            })
        }
        if (enableLogging) {
            install(Logging) {
                level = LogLevel.INFO
            }
        }
                // HttpTimeout plugin is not available in Ktor client Android
                // Using default timeouts from the Android engine
        defaultRequest {
            url(baseUrl)
            contentType(ContentType.Application.Json)
        }
    }

    // User Management
    suspend fun getUserProfile(token: String): Result<UserProfile> {
        return try {
            val response: UserProfile = client.get("user/profile") {
                header("Authorization", "Bearer $token")
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(token: String, profile: UserProfileUpdate): Result<UserProfile> {
        return try {
            val response: UserProfile = client.post("user/profile") {
                header("Authorization", "Bearer $token")
                setBody(profile)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Quote Management
    suspend fun syncQuotes(token: String, quotes: List<QuoteData>): Result<SyncResponse> {
        return try {
            val response: SyncResponse = client.post("quotes/sync") {
                header("Authorization", "Bearer $token")
                setBody(QuoteSyncRequest(quotes))
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getQuotes(token: String): Result<List<QuoteData>> {
        return try {
            val response: QuoteListResponse = client.get("quotes") {
                header("Authorization", "Bearer $token")
            }.body()
            Result.success(response.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Lead Management
    suspend fun syncLeads(token: String, leads: List<LeadData>): Result<SyncResponse> {
        return try {
            val response: SyncResponse = client.post("leads/sync") {
                header("Authorization", "Bearer $token")
                setBody(LeadSyncRequest(leads))
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getLeads(token: String): Result<List<LeadData>> {
        return try {
            val response: LeadListResponse = client.get("leads") {
                header("Authorization", "Bearer $token")
            }.body()
            Result.success(response.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Configuration
    suspend fun getApiConfiguration(token: String): Result<ApiConfiguration> {
        return try {
            val response: ApiConfiguration = client.get("config") {
                header("Authorization", "Bearer $token")
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Solar Analytics
    suspend fun submitSolarAnalysis(token: String, analysis: SolarAnalysisRequest): Result<SolarAnalysisResponse> {
        return try {
            val response: SolarAnalysisResponse = client.post("solar/analysis") {
                header("Authorization", "Bearer $token")
                setBody(analysis)
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSolarRecommendations(token: String, latitude: Double, longitude: Double): Result<SolarRecommendations> {
        return try {
            val response: SolarRecommendations = client.get("solar/recommendations") {
                header("Authorization", "Bearer $token")
                url {
                    parameters.append("lat", latitude.toString())
                    parameters.append("lon", longitude.toString())
                }
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Business Analytics
    suspend fun getBusinessAnalytics(token: String, period: String = "month"): Result<BusinessAnalytics> {
        return try {
            val response: BusinessAnalytics = client.get("analytics/business") {
                header("Authorization", "Bearer $token")
                url {
                    parameters.append("period", period)
                }
            }.body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Health Check
    suspend fun healthCheck(): Result<HealthStatus> {
        return try {
            val response: HealthStatus = client.get("health").body()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Notifications
    suspend fun getNotifications(token: String, unreadOnly: Boolean = false): Result<List<NotificationData>> {
        return try {
            val response: NotificationListResponse = client.get("notifications") {
                header("Authorization", "Bearer $token")
                url {
                    parameters.append("unread_only", unreadOnly.toString())
                }
            }.body()
            Result.success(response.data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(token: String, notificationId: String): Result<Unit> {
        return try {
            client.post("notifications/$notificationId/read") {
                header("Authorization", "Bearer $token")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun close() {
        client.close()
    }
}

// Additional Data Models
@Serializable
data class SolarAnalysisRequest(
    @SerialName("location") val location: LocationInfo,
    @SerialName("usage_data") val usageData: UsageData,
    @SerialName("preferences") val preferences: CustomerPreferences
)

@Serializable
data class LocationInfo(
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("address") val address: String,
    @SerialName("roof_area") val roofArea: Double? = null,
    @SerialName("roof_orientation") val roofOrientation: String? = null
)

@Serializable
data class UsageData(
    @SerialName("monthly_kwh") val monthlyKwh: Double,
    @SerialName("monthly_bill") val monthlyBill: Double,
    @SerialName("tariff_rate") val tariffRate: Double,
    @SerialName("historical_usage") val historicalUsage: List<MonthlyUsage>? = null
)

@Serializable
data class MonthlyUsage(
    @SerialName("month") val month: Int,
    @SerialName("year") val year: Int,
    @SerialName("kwh_used") val kwhUsed: Double,
    @SerialName("bill_amount") val billAmount: Double
)

@Serializable
data class CustomerPreferences(
    @SerialName("budget_max") val budgetMax: Double? = null,
    @SerialName("payback_period_max") val paybackPeriodMax: Int? = null,
    @SerialName("financing_options") val financingOptions: List<String>? = null
)

@Serializable
data class SolarAnalysisResponse(
    @SerialName("analysis_id") val analysisId: String,
    @SerialName("recommended_system") val recommendedSystem: SolarSystemSpec,
    @SerialName("financial_analysis") val financialAnalysis: FinancialAnalysis,
    @SerialName("environmental_impact") val environmentalImpact: EnvironmentalImpact,
    @SerialName("nasa_data") val nasaData: NasaDataSummary
)

@Serializable
data class SolarSystemSpec(
    @SerialName("system_size_kw") val systemSizeKw: Double,
    @SerialName("panel_count") val panelCount: Int,
    @SerialName("panel_type") val panelType: String,
    @SerialName("inverter_type") val inverterType: String,
    @SerialName("estimated_cost") val estimatedCost: Double
)

@Serializable
data class FinancialAnalysis(
    @SerialName("installation_cost") val installationCost: Double,
    @SerialName("annual_savings") val annualSavings: Double,
    @SerialName("payback_period_years") val paybackPeriodYears: Double,
    @SerialName("lifetime_savings") val lifetimeSavings: Double,
    @SerialName("roi_percentage") val roiPercentage: Double
)

@Serializable
data class EnvironmentalImpact(
    @SerialName("co2_offset_annually_tons") val co2OffsetAnnuallyTons: Double,
    @SerialName("trees_equivalent") val treesEquivalent: Int,
    @SerialName("car_miles_offset") val carMilesOffset: Double
)

@Serializable
data class NasaDataSummary(
    @SerialName("average_sun_hours") val averageSunHours: Double,
    @SerialName("peak_month") val peakMonth: String,
    @SerialName("peak_irradiance") val peakIrradiance: Double,
    @SerialName("annual_irradiance") val annualIrradiance: Double
)

@Serializable
data class SolarRecommendations(
    @SerialName("optimal_tilt") val optimalTilt: Double,
    @SerialName("optimal_azimuth") val optimalAzimuth: Double,
    @SerialName("recommended_panels") val recommendedPanels: List<PanelRecommendation>,
    @SerialName("seasonal_performance") val seasonalPerformance: Map<String, Double>
)

@Serializable
data class PanelRecommendation(
    @SerialName("brand") val brand: String,
    @SerialName("model") val model: String,
    @SerialName("wattage") val wattage: Int,
    @SerialName("efficiency") val efficiency: Double,
    @SerialName("price_per_watt") val pricePerWatt: Double,
    @SerialName("warranty_years") val warrantyYears: Int
)

@Serializable
data class BusinessAnalytics(
    @SerialName("total_quotes") val totalQuotes: Int,
    @SerialName("conversion_rate") val conversionRate: Double,
    @SerialName("average_system_size") val averageSystemSize: Double,
    @SerialName("revenue") val revenue: RevenueAnalytics,
    @SerialName("top_regions") val topRegions: List<RegionData>
)

@Serializable
data class RevenueAnalytics(
    @SerialName("current_period") val currentPeriod: Double,
    @SerialName("previous_period") val previousPeriod: Double,
    @SerialName("growth_percentage") val growthPercentage: Double
)

@Serializable
data class RegionData(
    @SerialName("region") val region: String,
    @SerialName("quote_count") val quoteCount: Int,
    @SerialName("average_system_size") val averageSystemSize: Double
)

@Serializable
data class HealthStatus(
    @SerialName("status") val status: String,
    @SerialName("version") val version: String,
    @SerialName("timestamp") val timestamp: String,
    @SerialName("services") val services: Map<String, String>
)

@Serializable
data class NotificationData(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("message") val message: String,
    @SerialName("type") val type: String,
    @SerialName("is_read") val isRead: Boolean,
    @SerialName("created_at") val createdAt: String,
    @SerialName("action_url") val actionUrl: String? = null
)

@Serializable
data class NotificationListResponse(
    @SerialName("data") val data: List<NotificationData>,
    @SerialName("unread_count") val unreadCount: Int,
    @SerialName("total") val total: Int
)

// Data Models
@Serializable
data class UserProfile(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
    @SerialName("phone") val phone: String? = null,
    @SerialName("company") val company: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class UserProfileUpdate(
    @SerialName("name") val name: String? = null,
    @SerialName("phone") val phone: String? = null,
    @SerialName("company") val company: String? = null
)

@Serializable
data class QuoteData(
    @SerialName("id") val id: String? = null,
    @SerialName("reference") val reference: String,
    @SerialName("client_name") val clientName: String,
    @SerialName("address") val address: String,
    @SerialName("usage_kwh") val usageKwh: Double?,
    @SerialName("bill_rands") val billRands: Double?,
    @SerialName("tariff") val tariff: Double,
    @SerialName("panel_watt") val panelWatt: Int,
    @SerialName("sun_hours") val sunHours: Double,
    @SerialName("system_kwp") val systemKwp: Double,
    @SerialName("estimated_generation") val estimatedGeneration: Double,
    @SerialName("payback_months") val paybackMonths: Int,
    @SerialName("savings_first_year") val savingsFirstYear: Double,
    @SerialName("date_epoch") val dateEpoch: Long,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class LeadData(
    @SerialName("id") val id: String? = null,
    @SerialName("name") val name: String,
    @SerialName("email") val email: String,
    @SerialName("phone") val phone: String,
    @SerialName("status") val status: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class QuoteSyncRequest(
    @SerialName("quotes") val quotes: List<QuoteData>
)

@Serializable
data class LeadSyncRequest(
    @SerialName("leads") val leads: List<LeadData>
)

@Serializable
data class QuoteListResponse(
    @SerialName("data") val data: List<QuoteData>,
    @SerialName("total") val total: Int,
    @SerialName("page") val page: Int = 1
)

@Serializable
data class LeadListResponse(
    @SerialName("data") val data: List<LeadData>,
    @SerialName("total") val total: Int,
    @SerialName("page") val page: Int = 1
)

@Serializable
data class SyncResponse(
    @SerialName("success") val success: Boolean,
    @SerialName("message") val message: String,
    @SerialName("synced_count") val syncedCount: Int,
    @SerialName("errors") val errors: List<String> = emptyList()
)

@Serializable
data class ApiConfiguration(
    @SerialName("nasa_api_enabled") val nasaApiEnabled: Boolean,
    @SerialName("nasa_api_url") val nasaApiUrl: String? = null,
    @SerialName("default_tariff") val defaultTariff: Double,
    @SerialName("default_panel_watt") val defaultPanelWatt: Int,
    @SerialName("company_info") val companyInfo: CompanyInfo
)

@Serializable
data class CompanyInfo(
    @SerialName("name") val name: String,
    @SerialName("contact_email") val contactEmail: String,
    @SerialName("contact_phone") val contactPhone: String,
    @SerialName("address") val address: String
)
