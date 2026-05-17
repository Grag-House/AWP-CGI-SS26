package hka.awp.cgi.temi.app.feature.weatherscreen

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class WeatherIcon { SUN, CLOUD, SUN_CLOUD, RAIN, SNOW, THUNDER, FOG }

// --- Domain Models (Used by UI) ---

data class WeatherState(
    val location: String = "Karlsruhe",
    val hourlyForecast: List<HourlyItem> = emptyList(),
    val weeklyForecast: List<DailyItem> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class HourlyItem(
    val label: String,
    val icon: WeatherIcon,
    val temp: String,
    val precipitation: String
)

data class DailyItem(
    val day: String,
    val icon: WeatherIcon,
    val high: String,
    val low: String
)

// --- API Models (Used for Parsing) ---

@Serializable
data class MetResponse(
    val properties: MetProperties
)

@Serializable
data class MetProperties(
    val timeseries: List<MetTimeSeries>
)

@Serializable
data class MetTimeSeries(
    val time: String,
    val data: MetData
)

@Serializable
data class MetData(
    val instant: MetInstant,
    @SerialName("next_1_hours") val next1Hours: MetNextHours? = null,
    @SerialName("next_6_hours") val next6Hours: MetNextHours? = null,
    @SerialName("next_12_hours") val next12Hours: MetNextHours? = null
)

@Serializable
data class MetInstant(
    val details: MetInstantDetails
)

@Serializable
data class MetInstantDetails(
    @SerialName("air_temperature") val airTemperature: Double
)

@Serializable
data class MetNextHours(
    val summary: MetSummary,
    val details: MetNextDetails? = null
)

@Serializable
data class MetSummary(
    @SerialName("symbol_code") val symbolCode: String
)

@Serializable
data class MetNextDetails(
    @SerialName("precipitation_amount") val precipitationAmount: Double? = null
)
