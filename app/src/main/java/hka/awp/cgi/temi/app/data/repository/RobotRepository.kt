package hka.awp.cgi.temi.app.data.repository

import android.content.Context
import android.os.Build
import android.provider.Settings
import hka.awp.cgi.temi.app.BuildConfig
import timber.log.Timber
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Collections

private const val FALLBACK_IP_ADDRESS = "0.0.0.0"
private const val NO_IP_FOUND_MESSAGE = "Keine IP gefunden"
private const val MAX_BRIGHTNESS = 255
private const val PREFS_NAME = "settings"
private const val DARK_MODE_KEY = "dark_mode"

data class RobotInfo(
    val ip: String,
    val model: String,
    val serial: String,
    val appVersion: String
                    )

class RobotRepository {

    fun getFullDeviceInfo(): RobotInfo {
        return RobotInfo(
            ip = getIpAddress(),
            model = getModelName(),
            serial = Build.SERIAL,
            appVersion = BuildConfig.VERSION_NAME
                        )
    }

    fun getIpAddress(): String {
        return try {
            findIpv4Address() ?: NO_IP_FOUND_MESSAGE
        } catch (exception: SocketException) {
            Timber.e(exception, "Failed to read network interfaces")
            FALLBACK_IP_ADDRESS
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing permission to read network interfaces")
            FALLBACK_IP_ADDRESS
        }
    }

    private fun findIpv4Address(): String? {
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())

        return interfaces
            .flatMap { networkInterface ->
                Collections.list(networkInterface.inetAddresses)
            }
            .firstOrNull { address ->
                !address.isLoopbackAddress && address.hostAddress?.contains(":") == false
            }
            ?.hostAddress
    }

    fun getModelName(): String = Build.MODEL

    fun setBrightness(value: Int, context: Context) {
        try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                value * MAX_BRIGHTNESS
                                  )
            Timber.d("Brightness changed successfully")
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing permission to change brightness")
        } catch (exception: IllegalArgumentException) {
            Timber.e(exception, "Invalid brightness value")
        }
    }

    fun saveDarkMode(
        enabled: Boolean,
        context: Context
                    ) {
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
                                    )
            .edit()
            .putBoolean(DARK_MODE_KEY, enabled)
            .apply()
    }

    fun getDarkMode(
        context: Context
                   ): Boolean {
        return context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
                                           )
            .getBoolean(
                DARK_MODE_KEY,
                false
                       )
    }

    fun setScreenTimeout(millis: Int, context: Context) {
        try {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                millis
                                  )
            Timber.d("Screen timeout changed to $millis ms")
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing permission to change screen timeout")
        } catch (exception: IllegalArgumentException) {
            Timber.e(exception, "Invalid screen timeout value")
        }
    }
}
