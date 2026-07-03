package hka.awp.cgi.temi.app.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NetworkWifi1Bar
import androidx.compose.material.icons.rounded.NetworkWifi2Bar
import androidx.compose.material.icons.rounded.NetworkWifi3Bar
import androidx.compose.material.icons.rounded.SignalWifi0Bar
import androidx.compose.material.icons.rounded.SignalWifi4Bar
import androidx.compose.material.icons.rounded.SignalWifiOff
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class NetworkManagerTest {
    @MockK
    lateinit var mockContext: Context

    @MockK(relaxed = true)
    lateinit var mockConnectivityManager: ConnectivityManager

    @MockK(relaxed = true)
    lateinit var mockWifiManager: WifiManager

    lateinit var networkManager: NetworkManager

    @BeforeEach
    fun setUp() {
        MockKAnnotations.init(this)

        every { mockContext.applicationContext } returns mockContext
        every { mockContext.getSystemService(Context.CONNECTIVITY_SERVICE) } returns
            mockConnectivityManager
        every { mockContext.getSystemService(Context.WIFI_SERVICE) } returns mockWifiManager

        networkManager = NetworkManager(mockContext)
    }

    @Test
    fun `Level 0 icon mapping`() {
        val result = networkManager.getWifiIconForLevel(0)
        assertEquals(Icons.Rounded.SignalWifi0Bar, result)
    }

    @Test
    fun `Level 1 icon mapping`() {
        val result = networkManager.getWifiIconForLevel(1)
        assertEquals(Icons.Rounded.NetworkWifi1Bar, result)
    }

    @Test
    fun `Level 2 icon mapping`() {
        val result = networkManager.getWifiIconForLevel(2)
        assertEquals(Icons.Rounded.NetworkWifi2Bar, result)
    }

    @Test
    fun `Level 3 icon mapping`() {
        val result = networkManager.getWifiIconForLevel(3)
        assertEquals(Icons.Rounded.NetworkWifi3Bar, result)
    }

    @Test
    fun `Level 4 icon mapping`() {
        val result = networkManager.getWifiIconForLevel(4)
        assertEquals(Icons.Rounded.SignalWifi4Bar, result)
    }

    @Test
    fun `Negative level boundary check`() {
        val result = networkManager.getWifiIconForLevel(-1)
        assertEquals(Icons.Rounded.SignalWifiOff, result)
    }

    @Test
    fun `Upper out of bounds check`() {
        val result = networkManager.getWifiIconForLevel(5)
        assertEquals(Icons.Rounded.SignalWifiOff, result)
    }

    @Test
    fun `Integer Minimum value handling`() {
        val result = networkManager.getWifiIconForLevel(Int.MIN_VALUE)
        assertEquals(Icons.Rounded.SignalWifiOff, result)
    }

    @Test
    fun `Integer Maximum value handling`() {
        val result = networkManager.getWifiIconForLevel(Int.MAX_VALUE)
        assertEquals(Icons.Rounded.SignalWifiOff, result)
    }
}
