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

        fun setDailyWeatherCards(): MutableList<DailyItem>{
            val data = WeatherData.getWeeklyData()

            val dailyData = mutableListOf<DailyItem>()

            for (i in 0..(data.size - 1)) {
                dailyData.add(
                    DailyItem(
                        data.get(i).weekday,
                        data.get(i).symbol,
                        data.get(i).maxTemp.toString(),
                        data.get(i).minTemp.toString()
                    )
                )
            }
            return dailyData
        }
    }

}