package hka.awp.temi_cgi_app.feature.weatherscreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Umbrella
import androidx.compose.material.icons.filled.WbCloudy
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class HourlyItem(
    val label: String,       // "Jetzt", "t+1", …
    val icon: WeatherIcon,
    val temp: String,        // "21°"
    val precipitation: String         // "0%"
)

data class DailyItem(
    val day: String,         // "Heute", "Do", …
    val icon: WeatherIcon,
    val high: String,
    val low: String
)

class WeatherCards {

    companion object {
        fun setHourlyWeatherCards(): MutableList<HourlyItem> {
            val data = WeatherData.getData()

            val hourlyData = mutableListOf<HourlyItem>()

            for (i in 0..(data.size - 1)) {
                hourlyData.add(
                    HourlyItem(
                        data.get(i).time,
                        data.get(i).symbol,
                        data.get(i).temp.toString(),
                        data.get(i).precipitation.toString()
                    )
                )
            }
            return hourlyData
        }
    }

}