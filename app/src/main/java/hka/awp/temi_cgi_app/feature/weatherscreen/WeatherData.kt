package hka.awp.temi_cgi_app.feature.weatherscreen

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class WeatherData {
    data class WeatherItem(
        val time: String,
        val temp: Double,
        val precipitation: Double,
        val symbol: WeatherIcon
    )
    companion object hourlyDataObject {
        fun getData(): List<WeatherItem> {
            val lat = 49.01373022923785;
            val lon = 8.357329493642128;

            val client = OkHttpClient();

            val request = Request.Builder()
                .url("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=$lat&lon=$lon")
                .header("User-Agent", "https://github.com/Grag-House/AWP-CGI-SS26")
                .get()
                .build();

            val weatherDatas =
                mutableListOf<WeatherItem>() //list that will contain the received weather data for the next 10 hours

            try {
                val response = client.newCall(request).execute();
                val json = JSONObject(response.body.string());
                val timeseries = json
                    .getJSONObject("properties")
                    .getJSONArray("timeseries");

                for (i in 0 until minOf(10, timeseries.length())) {
                    val entry = timeseries.getJSONObject(i);
                    val time = entry.getString("time");

                    val data = entry.getJSONObject("data");

                    val temperature = data
                        .getJSONObject("instant")
                        .getJSONObject("details")
                        .getDouble("air_temperature");

                    val next1 = data.getJSONObject("next_1_hours");
                    val precipitation =
                        next1.getJSONObject("details").getDouble("precipitation_amount");
                    val symbolCode = next1.getJSONObject("summary").getString("symbol_code");

                    weatherDatas.add(WeatherItem(time, temperature, precipitation, convertSymbolToIcon(symbolCode)))

                }
            } catch (e: Exception) {
                for (i in 0..9) {
                    weatherDatas.add(WeatherItem("the time", i.toDouble(), i.toDouble(),
                        WeatherIcon.SUN))
                }
            }
            return weatherDatas
        }
        /**
         * Converts the symbol codes received from the MET api into WeatherIcons to be displayed
         * symbol codes taken from: https://github.com/metno/weathericons/blob/main/weather/README.md
         */
        fun convertSymbolToIcon(symbol: String): WeatherIcon{
            when(symbol){
                "clearsky" -> return WeatherIcon.SUN
                "fair" -> return WeatherIcon.SUN
                "partlycloudy" -> return WeatherIcon.SUN_CLOUD
                "cloudy" -> return WeatherIcon.CLOUD
                "lightrainshowers" -> return WeatherIcon.RAIN
                "rainshowers" -> return WeatherIcon.RAIN
                "heavyrainshowers" -> return WeatherIcon.RAIN
                "lightrainshowersandthunder" -> return WeatherIcon.CLOUD// for these the icon has to be implemented and then the correct WeatherIon chosen here
                "rainshowersandthunder" -> return WeatherIcon.CLOUD//
                "heavyrainshowersandthunder" -> return WeatherIcon.CLOUD//
                "lightsleetshowers" -> return WeatherIcon.CLOUD//
                "sleetshowers" -> return WeatherIcon.CLOUD//
                "heavysleetshowers" -> return WeatherIcon.CLOUD//
                "lightssleetshowersandthunder" -> return WeatherIcon.CLOUD//
                "sleetshowersandthunder" -> return WeatherIcon.CLOUD//
                "heavysleetshowersandthunder" -> return WeatherIcon.CLOUD//
                "lightsnowshowers" -> return WeatherIcon.CLOUD//
                "snowshowers" -> return WeatherIcon.CLOUD//
                "heavysnowshowers" -> return WeatherIcon.CLOUD//
                "lightssnowshowersandthunder" -> return WeatherIcon.CLOUD//
                "snowshowersandthunder" -> return WeatherIcon.CLOUD//
                "heavysnowshowersandthunder" -> return WeatherIcon.CLOUD//
                "lightrain" -> return WeatherIcon.RAIN
                "rain" -> return WeatherIcon.RAIN
                "heavyrain" -> return WeatherIcon.RAIN
                "lightrainandthunder" -> return WeatherIcon.CLOUD//
                "rainandthunder" -> return WeatherIcon.CLOUD//
                "heavyrainandthunder" -> return WeatherIcon.CLOUD//
                "lightsleet" -> return WeatherIcon.CLOUD//
                "sleet" -> return WeatherIcon.CLOUD//
                "heavysleet" -> return WeatherIcon.CLOUD//
                "lightsleetandthunder" -> return WeatherIcon.CLOUD//
                "sleetandthunder" -> return WeatherIcon.CLOUD//
                "heavysleetandthunder" -> return WeatherIcon.CLOUD//
                "lightsnow" -> return WeatherIcon.CLOUD//
                "snow" -> return WeatherIcon.CLOUD//
                "heavysnow" -> return WeatherIcon.CLOUD//
                "lightsnowandthunder" -> return WeatherIcon.CLOUD//
                "snowandthunder" -> return WeatherIcon.CLOUD//
                "heavysnowandthunder" -> return WeatherIcon.CLOUD//
                "fog" -> return WeatherIcon.CLOUD//
            }
        }
    }
}