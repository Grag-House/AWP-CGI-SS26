package hka.awp.temi_cgi_app.data.repository

import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

class RobotRepository {

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

    fun getModelName(): String = android.os.Build.MODEL
}