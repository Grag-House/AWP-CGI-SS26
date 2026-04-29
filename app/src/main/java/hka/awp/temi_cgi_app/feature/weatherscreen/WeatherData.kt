package hka.awp.temi_cgi_app.feature.weatherscreen

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject

class WeatherData {
    data class WeatherItem(
        val time: String,
        val temp: Double,
        val precipitation: Double,
        val symbol: String
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

                    weatherDatas.add(WeatherItem(time, temperature, precipitation, symbolCode))

//            println("%-30s %-15s %-20s %s".format(
//                time,
//                "%.1f °C".format(temperature),
//                "%.1f mm".format(precipitation),
//                symbolCode
//            ));
                }
            } catch (e: Exception) {
                for (i in 0..9) {
                    weatherDatas.add(WeatherItem("the time", i.toDouble(), i.toDouble(), "SUN"))
                }
            }
            return weatherDatas
        }
    }
}