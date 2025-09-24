package dev.solora.quote

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class NasaPowerClient {
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
        // Note: HttpTimeout plugin is not available in Ktor client Android
        // Using default timeouts from the Android engine
    }

    @Serializable
    data class PowerResponse(
        @SerialName("properties") val properties: Properties? = null,
        @SerialName("messages") val messages: List<String>? = null,
        @SerialName("parameters") val parameters: Map<String, ParameterInfo>? = null
    ) {
        @Serializable
        data class Properties(
            @SerialName("parameter") val parameter: Parameter? = null
        ) {
            @Serializable
            data class Parameter(
                @SerialName("ALLSKY_SFC_SW_DWN") val solarIrradiance: Map<String, Double>? = null,
                @SerialName("T2M") val temperature: Map<String, Double>? = null,
                @SerialName("WS10M") val windSpeed: Map<String, Double>? = null,
                @SerialName("RH2M") val humidity: Map<String, Double>? = null
            )
        }
        
        @Serializable
        data class ParameterInfo(
            @SerialName("longname") val longName: String? = null,
            @SerialName("units") val units: String? = null
        )
    }

    @Serializable
    data class SolarData(
        val month: Int,
        val solarIrradiance: Double, // kWh/m²/day
        val temperature: Double? = null, // °C
        val windSpeed: Double? = null, // m/s
        val humidity: Double? = null, // %
        val estimatedSunHours: Double // Peak sun hours
    )

    @Serializable
    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val monthlyData: Map<Int, SolarData>,
        val averageAnnualIrradiance: Double,
        val averageAnnualSunHours: Double
    )

    // Enhanced function to get comprehensive solar data
    suspend fun getSolarData(lat: Double, lon: Double, year: Int = 2023): Result<LocationData> {
        return try {
            // Validate coordinates
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                return Result.failure(IllegalArgumentException("Invalid coordinates: lat=$lat, lon=$lon"))
            }

            val parameters = listOf(
                "ALLSKY_SFC_SW_DWN", // Solar irradiance
                "T2M",               // Temperature
                "WS10M",             // Wind speed
                "RH2M"               // Humidity
            ).joinToString(",")

            val url = "https://power.larc.nasa.gov/api/temporal/monthly/point" +
                    "?parameters=$parameters" +
                    "&community=RE" +
                    "&latitude=$lat" +
                    "&longitude=$lon" +
                    "&start=$year" +
                    "&end=$year" +
                    "&format=JSON"

            val response: PowerResponse = client.get(url).body()
            
            // Check for API messages/errors
            response.messages?.forEach { message ->
                if (message.contains("error", ignoreCase = true)) {
                    return Result.failure(Exception("NASA API Error: $message"))
                }
            }

            val parameter = response.properties?.parameter
                ?: return Result.failure(Exception("No parameter data in response"))

            val monthlyData = mutableMapOf<Int, SolarData>()
            var totalIrradiance = 0.0
            var totalSunHours = 0.0

            // Process data for each month
            for (month in 1..12) {
                val monthKey = month.toString().padStart(2, '0')
                
                val irradiance = parameter.solarIrradiance?.get(monthKey)
                    ?: return Result.failure(Exception("Missing solar irradiance data for month $month"))
                
                val temperature = parameter.temperature?.get(monthKey)
                val windSpeed = parameter.windSpeed?.get(monthKey)
                val humidity = parameter.humidity?.get(monthKey)
                
                // Convert solar irradiance to peak sun hours
                // Peak sun hours ≈ daily irradiance (kWh/m²/day) / 1 kW/m² (standard test conditions)
                val sunHours = irradiance / 1.0
                
                monthlyData[month] = SolarData(
                    month = month,
                    solarIrradiance = irradiance,
                    temperature = temperature,
                    windSpeed = windSpeed,
                    humidity = humidity,
                    estimatedSunHours = sunHours
                )
                
                totalIrradiance += irradiance
                totalSunHours += sunHours
            }

            val locationData = LocationData(
                latitude = lat,
                longitude = lon,
                monthlyData = monthlyData,
                averageAnnualIrradiance = totalIrradiance / 12,
                averageAnnualSunHours = totalSunHours / 12
            )

            Result.success(locationData)
            
        } catch (e: Exception) {
            Result.failure(Exception("Failed to fetch NASA Power data: ${e.message}", e))
        }
    }

    // Simplified function for backward compatibility
    suspend fun getMonthlySunHours(lat: Double, lon: Double, month: Int): Result<Double> {
        return try {
            val solarData = getSolarData(lat, lon)
            if (solarData.isSuccess) {
                val monthData = solarData.getOrNull()?.monthlyData?.get(month)
                if (monthData != null) {
                    Result.success(monthData.estimatedSunHours)
                } else {
                    Result.failure(Exception("No data available for month $month"))
                }
            } else {
                Result.failure(solarData.exceptionOrNull() ?: Exception("Unknown error"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get monthly sun hours: ${e.message}", e))
        }
    }

    // Get optimal solar data for a specific location
    suspend fun getOptimalSolarMonth(lat: Double, lon: Double): Result<Pair<Int, SolarData>> {
        return try {
            val locationData = getSolarData(lat, lon).getOrThrow()
            val optimalMonth = locationData.monthlyData.maxByOrNull { it.value.solarIrradiance }
            if (optimalMonth != null) {
                Result.success(Pair(optimalMonth.key, optimalMonth.value))
            } else {
                Result.failure(Exception("No optimal month data found"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Failed to find optimal solar month: ${e.message}", e))
        }
    }

    fun close() {
        client.close()
    }
}


