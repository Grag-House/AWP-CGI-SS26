package hka.awp.cgi.temi.app.feature.controller

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ControllerScreen(
    modifier: Modifier = Modifier,
    viewModel: ControllerViewModel = koinViewModel(),
) {
    val controllerEnabled by viewModel.controllerEnabled.collectAsState()
    val devices by viewModel.devices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()

    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissions ->
        val allGranted = permissions.values.all { it }

        if (allGranted) {
            viewModel.startBluetoothScan()
        }
    }

    Column(
        modifier = modifier
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Controllersteuerung aktivieren")

            Spacer(modifier = Modifier.weight(1f))

            Switch(
                checked = controllerEnabled,
                onCheckedChange = viewModel::setControllerEnabled,
            )
        }

        Divider(modifier = Modifier.padding(vertical = 16.dp))

        Button(
            onClick = {
                bluetoothPermissionLauncher.launch(getBluetoothPermissions())
            },
            enabled = !isScanning,
        ) {
            Text(
                if (isScanning) {
                    "Suche läuft..."
                } else {
                    "Bluetooth-Geräte suchen"
                },
            )
        }

        Button(
            onClick = viewModel::logPairedDevices,
        ) {
            Text("Gekoppelte Geräte loggen")
        }

        devices.forEach { device ->
            ControllerDeviceRow(
                device = device,
                onPairClick = {
                    viewModel.pairDevice(device.address)
                },
                onConnectClick = {
                    viewModel.connectHidDevice(device.address)
                },
                onRemoveClick = {
                    viewModel.removeBond(device.address)
                },
                onDisconnectClick = {
                    viewModel.disconnectHidDevice(device.address)
                },
            )
        }
    }
}

@Composable
private fun ControllerDeviceRow(
    device: ControllerDevice,
    onPairClick: () -> Unit,
    onConnectClick: () -> Unit,
    onRemoveClick: () -> Unit,
    onDisconnectClick: () -> Unit,
) {
    val isBonded = device.bondState == BluetoothDevice.BOND_BONDED
    val isBonding = device.bondState == BluetoothDevice.BOND_BONDING
    val isConnected: Boolean

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(device.name)
            Text(device.address)
            Text("Status: ${device.bondStateLabel()}")
        }

        Button(
            onClick = onPairClick,
            enabled = !isBonded && !isBonding,
              ) {
            Text(
                when {
                    isBonded -> "Gekoppelt"
                    isBonding -> "Kopplung läuft..."
                    else -> "Koppeln"
                },
                )
        }

        Button(
            onClick = {
                if (device.isConnected) {
                    onDisconnectClick()
                } else {
                    onConnectClick()
                }
            },
            enabled = device.bondState == BluetoothDevice.BOND_BONDED,
              ) {
            Text(if (device.isConnected) "Trennen" else "Verbinden")
        }

        Button(
            onClick = onRemoveClick,
            enabled = device.bondState == BluetoothDevice.BOND_BONDED,
              ) {
            Text("Entfernen")
        }
    }
}

private fun ControllerDevice.bondStateLabel(): String {
    return when (bondState) {
        BluetoothDevice.BOND_NONE -> "Nicht gekoppelt"
        BluetoothDevice.BOND_BONDING -> "Kopplung läuft..."
        BluetoothDevice.BOND_BONDED -> "Gekoppelt"
        else -> "Unbekannt"
    }
}

private fun getBluetoothPermissions(): Array<String> {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
        )
    }
}
