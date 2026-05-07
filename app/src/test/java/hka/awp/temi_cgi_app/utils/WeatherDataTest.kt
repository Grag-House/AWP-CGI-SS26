package hka.awp.temi_cgi_app.utils

import hka.awp.temi_cgi_app.feature.weatherscreen.WeatherData
import hka.awp.temi_cgi_app.feature.weatherscreen.WeatherIcon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class WeatherDataTest {
    //TODO() add api test

    @Test
    fun `convertSymbolToIcon maps clearsky to SUN`() {
        assertEquals(WeatherIcon.SUN, WeatherData.convertSymbolToIcon("clearsky"))
    }

    @Test
    fun `convertSymbolToIcon maps rain to RAIN`() {
        assertEquals(WeatherIcon.RAIN, WeatherData.convertSymbolToIcon("rain"))
    }

    @Test
    fun `convertSymbolToIcon maps fog to FOG`() {
        assertEquals(WeatherIcon.FOG, WeatherData.convertSymbolToIcon("fog"))
    }

    @Test
    fun `convertSymbolToIcon maps snow to SNOW`() {
        assertEquals(WeatherIcon.SNOW, WeatherData.convertSymbolToIcon("snow"))
    }

    @Test
    fun `convertSymbolToIcon maps thunder to THUNDER`() {
        assertEquals(WeatherIcon.THUNDER, WeatherData.convertSymbolToIcon("rainandthunder"))
    }

    @Test
    fun `convertSymbolToIcon returns SUN for unknown symbol`() {
        assertEquals(WeatherIcon.SUN, WeatherData.convertSymbolToIcon("unknownsymbol"))
    }
}