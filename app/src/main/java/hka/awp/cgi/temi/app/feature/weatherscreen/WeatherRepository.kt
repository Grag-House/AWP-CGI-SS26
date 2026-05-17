package hka.awp.cgi.temi.app.feature.weatherscreen

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Repository responsible for fetching and processing weather data from the MET Norway Weather API.
 *
 * This class handles the network communication using [OkHttpClient], parses the JSON response
 * into domain models, and transforms the raw time-series data into a [WeatherState] containing
 * hourly and weekly forecasts.
 *
 * @property client The [OkHttpClient] used to perform network requests.
 * @property json The [Json] instance used for deserializing the API response, configured to ignore unknown keys.
 */
class WeatherRepository(
    private val client: OkHttpClient,
    private val json: Json = Json { ignoreUnknownKeys = true }
) {
    companion object {
        // TODO replace with CGI specific user agent
        private const val USER_AGENT = "https://github.com/Grag-House/AWP-CGI-SS26"

        // TODO move  to  method which reads the current location from temi
        @Suppress("MagicNumber")
        val LATITUDE: Double by lazy { 49.0138 }

        @Suppress("MagicNumber")
        val LONGITUDE: Double by lazy { 8.3573 }
        private const val MET_API_ENDPOINT =
            "https://api.met.no/weatherapi/locationforecast/2.0/compact"

        private val API_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }

    private val weekdayFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())

    suspend fun getWeatherData(): Result<WeatherState> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val request = Request.Builder()
                    .url("$MET_API_ENDPOINT?lat=$LATITUDE&lon=$LONGITUDE")
                    .header("User-Agent", USER_AGENT)
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        WeatherApiException(response.code, "API Error: ${response.code}")
                    )
                }

                val body = response.body.string()
                val metResponse = json.decodeFromString<MetResponse>(body)
                Result.success(transformToState(metResponse))
            } catch (e: CancellationException) {
                throw e
            } catch (e: UnknownHostException) {
                Timber.e("Possibly no internet connection: ${e.message}")
                Result.failure(Exception("Check your internet connection"))
            } catch (e: SerializationException) {
                Timber.e("Error during JSON deserialization occured: ${e.message}")
                Result.failure(Exception("An Error occurred while parsing the JSON data"))
            } catch (
                @Suppress("TooGenericExceptionCaught")
                e: Exception
            ) {
                Timber.e(e, "Unknown error during the weather API call")
                Result.failure(e)
            }
        }

    private fun transformToState(response: MetResponse): WeatherState {
        val timeseries = response.properties.timeseries
        val today = LocalDate.now()
        // end of the week

        @Suppress("MagicNumber")
        val endDate = today.plusDays(7)

        // take entries for the next 10 hours
        @Suppress("MagicNumber")
        val hourly = timeseries.take(10).map { entry ->
            HourlyItem(
                label = entry.time.substring(11, 16),
                icon = convertSymbolToIcon(entry.data.next1Hours?.summary?.symbolCode ?: "clearsky"),
                temp = entry.data.instant.details.airTemperature.roundToInt().toString(),
                precipitation = entry.data.next1Hours?.details?.precipitationAmount?.toString() ?: "0.0"
            )
        }

        // Daily
        val dailyEntries = mutableMapOf<LocalDate, MutableList<Pair<Double, String?>>>()
        timeseries.forEach { entry ->
            @Suppress("MagicNumber")
            val dateString = entry.time.substring(0, 10)
            val date = LocalDate.parse(dateString, API_DATE_FORMATTER)

            if ((date.isEqual(today) || date.isAfter(today)) && date.isBefore(endDate)) {
                val temp = entry.data.instant.details.airTemperature
                val symbol = entry.data.next1Hours?.summary?.symbolCode
                    ?: entry.data.next6Hours?.summary?.symbolCode
                    ?: entry.data.next12Hours?.summary?.symbolCode
                dailyEntries.getOrPut(date) { mutableListOf() }.add(temp to symbol)
            }
        }

        val weekly = dailyEntries.entries.sortedBy { it.key }.map { (date, entries) ->
            val temps = entries.map { it.first }
            val dominantSymbol = entries.mapNotNull { it.second }
                .groupingBy { it }.eachCount()
                .maxByOrNull { it.value }?.key ?: "clearsky"

            DailyItem(
                day = date.format(weekdayFormatter),
                icon = convertSymbolToIcon(dominantSymbol),
                high = temps.maxOrNull()?.roundToInt()?.toString() ?: "0",
                low = temps.minOrNull()?.roundToInt()?.toString() ?: "0"
            )
        }

        return WeatherState(
            hourlyForecast = hourly,
            weeklyForecast = weekly
        )
    }

    private fun convertSymbolToIcon(symbol: String): WeatherIcon {
        val base = symbol.substringBefore("_")
        return when {
            base == "clearsky" || base == "fair" -> WeatherIcon.SUN
            base == "partlycloudy" -> WeatherIcon.SUN_CLOUD
            base == "cloudy" -> WeatherIcon.CLOUD
            base == "fog" -> WeatherIcon.FOG
            base.contains("thunder") -> WeatherIcon.THUNDER
            base.contains("sleet") || base.contains("snow") -> WeatherIcon.SNOW
            base.contains("rain") -> WeatherIcon.RAIN
            else -> WeatherIcon.SUN
        }
    }
}
