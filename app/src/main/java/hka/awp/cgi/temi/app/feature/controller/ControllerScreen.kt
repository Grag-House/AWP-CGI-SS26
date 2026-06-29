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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
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
        modifier = modifier.padding(24.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.controller_steering_active))

            Spacer(modifier = Modifier.weight(1f))

            Switch(
                checked = controllerEnabled,
                onCheckedChange = viewModel::setControllerEnabled,
            )
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Button(
                onClick = {
                    if (isScanning) {
                        viewModel.stopBluetoothScan()
                    } else {
                        bluetoothPermissionLauncher.launch(getBluetoothPermissions())
                    }
                },
            ) {
                Text(
                    if (isScanning) {
                        stringResource(R.string.controller_search_cancelled)
                    } else {
                        stringResource(R.string.controller_search_devices)
                    },
                )
            }

            Button(
                onClick = viewModel::loadPairedDevices,
            ) {
                Text(stringResource(R.string.controller_loading_bluetooth_devices))
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
                    onDisconnectClick = {
                        viewModel.disconnectHidDevice(device.address)
                    },
                    onRemoveClick = {
                        viewModel.removeBond(device.address)
                    },
                )
            }
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
            Text(stringResource(R.string.controller_status_label, device.bondStateLabel()))
        }

        Button(
            onClick = onPairClick,
            enabled = !isBonded && !isBonding,
        ) {
            Text(
                when {
                    isBonded -> stringResource(R.string.controller_isBonded)
                    isBonding -> stringResource(R.string.controller_isBonding)
                    else -> stringResource(R.string.controller_bond)
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
            Text(
                if (device.isConnected) {
                    stringResource(
                        R.string.controller_disconnect
                    )
                } else {
                    stringResource(R.string.controller_connect)
                }
            )
        }

        Button(
            onClick = onRemoveClick,
            enabled = device.bondState == BluetoothDevice.BOND_BONDED,
        ) {
            Text(stringResource(R.string.controller_delete))
        }
    }
}

@Composable
private fun ControllerDevice.bondStateLabel(): String {
    return when (bondState) {
        BluetoothDevice.BOND_NONE -> stringResource(R.string.controller_isNotBonded)
        BluetoothDevice.BOND_BONDING -> stringResource(R.string.controller_isBonding)
        BluetoothDevice.BOND_BONDED -> stringResource(R.string.controller_bond)
        else -> stringResource(R.string.unknown)
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
