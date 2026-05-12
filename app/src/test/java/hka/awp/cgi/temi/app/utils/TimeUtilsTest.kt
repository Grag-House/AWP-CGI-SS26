package hka.awp.cgi.temi.app.utils

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class TimeUtilsTest {
    @Test
    fun `getLocalTime returns formatted time correctly with UTC zone`() {
        val instant = Instant.parse("2023-10-27T10:15:30Z")
        val zoneId = ZoneId.of("UTC")
        val clock = Clock.fixed(instant, zoneId)
        val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(zoneId)
        val result = getLocalTime(clock, formatter)
        assertEquals("10:15", result)
    }

    @Test
    fun `getLocalTime returns formatted date and time correctly`() {
        val instant = Instant.parse("2023-10-27T10:15:30Z")
        val zoneId = ZoneId.of("UTC")
        val clock = Clock.fixed(instant, zoneId)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(zoneId)
        val result = getLocalTime(clock, formatter)
        assertEquals("2023-10-27 10:15:30", result)
    }

    @Test
    fun `getLocalTime with ISO_INSTANT formatter`() {
        val instant = Instant.parse("2023-10-27T10:15:30Z")
        val clock = Clock.fixed(instant, ZoneId.of("UTC"))
        val formatter = DateTimeFormatter.ISO_INSTANT
        val result = getLocalTime(clock, formatter)
        assertEquals("2023-10-27T10:15:30Z", result)
    }
}
