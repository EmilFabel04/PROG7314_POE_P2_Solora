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
    }

    @Serializable
    data class PowerResponse(
        @SerialName("geometry") val geometry: Geometry? = null,
        @SerialName("properties") val properties: Properties? = null,
        @SerialName("type") val type: String? = null
    ) {
        @Serializable
        data class Geometry(
            @SerialName("coordinates") val coordinates: List<Double>? = null,
            @SerialName("type") val type: String? = null
        )

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

    /**
     * Get solar data from NASA Power API using the correct endpoint
     * Based on: https://power.larc.nasa.gov/data-access-viewer/
     */
    suspend fun getSolarData(lat: Double, lon: Double, year: Int = 2023): Result<LocationData> {
        return try {
            // Getting solar data for lat=$lat, lon=$lon, year=$year
            
            // Validate coordinates
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                return Result.failure(IllegalArgumentException("Invalid coordinates: lat=$lat, lon=$lon"))
            }

            // Use the correct NASA Power API endpoint
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
                    "&start=${year}0101" +
                    "&end=${year}1231" +
                    "&format=JSON"

            // NASA API URL: $url

            val response: PowerResponse = client.get(url).body()
            
            // NASA API Response received
            // Full response: $response
            // Response type: ${response.type}
            // Response geometry: ${response.geometry}
            // Response properties: ${response.properties}
            
            // If the response is empty, try the daily endpoint as fallback
            if (response.properties == null) {
                // Monthly endpoint failed, trying daily endpoint
                return tryDailyEndpoint(lat, lon, year)
            }

            val parameter = response.properties?.parameter
                ?: return Result.failure(Exception("No parameter data in NASA API response"))
            
            // Parameter data: $parameter
            // Solar irradiance data: ${parameter.solarIrradiance}
            // Temperature data: ${parameter.temperature}

            val monthlyData = mutableMapOf<Int, SolarData>()
            var totalIrradiance = 0.0
            var totalSunHours = 0.0

            // Process data for each month
            for (month in 1..12) {
                val monthKey = month.toString().padStart(2, '0')
                // Processing month $month (key: $monthKey)
                
                val irradiance = parameter.solarIrradiance?.get(monthKey)
                // Month $month irradiance: $irradiance
                
                if (irradiance == null) {
                    // Missing solar irradiance data for month $month (key: $monthKey)
                    // Available solar irradiance keys: ${parameter.solarIrradiance?.keys}
                    
                    // Try alternative month key formats
                    val altKey1 = month.toString() // "1", "2", etc.
                    val altKey2 = String.format("%02d", month) // "01", "02", etc.
                    val altKey3 = "0$month" // "01", "02", etc. for months 1-9
                    
                    // Trying alternative keys: $altKey1, $altKey2, $altKey3
                    
                    val alternativeIrradiance = parameter.solarIrradiance?.get(altKey1) 
                        ?: parameter.solarIrradiance?.get(altKey2)
                        ?: parameter.solarIrradiance?.get(altKey3)
                    
                    if (alternativeIrradiance != null) {
                        // Found data with alternative key: $alternativeIrradiance
                        val temperature = parameter.temperature?.get(altKey1) ?: parameter.temperature?.get(altKey2) ?: parameter.temperature?.get(altKey3)
                        val windSpeed = parameter.windSpeed?.get(altKey1) ?: parameter.windSpeed?.get(altKey2) ?: parameter.windSpeed?.get(altKey3)
                        val humidity = parameter.humidity?.get(altKey1) ?: parameter.humidity?.get(altKey2) ?: parameter.humidity?.get(altKey3)
                        
                        val sunHours = alternativeIrradiance / 1.0 // Peak sun hours
                        
                        monthlyData[month] = SolarData(
                            month = month,
                            solarIrradiance = alternativeIrradiance,
                            temperature = temperature,
                            windSpeed = windSpeed,
                            humidity = humidity,
                            estimatedSunHours = sunHours
                        )
                        
                        totalIrradiance += alternativeIrradiance
                        totalSunHours += sunHours
                        continue
                    }
                    
                    return Result.failure(Exception("Missing solar irradiance data for month $month"))
                }
                
                val temperature = parameter.temperature?.get(monthKey)
                val windSpeed = parameter.windSpeed?.get(monthKey)
                val humidity = parameter.humidity?.get(monthKey)
                
                // Convert solar irradiance to peak sun hours
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

            // NASA data calculated successfully: irradiance=${locationData.averageAnnualIrradiance}, sunHours=${locationData.averageAnnualSunHours}
            Result.success(locationData)
            
        } catch (e: Exception) {
            // ("NasaPowerClient", "Failed to fetch NASA Power data: ${e.message}", e)
            Result.failure(Exception("Failed to fetch NASA Power data: ${e.message}", e))
        }
    }

    /**
     * Try the daily endpoint as fallback when monthly fails
     */
    private suspend fun tryDailyEndpoint(lat: Double, lon: Double, year: Int): Result<LocationData> {
        return try {
            // Trying daily endpoint for lat=$lat, lon=$lon, year=$year
            
            // Use daily endpoint for the middle of the year (July 15)
            val url = "https://power.larc.nasa.gov/api/temporal/daily/point" +
                    "?parameters=ALLSKY_SFC_SW_DWN" +
                    "&community=RE" +
                    "&latitude=$lat" +
                    "&longitude=$lon" +
                    "&start=${year}0701" +
                    "&end=${year}0731" +
                    "&format=JSON"
            
            // Daily API URL: $url
            
            val response: PowerResponse = client.get(url).body()
            
            // Daily API Response: $response
            
            if (response.properties?.parameter?.solarIrradiance != null) {
                // Calculate average from daily data
                val dailyData = response.properties.parameter.solarIrradiance
                val averageIrradiance = dailyData.values.average()
                
                // Daily data average irradiance: $averageIrradiance
                
                // Create monthly data with the average
                val monthlyData = mutableMapOf<Int, SolarData>()
                for (month in 1..12) {
                    monthlyData[month] = SolarData(
                        month = month,
                        solarIrradiance = averageIrradiance,
                        temperature = 20.0,
                        windSpeed = 3.0,
                        humidity = 70.0,
                        estimatedSunHours = averageIrradiance
                    )
                }
                
                val locationData = LocationData(
                    latitude = lat,
                    longitude = lon,
                    monthlyData = monthlyData,
                    averageAnnualIrradiance = averageIrradiance,
                    averageAnnualSunHours = averageIrradiance
                )
                
                // Daily endpoint success: irradiance=$averageIrradiance
                Result.success(locationData)
            } else {
                // Daily endpoint also failed
                Result.failure(Exception("Both monthly and daily endpoints failed"))
            }
        } catch (e: Exception) {
            // ("NasaPowerClient", "Daily endpoint error: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get solar data with fallback values for South Africa
     * This provides reasonable defaults when NASA API fails
     */
    suspend fun getSolarDataWithFallback(lat: Double, lon: Double, year: Int = 2023): Result<LocationData> {
        val nasaResult = getSolarData(lat, lon, year)
        
        return if (nasaResult.isSuccess) {
            nasaResult
        } else {
            // NASA API failed, using fallback data for South Africa
            
            // Fallback data for South Africa (typical values)
            val fallbackMonthlyData = mapOf(
                1 to SolarData(1, 6.5, 25.0, 3.5, 65.0, 6.5), // January
                2 to SolarData(2, 6.2, 25.5, 3.2, 68.0, 6.2), // February
                3 to SolarData(3, 5.8, 24.0, 3.8, 70.0, 5.8), // March
                4 to SolarData(4, 5.2, 21.5, 4.2, 72.0, 5.2), // April
                5 to SolarData(5, 4.8, 18.0, 4.5, 75.0, 4.8), // May
                6 to SolarData(6, 4.5, 15.5, 4.8, 78.0, 4.5), // June
                7 to SolarData(7, 4.8, 15.0, 4.6, 77.0, 4.8), // July
                8 to SolarData(8, 5.5, 17.5, 4.2, 74.0, 5.5), // August
                9 to SolarData(9, 6.2, 20.5, 3.9, 71.0, 6.2), // September
                10 to SolarData(10, 6.8, 23.0, 3.6, 68.0, 6.8), // October
                11 to SolarData(11, 7.0, 24.5, 3.4, 66.0, 7.0), // November
                12 to SolarData(12, 6.8, 25.0, 3.3, 65.0, 6.8)  // December
            )
            
            val locationData = LocationData(
                latitude = lat,
                longitude = lon,
                monthlyData = fallbackMonthlyData,
                averageAnnualIrradiance = 5.8, // kWh/m²/day average for South Africa
                averageAnnualSunHours = 5.8
            )
            
            // Using fallback data: irradiance=${locationData.averageAnnualIrradiance}, sunHours=${locationData.averageAnnualSunHours}
            Result.success(locationData)
        }
    }

    // Simplified function for backward compatibility
    suspend fun getMonthlySunHours(lat: Double, lon: Double, month: Int): Result<Double> {
        return try {
            val solarData = getSolarDataWithFallback(lat, lon)
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
            val locationData = getSolarDataWithFallback(lat, lon).getOrThrow()
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