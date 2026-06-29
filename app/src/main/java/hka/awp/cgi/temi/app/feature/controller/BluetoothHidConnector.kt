package hka.awp.cgi.temi.app.feature.controller

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import timber.log.Timber

/**
 * Kapselt die via Java-Reflection realisierte Kopplung an das versteckte Android-HID-Profil.
 */
class BluetoothHidConnector(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
                           ) {
    companion object {
        private const val BLUETOOTH_PROFILE_HID_DEVICE = 4
    }

    @Suppress("TooGenericExceptionCaught")
    fun connect(address: String) {
        try {
            val adapter = bluetoothAdapter ?: return
            val device = adapter.getRemoteDevice(address)

            val getProfileProxyMethod = BluetoothAdapter::class.java.getMethod(
                "getProfileProxy",
                Context::class.java,
                BluetoothProfile.ServiceListener::class.java,
                Int::class.javaPrimitiveType,
                                                                              )

            val listener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    try {
                        val connectMethod = proxy.javaClass.getMethod("connect", BluetoothDevice::class.java)
                        val result = connectMethod.invoke(proxy, device)
                        Timber.d("HID connect result=$result for ${address}")
                    } catch (exception: Exception) {
                        Timber.e(exception, "Failed to invoke HID connect")
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    Timber.d("Bluetooth profile disconnected: profile=$profile")
                }
            }

            getProfileProxyMethod.invoke(adapter, context, listener, BLUETOOTH_PROFILE_HID_DEVICE)
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing permission for HID connect")
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to connect HID device via reflection")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    fun disconnect(address: String) {
        try {
            val adapter = bluetoothAdapter ?: return
            val device = adapter.getRemoteDevice(address)

            val listener = object : BluetoothProfile.ServiceListener {
                override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                    try {
                        val disconnectMethod = proxy.javaClass.getMethod("disconnect", BluetoothDevice::class.java)
                        val result = disconnectMethod.invoke(proxy, device)
                        Timber.d("HID disconnect result=$result for $address")
                    } catch (exception: Exception) {
                        Timber.e(exception, "Failed to invoke HID disconnect")
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    Timber.d("Bluetooth profile disconnected: profile=$profile")
                }
            }

            adapter.getProfileProxy(context, listener, BLUETOOTH_PROFILE_HID_DEVICE)
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing permission for HID disconnect")
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to disconnect HID device")
        }
    }
}
