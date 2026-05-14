package hka.awp.cgi.temi.app.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.net.NetworkInterface
import java.util.Collections

data class RobotInfo(
    val ip: String,
    val model: String,
    val serial: String,
    val appVersion: String
)

class RobotRepository {

    fun getFullDeviceInfo(): RobotInfo {
        return RobotInfo(
            ip = "getIpAddress()",
            model = "getModelName()",
            serial = "android.os.Build.getSerial()",
            appVersion = "BuildConfig.VERSION_NAME"
        )
    }

    fun getIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress) {
                        val sAddr = addr.hostAddress
                        val isIPv4 = sAddr.indexOf(':') < 0
                        if (isIPv4) return sAddr
                    }
                }
            }
        } catch (ex: Exception) {
            return "0.0.0.0"
        }
        return "Keine IP gefunden"
    }

    fun getModelName(): String = Build.MODEL

    fun setBrightness(value: Int, context: Context) {
        try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                (value * 255)
                                  )
            // TODO = TemiBrightness
            println("Erfolgreich Brightness verändert.")
        } catch (e: Exception) {
            println("Fehler beim Schreiben: ${e.message}")
        }
    }

    fun setScreenTimeout(millis: Int, context: Context) {
        try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                millis
                                  )
            println("Timeout auf $millis ms gesetzt")
        } catch (e: Exception) {
            println("Fehler beim Timeout: ${e.message}")
        }
    }
}
