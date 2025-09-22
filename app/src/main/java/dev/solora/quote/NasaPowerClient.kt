package dev.solora.quote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class NasaPowerClient {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json() }
    }

    @Serializable
    data class PowerResponse(
        @SerialName("properties") val properties: Properties? = null
    ) {
        @Serializable
        data class Properties(@SerialName("parameter") val parameter: Parameter? = null) {
            @Serializable
            data class Parameter(@SerialName("ALLSKY_SFC_SW_DWN") val sun: Map<String, Double>? = null)
        }
    }

    suspend fun getMonthlySunHours(lat: Double, lon: Double, month: Int): Double? {
        val url = "https://power.larc.nasa.gov/api/temporal/monthly/point?parameters=ALLSKY_SFC_SW_DWN&community=RE&latitude=$lat&longitude=$lon&format=JSON"
        val resp: PowerResponse = client.get(url).body()
        val monthKey = month.toString().padStart(2, '0')
        val kwhPerSqmDay = resp.properties?.parameter?.sun?.get(monthKey)
        return kwhPerSqmDay
    }
}


