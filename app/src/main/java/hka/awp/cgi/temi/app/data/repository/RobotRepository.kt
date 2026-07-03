package hka.awp.cgi.temi.app.data.repository

import android.os.Build
import com.robotemi.sdk.Robot
import hka.awp.cgi.temi.app.BuildConfig
import hka.awp.cgi.temi.app.data.model.RobotInfo
import timber.log.Timber
import java.net.NetworkInterface
import java.net.SocketException
import java.util.Collections

private const val FALLBACK_IP_ADDRESS = "0.0.0.0"
private const val NO_IP_FOUND_MESSAGE = "Keine IP gefunden"

class RobotRepository {

    fun getFullDeviceInfo(robot: Robot?): RobotInfo {
        return RobotInfo(
            ip = getIpAddress(),
            model = getModelName(),
            serial = robot?.serialNumber ?: "Unbekannt",
            appVersion = BuildConfig.VERSION_NAME,
            roboxVersion = robot?.roboxVersion ?: "Unbekannt",
            launcherVersion = robot?.launcherVersion ?: "Unbekannt"
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
}
