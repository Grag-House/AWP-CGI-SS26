package hka.awp.temi_cgi_app.feature.weatherscreen

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import hka.awp.temi_cgi_app.R

data class HourlyItem(
    var label: String,       // "Jetzt", "t+1", …
    val icon: WeatherIcon,
    val temp: String,        // "21°"
    val precipitation: String         // "0%"
)

data class DailyItem(
    var day: String,         // "Heute", "Do", …
    val icon: WeatherIcon,
    val high: String,
    val low: String
)

class WeatherCards {

    companion object {

        fun setHourlyWeatherCards(): MutableList<HourlyItem> {
            val data = WeatherData.getHourlyData()

            val hourlyData = mutableListOf<HourlyItem>()

//            hourlyData.add(
//                HourlyItem(
//                    stringResource(R.string.now),
//                    data[0].symbol,
//                    data[0].temp.toString(),
//                    data[0].precipitation.toString()
//                )
//            )

            for (i in 0..(data.size - 1)) {
                hourlyData.add(
                    HourlyItem(
                        data[i].time,
                        data[i].symbol,
                        data[i].temp.toString(),
                        data[i].precipitation.toString()
                    )
                )
            }
            return hourlyData
        }


        fun setDailyWeatherCards(): MutableList<DailyItem>{
            val data = WeatherData.getWeeklyData()

            val dailyData = mutableListOf<DailyItem>()

//            dailyData.add(
//                DailyItem(
//                    stringResource(R.string.today),
//                    data[0].symbol,
//                    data[0].maxTemp.toString(),
//                    data[0].minTemp.toString()
//                )
//            )

            for (i in 0..minOf(6,(data.size - 1))) {
                dailyData.add(
                    DailyItem(
                        data[i].weekday,
                        data[i].symbol,
                        data[i].maxTemp.toString(),
                        data[i].minTemp.toString()
                    )
                )
            }
            return dailyData
        }
    }

}