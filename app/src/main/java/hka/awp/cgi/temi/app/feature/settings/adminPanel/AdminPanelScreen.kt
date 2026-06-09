package hka.awp.cgi.temi.app.feature.settings.adminPanel

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.ui.components.SettingsHeader
import org.koin.compose.viewmodel.koinViewModel

// ─── Icon view ────────────────────────────────────────────────────────────────

/**
 * Displays a tinted icon inside a rounded background box, consistent with the
 * app-wide icon treatment used on this screen.
 *
 * @param icon The [ImageVector] to render.
 * @param contentDescription Accessibility description for the icon.
 * @param modifier Optional [Modifier].
 */
@Composable
fun ConfigIconBox(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
    }
}

// ─── Cards ────────────────────────────────────────────────────────────────────

/**
 *
 * @param modifier Optional [Modifier].
 * @param onClick Optional click handler. When non-null the card becomes clickable.
 * @param content The composable content rendered inside the card's [Column].
 */
@Composable
fun ConfigCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            content = content
        )
    }
}

/**
 * Displays the webserver URL with a copy-to-clipboard action.
 *
 * @param url The webserver URL string to display.
 */
@Composable
fun WebserverUrlCard(url: String) {
    ConfigCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.Language,
                contentDescription = "Webserver-URL" // stringResource(R.string.config_webserver_url)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigLabel(stringResource(R.string.admin_panel_webserver_url))
                ConfigValue(url)
            }
        }
    }
}

/**
 * Displays the current app version alongside a "Latest" badge when applicable.
 *
 * @param version The version string (e.g. "v1.4.2").
 */
@Composable
fun AppVersionCard(version: String) {
    ConfigCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.Info,
                contentDescription = "AppVersion" // stringResource(R.string.config_app_version)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigLabel(stringResource(R.string.admin_panel_appversion))
                ConfigValue(version)
            }
        }
    }
}

/**
 * Displays a navigable card for the MQTT reports section.
 *
 * @param onNavigate Callback invoked when the row is tapped.
 */
@Composable
fun MqttReportsCard(onNavigate: () -> Unit) {
    ConfigCard(onClick = onNavigate) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.Storage,
                contentDescription = "Mqtt-reports" // stringResource(R.string.config_mqtt_reports)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue(stringResource(R.string.admin_panel_mqtt_reports))
                ConfigSubtext(stringResource(R.string.admin_panel_mqtt_reports_subtitle))
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Displays the webserver password row with masked dots and a "Change" action.
 *
 * @param onChangePassword Callback invoked when "Change" is tapped.
 */
@Composable
fun WebserverPasswordCard(
    onChangePassword: () -> Unit
) {
    ConfigCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.Lock,
                contentDescription = "Webserver password" // stringResource(R.string.config_password)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue(stringResource(R.string.admin_panel_webserver_password))
                PasswordDots()
            }
            Text(
                text = stringResource(R.string.admin_panel_webserver_password_change),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onChangePassword)
            )
        }
    }
}

@Composable
fun ChangePasswordDialog(
    onConfirm: (oldPassword: String, newPassword: String) -> Unit,
    onDismiss: () -> Unit
                        ) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var oldVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }

    val passwordsMatch = newPassword == confirmPassword
    val confirmError = confirmPassword.isNotEmpty() && !passwordsMatch

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(imageVector = Icons.Outlined.Lock, contentDescription = null)
        },
        title = {
            Text(stringResource(R.string.admin_panel_change_password_title))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = oldPassword,
                    onValueChange = { oldPassword = it },
                    label = { Text(stringResource(R.string.admin_panel_old_password)) },
                    visualTransformation = if (oldVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { oldVisible = !oldVisible }) {
                            Icon(
                                imageVector = if (oldVisible) Icons.Outlined.VisibilityOff
                                else Icons.Outlined.Visibility,
                                contentDescription = if (oldVisible) "Hide password"
                                else "Show password"
                                )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                                 )

                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(stringResource(R.string.admin_panel_new_password)) },
                    visualTransformation = if (newVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { newVisible = !newVisible }) {
                            Icon(
                                imageVector = if (newVisible) Icons.Outlined.VisibilityOff
                                else Icons.Outlined.Visibility,
                                contentDescription = if (newVisible) "Hide password"
                                else "Show password"
                                )
                        }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                                 )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(stringResource(R.string.admin_panel_confirm_password)) },
                    visualTransformation = if (confirmVisible) VisualTransformation.None
                    else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { confirmVisible = !confirmVisible }) {
                            Icon(
                                imageVector = if (confirmVisible) Icons.Outlined.VisibilityOff
                                else Icons.Outlined.Visibility,
                                contentDescription = if (confirmVisible) "Hide password"
                                else "Show password"
                                )
                        }
                    },
                    isError = confirmError,
                    supportingText = if (confirmError) {
                        { Text(stringResource(R.string.admin_panel_passwords_no_match)) }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                                 )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(oldPassword, newPassword) },
                enabled = oldPassword.isNotEmpty() && newPassword.isNotEmpty() && passwordsMatch
                  ) {
                Text(stringResource(R.string.admin_panel_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.admin_panel_cancel))
            }
        }
               )
}

/**
 * Displays coordinate management with edit and navigate actions.
 *
 * @param coordinates The formatted coordinate string to display.
 * @param onEdit Callback invoked when the edit icon is tapped.
 */
@Composable
fun CoordinateManagementCard(
    coordinates: String,
    onEdit: () -> Unit,
) {
    ConfigCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.LocationOn,
                contentDescription = "Coordinates" // stringResource(R.string.config_coordinates)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue(stringResource(R.string.admin_panel_weather_coordinates))
                ConfigSubtext(coordinates)
            }
            Text(
                text = stringResource(R.string.admin_panel_webserver_coordiantes_change),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onEdit)
            )
        }
    }
}

@Composable
fun EditCoordinatesDialog(
    initialLatitude: Double,
    initialLongitude: Double,
    onConfirm: (latitude: Double, longitude: Double) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
                         ) {
    var latitudeInput by remember { mutableStateOf(initialLatitude.toString()) }
    var longitudeInput by remember { mutableStateOf(initialLongitude.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.LocationOn,
                contentDescription = null
                )
        },
        title = {
            Text(text = stringResource(R.string.admin_panel_webserver_coordiantes_change))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = latitudeInput,
                    onValueChange = { latitudeInput = it },
                    label = { Text(stringResource(R.string.admin_panel_latitude)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                                 )
                OutlinedTextField(
                    value = longitudeInput,
                    onValueChange = { longitudeInput = it },
                    label = { Text(stringResource(R.string.admin_panel_longitude)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                                 )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val lat = latitudeInput.toDoubleOrNull() ?: return@Button
                    val lon = longitudeInput.toDoubleOrNull() ?: return@Button
                    onConfirm(lat, lon)
                }
                  ) {
                Text(stringResource(R.string.admin_panel_confirm))
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onReset) {
                    Text(stringResource(R.string.admin_panel_reset_defaults))
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.admin_panel_cancel))
                }
            }
        }
               )
}

// ─── Typography helpers ───────────────────────────────────────────────────────

/** Small all-caps label rendered above a value (e.g. "WEBSERVER URL"). */
@Composable
private fun ConfigLabel(text: String) {
    Text(
        text = text,
        fontSize = 10.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.8.sp
    )
}

/** Primary value text inside a configuration card. */
@Composable
private fun ConfigValue(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurface
    )
}

/** Secondary subtext beneath the primary value. */
@Composable
private fun ConfigSubtext(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Renders a row of filled circles to visually mask a password. */
@Composable
private fun PasswordDots(count: Int = 7) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(count) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

// ─── Main screen ─────────────────────────────────────────────────────────────

@Composable
fun AdminPanelScreen(
    onBackClick: () -> Unit,
    viewModel: AdminPanelViewModel = koinViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCoordinateDialog by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    if (showCoordinateDialog) {
        EditCoordinatesDialog(
            initialLatitude = uiState.latitude,
            initialLongitude = uiState.longitude,
            onConfirm = { lat, lon ->
                viewModel.onEditCoordinates(lat, lon)
                showCoordinateDialog = false
            },
            onReset = {
                viewModel.onResetCoordinates()
                showCoordinateDialog = false
            },
            onDismiss = { showCoordinateDialog = false }
                             )
    }
    if (showPasswordDialog) {
        ChangePasswordDialog(
            onConfirm = { old, new ->
                viewModel.onChangePassword(old, new)
                showPasswordDialog = false
            },
            onDismiss = { showPasswordDialog = false }
                            )
    }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
        ) {
            SettingsHeader(
                title = stringResource(R.string.admin_panel_header),
                onBackClick = onBackClick
            )

            Spacer(
                modifier = Modifier.height(40.dp)
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                WebserverUrlCard(
                    url = uiState.webserverUrl
                )

                AppVersionCard(
                    version = uiState.appVersion,
                )

                MqttReportsCard(
                    onNavigate = viewModel::onOpenMqttReports
                )

                WebserverPasswordCard(
                    onChangePassword = { showPasswordDialog = true }
                )

                CoordinateManagementCard(
                    coordinates = uiState.coordinates,
                    onEdit = { showCoordinateDialog = true }
                )
            }
        }
    }
}
