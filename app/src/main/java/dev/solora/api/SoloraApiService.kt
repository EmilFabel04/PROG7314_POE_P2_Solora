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

class SoloraApiService {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
        defaultRequest {
            // Replace with your actual backend URL
            url("https://api.solora.dev/v1/")
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

    fun close() {
        client.close()
    }
}

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
