package hka.awp.cgi.temi.app.feature.weatherscreen

data class HourlyItem(
    var label: String,
    val icon: WeatherIcon,
    val temp: String,
    val precipitation: String
)

data class DailyItem(
    var day: String,
    val icon: WeatherIcon,
    val high: String,
    val low: String
)

class WeatherCards {

    companion object {

        fun setHourlyWeatherCards(): MutableList<HourlyItem> {
            val data = WeatherData.getHourlyData()

            val hourlyData = mutableListOf<HourlyItem>()

            for (i in 0..<data.size) {
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

        fun setDailyWeatherCards(): MutableList<DailyItem> {
            val data = WeatherData.getWeeklyData()

            val dailyData = mutableListOf<DailyItem>()

            for (i in 0..minOf(6, (data.size - 1))) {
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
