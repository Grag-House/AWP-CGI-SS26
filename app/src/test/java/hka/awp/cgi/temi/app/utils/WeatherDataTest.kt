package hka.awp.temi_cgi_app.utils

import hka.awp.cgi.temi.app.feature.weatherscreen.WeatherData
import hka.awp.cgi.temi.app.feature.weatherscreen.WeatherIcon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WeatherDataTest {
    //TODO() add api test
    @Test
    fun testGetHourlyData() {
        val result = WeatherData.getHourlyData()
        println("Got ${result.size} items")
        result.forEach { item -> println(item) }
        assert(result.isNotEmpty())
    }

    @Test
    fun `maps clearsky to SUN`() {
        assertEquals(WeatherIcon.SUN, WeatherData.convertSymbolToIcon("clearsky"))
    }

    @Test
    fun `maps rain to RAIN`() {
        assertEquals(WeatherIcon.RAIN, WeatherData.convertSymbolToIcon("rain"))
    }

    @Test
    fun `maps fog to FOG`() {
        assertEquals(WeatherIcon.FOG, WeatherData.convertSymbolToIcon("fog"))
    }

    @Test
    fun `maps snow to SNOW`() {
        assertEquals(WeatherIcon.SNOW, WeatherData.convertSymbolToIcon("snow"))
    }

    @Test
    fun `maps thunder to THUNDER`() {
        assertEquals(WeatherIcon.THUNDER, WeatherData.convertSymbolToIcon("rainandthunder"))
    }

    @Test
    fun `returns SUN for unknown symbol`() {
        assertEquals(WeatherIcon.SUN, WeatherData.convertSymbolToIcon("unknownsymbol"))
    }
}
