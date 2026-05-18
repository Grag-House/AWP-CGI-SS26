package hka.awp.cgi.temi.app.feature.weatherscreen

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class WeatherRepositoryTest {

    private lateinit var repository: WeatherRepository
    private val mockClient = mockk<OkHttpClient>()
    private val mockCall = mockk<Call>()

    private val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())

    @BeforeEach
    fun setup() {
        repository = WeatherRepository(client = mockClient, hourlyFormatter = formatter)
    }

    @Test
    fun `getWeatherData returns success when API call is successful`() = runBlocking {
        val now = java.time.Instant.now().toString()

        // Arrange
        val jsonResponse = """
            {
                "properties": {
                    "timeseries": [
                        {
                            "time": "$now",
                            "data": {
                                "instant": { "details": { "air_temperature": 20.5 } },
                                "next_1_hours": {
                                    "summary": { "symbol_code": "clearsky" },
                                    "details": { "precipitation_amount": 0.0 }
                                }
                            }
                        }
                    ]
                }
            }
        """.trimIndent()

        val response = mockk<Response> {
            every { isSuccessful } returns true
            every { body } returns jsonResponse.toResponseBody()
        }

        every { mockClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns response

        // Act
        val result = repository.getWeatherData()

        // Assert
        assertTrue(result.isSuccess)
        val state = result.getOrThrow()

        // 20.5 is rounded to 21
        assertEquals(1, state.hourlyForecast.size)
        assertEquals("21", state.hourlyForecast[0].temp)
        assertEquals(WeatherIcon.SUN, state.hourlyForecast[0].icon)
    }

    @Test
    fun `getWeatherData returns failure when API call fails`() = runBlocking {
        // Arrange
        val response = mockk<Response> {
            every { isSuccessful } returns false
            every { code } returns 404
        }

        every { mockClient.newCall(any()) } returns mockCall
        every { mockCall.execute() } returns response

        // Act
        val result = repository.getWeatherData()

        // Assert
        assertTrue(result.isFailure)
    }
}
