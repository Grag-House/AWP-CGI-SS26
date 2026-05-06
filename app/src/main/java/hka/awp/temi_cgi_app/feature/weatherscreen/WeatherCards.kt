package hka.awp.temi_cgi_app.feature.weatherscreen

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
            val data = WeatherData.getHourlyData()

            val hourlyData = mutableListOf<HourlyItem>()

            hourlyData.add(
                HourlyItem(
                    "Jetzt",
                    data[0].symbol,
                    data[0].temp.toString(),
                    data[0].precipitation.toString()
                )
            )

            for (i in 1..(data.size - 1)) {
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

            dailyData.add(
                DailyItem(
                    "Heute",
                    data[0].symbol,
                    data[0].maxTemp.toString(),
                    data[0].minTemp.toString()
                )
            )

            for (i in 1..minOf(6,(data.size - 1))) {
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