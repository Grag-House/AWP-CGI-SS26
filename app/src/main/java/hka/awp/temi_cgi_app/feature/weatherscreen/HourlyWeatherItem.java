package hka.awp.temi_cgi_app.feature.weatherscreen;

import static java.sql.DriverManager.println;

import androidx.compose.ui.graphics.vector.ImageVector;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

class HourlyWeatherItem {

    fun getData() throws IOException {
        double lat = 49.01373022923785;
        double lon = 8.357329493642128;

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=$lat&lon=$lon")
                .header("User-Agent", "MyWeatherApp/1.0 your@email.com")
                .get()
                .build();

        try {
            Response response = client.newCall(request).execute();
            var json = new JSONObject(response.body().string());
            var timeseries = json
                    .getJSONObject("properties")
                    .getJSONArray("timeseries");

            println("%-30s %-15s %-20s %s".format("Time (UTC)", "Temp (°C)", "Precip next 1h (mm)", "Symbol"));
            println("-".repeat(85));

            for(i in 0 < .. < 10) {
                println(i);
            }

            for (i in 0 until minOf(10, timeseries.length())){
                var entry = timeseries.getJSONObject(i);
                var time = entry.getString("time");

                var data = entry.getJSONObject("data");

                var temperature = data
                        .getJSONObject("instant")
                        .getJSONObject("details")
                        .getDouble("air_temperature");

                var next1 = data.getJSONObject("next_1_hours");
                var precipitation = next1.getJSONObject("details").getDouble("precipitation_amount");
                var symbolCode = next1.getJSONObject("summary").getString("symbol_code");

                println("%-30s %-15s %-20s %s".format(
                        String.valueOf(time),
                        "%.1f °C".format(String.valueOf(temperature)),
                        "%.1f mm".format(String.valueOf(precipitation)),
                        symbolCode
                ));
            }
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }


    }
}
