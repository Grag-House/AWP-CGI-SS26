package hka.awp.cgi.temi.app.feature.weatherscreen

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WeatherData {

    data class WeatherItemHour(
        val time: String,
        val temp: Double,
        val precipitation: Double,
        val symbol: WeatherIcon
    )
    companion object GetDataObject {
        fun getHourlyData(): List<WeatherItemHour> {
            val lat = 49.0138 // only 4 decimal places as recommended by the MET
            val lon = 8.3573

            val client = OkHttpClient()

            val request = Request.Builder()
                .url("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=$lat&lon=$lon")
                .header(
                    "User-Agent",
                    "https://github.com/Grag-House/AWP-CGI-SS26"
                ) // has to be an email-address or GitHub repo per MET regulations
                .get()
                .build()

            val weatherDatas =
                mutableListOf<WeatherItemHour>() // list that will contain the received weather data for the next 10 hours

            try {
                val response = client.newCall(request).execute()
                val json = JSONObject(response.body.string())
                val timeseries = json
                    .getJSONObject("properties")
                    .getJSONArray("timeseries")

                for (i in 0 until minOf(10, timeseries.length())) {
                    val entry = timeseries.getJSONObject(i)
                    val time = entry.getString("time")

                    val data = entry.getJSONObject("data")

                    val temperature = data
                        .getJSONObject("instant")
                        .getJSONObject("details")
                        .getDouble("air_temperature")

                    val next1 = data.getJSONObject("next_1_hours")
                    val precipitation =
                        next1.getJSONObject("details").getDouble("precipitation_amount")
                    val symbolCode = next1.getJSONObject("summary").getString("symbol_code")

                    weatherDatas.add(WeatherItemHour(time, temperature, precipitation, convertSymbolToIcon(symbolCode)))
                }
            } catch (e: Exception) {
                for (i in 0..9) {
                    weatherDatas.add(
                        WeatherItemHour(
                            "the time",
                            i.toDouble(),
                            i.toDouble(),
                            WeatherIcon.SUN
                        )
                    )
                }
//                weatherDatas.add((WeatherItemHour("jetzt", 1.0,1.0, WeatherIcon.THUNDER))) // for icon testing
//                weatherDatas.add((WeatherItemHour("2 Uhr", 2.0,1.0, WeatherIcon.THUNDER)))
//                weatherDatas.add((WeatherItemHour("3 Uhr", 3.0,1.0, WeatherIcon.SNOW)))
//                weatherDatas.add((WeatherItemHour("4 Uhr", 4.0,1.0, WeatherIcon.FOG)))
//                weatherDatas.add((WeatherItemHour("5 Uhr", 5.0,1.0, WeatherIcon.RAIN)))
//                weatherDatas.add((WeatherItemHour("6 Uhr", 6.0,1.0, WeatherIcon.SUN_CLOUD)))
//                weatherDatas.add((WeatherItemHour("7 Uhr", 7.0,1.0, WeatherIcon.CLOUD)))
//                weatherDatas.add((WeatherItemHour("8 Uhr", 8.0,1.0, WeatherIcon.SUN)))
//                weatherDatas.add((WeatherItemHour("9 Uhr", 9.0,1.0, WeatherIcon.SUN)))
//                weatherDatas.add((WeatherItemHour("10 Uhr", 10.0,1.0, WeatherIcon.SUN)))
            }
            return weatherDatas
        }

        data class WeatherItemDay(
            val weekday: String,
            val minTemp: Double,
            val maxTemp: Double,
            val symbol: WeatherIcon
        )

        fun getWeeklyData(): List<WeatherItemDay> {
            val lat = 49.0138
            val lon = 8.3573

            val client = OkHttpClient()

            val request = Request.Builder()
                .url("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=$lat&lon=$lon")
                .header("User-Agent", "https://github.com/Grag-House/AWP-CGI-SS26")
                .get()
                .build()

            // key: "yyyy-MM-dd" String statt LocalDate
            val dailyEntries = mutableMapOf<String, MutableList<Pair<Double, String?>>>()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val today = dateFormat.format(Date())
            val calEnd = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
            val endDate = dateFormat.format(calEnd.time)

            try {
                val response = client.newCall(request).execute()
                val json = JSONObject(response.body.string())
                val timeseries = json
                    .getJSONObject("properties")
                    .getJSONArray("timeseries")

                for (i in 0 until timeseries.length()) {
                    val entry = timeseries.getJSONObject(i)
                    val date = entry.getString("time").substring(0, 10) // "yyyy-MM-dd"

                    if (date < today || date >= endDate) continue

                    val data = entry.getJSONObject("data")

                    val temperature = data
                        .getJSONObject("instant")
                        .getJSONObject("details")
                        .getDouble("air_temperature")

                    val symbolCode = (
                        data.optJSONObject("next_1_hours")
                            ?: data.optJSONObject("next_6_hours")
                            ?: data.optJSONObject("next_12_hours")
                        )?.getJSONObject("summary")?.getString("symbol_code")

                    dailyEntries.getOrPut(date) { mutableListOf() }
                        .add(Pair(temperature, symbolCode))
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val dailyDatas = mutableListOf<WeatherItemDay>()
                for (i in 0..9) {
                    dailyDatas.add(
                        WeatherItemDay(
                            "the day",
                            i.toDouble(),
                            i.toDouble(),
                            WeatherIcon.SUN
                        )
                    )
                }
                return dailyDatas
            }

            return dailyEntries.entries
                .sortedBy { it.key }
                .map { (dateStr, entries) ->
                    val temps = entries.map { it.first }
                    val symbol = entries.mapNotNull { it.second }
                        .groupingBy { it }.eachCount()
                        .maxByOrNull { it.value }
                        .let {
                            it?.key ?: "clearsky"
                        } // clearsky is a fallback if the string would be null so that at least any icon is displayed

                    WeatherItemDay(
                        weekday = getGermanWeekday(dateStr, dateFormat),
                        minTemp = temps.min(),
                        maxTemp = temps.max(),
                        symbol = convertSymbolToIcon(symbol)
                    )
                }
        }
        fun getGermanWeekday(dateStr: String, dateFormat: SimpleDateFormat): String {
            val weekdays = arrayOf("Sonntag", "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag")
            val cal = Calendar.getInstance().apply {
                time = dateFormat.parse(dateStr) ?: return "?"
            }
            return weekdays[cal.get(Calendar.DAY_OF_WEEK) - 1]
        }

        /**
         * Converts the symbol codes received from the MET api into WeatherIcons to be displayed
         * symbol codes taken from: https://github.com/metno/weathericons/blob/main/weather/README.md
         */
        fun convertSymbolToIcon(symbol: String): WeatherIcon {
            when (symbol) {
                "clearsky" -> return WeatherIcon.SUN
                "fair" -> return WeatherIcon.SUN
                "partlycloudy" -> return WeatherIcon.SUN_CLOUD
                "cloudy" -> return WeatherIcon.CLOUD
                "lightrainshowers" -> return WeatherIcon.RAIN
                "rainshowers" -> return WeatherIcon.RAIN
                "heavyrainshowers" -> return WeatherIcon.RAIN
                "lightrainshowersandthunder" -> return WeatherIcon.THUNDER
                "rainshowersandthunder" -> return WeatherIcon.THUNDER
                "heavyrainshowersandthunder" -> return WeatherIcon.THUNDER
                "lightsleetshowers" -> return WeatherIcon.SNOW
                "sleetshowers" -> return WeatherIcon.SNOW
                "heavysleetshowers" -> return WeatherIcon.SNOW
                "lightssleetshowersandthunder" -> return WeatherIcon.THUNDER
                "sleetshowersandthunder" -> return WeatherIcon.THUNDER
                "heavysleetshowersandthunder" -> return WeatherIcon.THUNDER
                "lightsnowshowers" -> return WeatherIcon.SNOW
                "snowshowers" -> return WeatherIcon.SNOW
                "heavysnowshowers" -> return WeatherIcon.SNOW
                "lightssnowshowersandthunder" -> return WeatherIcon.THUNDER
                "snowshowersandthunder" -> return WeatherIcon.THUNDER
                "heavysnowshowersandthunder" -> return WeatherIcon.THUNDER
                "lightrain" -> return WeatherIcon.RAIN
                "rain" -> return WeatherIcon.RAIN
                "heavyrain" -> return WeatherIcon.RAIN
                "lightrainandthunder" -> return WeatherIcon.THUNDER
                "rainandthunder" -> return WeatherIcon.THUNDER
                "heavyrainandthunder" -> return WeatherIcon.THUNDER
                "lightsleet" -> return WeatherIcon.SNOW
                "sleet" -> return WeatherIcon.SNOW
                "heavysleet" -> return WeatherIcon.SNOW
                "lightsleetandthunder" -> return WeatherIcon.THUNDER
                "sleetandthunder" -> return WeatherIcon.THUNDER
                "heavysleetandthunder" -> return WeatherIcon.THUNDER
                "lightsnow" -> return WeatherIcon.SNOW
                "snow" -> return WeatherIcon.SNOW
                "heavysnow" -> return WeatherIcon.SNOW
                "lightsnowandthunder" -> return WeatherIcon.THUNDER
                "snowandthunder" -> return WeatherIcon.THUNDER
                "heavysnowandthunder" -> return WeatherIcon.THUNDER
                "fog" -> return WeatherIcon.FOG
            }
            return WeatherIcon.SUN
        }
    }
}
