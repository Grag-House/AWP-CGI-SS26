package hka.awp.cgi.temi.app.feature.controller

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.Context
import timber.log.Timber

/**
 * A specialized utility wrapper that encapsulates connections to the hidden Android Bluetooth HID Profile.
 *
 * Because the platform-level Human Interface Device (HID) profile definitions are marked as hidden (`@hide`)
 * within standard Android Open Source Project (AOSP) framework distributions, they cannot be compiled against
 * directly. This class bypasses those SDK limitations by dynamically discovering and invoking the hidden API
 * surface methods via runtime Java reflection.
 *
 * @property context The component or application context environment.
 * @property bluetoothAdapter The host system's hardware radio controller abstraction interface.
 */
class BluetoothHidConnector(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter?
) {
    companion object {
        /**
         * Hidden constant identifier representing [BluetoothProfile.HID_DEVICE] within the framework internals.
         */
        private const val BLUETOOTH_PROFILE_HID_DEVICE = 4
    }

    /**
     * Establishes an active asynchronous HID profile connection
     * to the remote device matching the specified MAC address.
     *
     * It dynamically requests a profile proxy method (`getProfileProxy`) from the system [BluetoothAdapter].
     * Once the hidden profile service connects asynchronously, it grabs the underlying target class,
     * extracts the hidden `connect` method, and dispatches it against the target [BluetoothDevice].
     *
     * @param address The hardware MAC address of the targeted peripheral.
     */
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
                        Timber.d("HID connect result=$result for $address")
                    } catch (exception: Exception) {
                        Timber.e(exception, "Failed to invoke HID connect via reflection")
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    Timber.d("Bluetooth profile disconnected: profile=$profile")
                }
            }

            getProfileProxyMethod.invoke(adapter, context, listener, BLUETOOTH_PROFILE_HID_DEVICE)
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing runtime permission required for HID connect")
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to connect HID device due to reflection initialization error")
        }
    }

    /**
     * Tears down an active HID profile connection targeting the designated hardware MAC address.
     *
     * Similar to the connection lifecycle routine, this establishes a proxy listener, unearths the
     * hidden `disconnect` method layout inside the dynamic proxy handle at runtime, and fires a
     * disconnect request targeting the peripheral.
     *
     * @param address The hardware MAC address of the targeted peripheral.
     */
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
                        Timber.e(exception, "Failed to invoke HID disconnect via reflection")
                    }
                }

                override fun onServiceDisconnected(profile: Int) {
                    Timber.d("Bluetooth profile disconnected: profile=$profile")
                }
            }

            adapter.getProfileProxy(context, listener, BLUETOOTH_PROFILE_HID_DEVICE)
        } catch (exception: SecurityException) {
            Timber.e(exception, "Missing runtime permission required for HID disconnect")
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to disconnect HID device due to reflection initialization error")
        }
    }
}
