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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// ─── Data models ─────────────────────────────────────────────────────────────

/**
 * Represents the complete UI state for the System Configuration screen.
 *
 * @property webserverUrl The URL of the webserver.
 * @property appVersion The current application version string.
 * @property isLatestVersion Whether the installed version is the latest available.
 * @property webserverPassword The (masked) webserver password.
 * @property coordinates The geographic coordinates, formatted for display.
 */
data class SystemConfigUiState(
    val webserverUrl: String = "",
    val appVersion: String = "",
    val isLatestVersion: Boolean = false,
    val webserverPassword: String = "",
    val coordinates: String = ""
)

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
 * A reusable container that provides consistent styling for configuration rows,
 * mirroring the [WeatherCard] pattern used in the weather feature.
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
 * @param onCopy Callback invoked when the copy icon is tapped.
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
                contentDescription = "webservertest" // stringResource(R.string.config_webserver_url)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigLabel("Webserver-test") // stringResource(R.string.config_webserver_url)
                ConfigValue(url)
            }
        }
    }
}

/**
 * Displays the current app version alongside a "Latest" badge when applicable.
 *
 * @param version The version string (e.g. "v1.4.2").
 * @param isLatest Whether to show the "Latest" badge.
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
                contentDescription = "appversion" // stringResource(R.string.config_app_version)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigLabel("appversion") // stringResource(R.string.config_app_version)
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
                contentDescription = "mqtt-reports" // stringResource(R.string.config_mqtt_reports)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue("mqtt value") // stringResource(R.string.config_mqtt_reports)
                ConfigSubtext("mqtt subtitle") // stringResource(R.string.config_mqtt_subtitle)
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
fun WebserverPasswordCard(onChangePassword: () -> Unit) {
    ConfigCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.Lock,
                contentDescription = "webserver password" // stringResource(R.string.config_password)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue("webserver password value") // stringResource(R.string.config_password)
                PasswordDots()
            }
            Text(
                text = "webserver password", // stringResource(R.string.config_change)
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.clickable(onClick = onChangePassword)
            )
        }
    }
}

/**
 * Displays coordinate management with edit and navigate actions.
 *
 * @param coordinates The formatted coordinate string to display.
 * @param onEdit Callback invoked when the edit icon is tapped.
 * @param onNavigate Callback invoked when the card is tapped.
 */
@Composable
fun CoordinateManagementCard(
    coordinates: String,
    onEdit: () -> Unit,
    onNavigate: () -> Unit
) {
    ConfigCard(onClick = onNavigate) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ConfigIconBox(
                icon = Icons.Outlined.LocationOn,
                contentDescription = "coordinates" // stringResource(R.string.config_coordinates)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                ConfigValue("coordinates") // stringResource(R.string.config_coordinates)
                ConfigSubtext(coordinates)
            }
            IconButton(onClick = onEdit) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "edit?", // stringResource(R.string.config_edit)
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
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

// ─── Top bar ──────────────────────────────────────────────────────────────────

/**
 * Top app bar for the System Configuration screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SystemConfigTopBar() {
    TopAppBar(
        title = {
            Text(
                text = "idk what this is", // stringResource(R.string.config_title)
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

// ─── Main screen ─────────────────────────────────────────────────────────────

/**
 * Main content composable for the System Configuration screen.
 *
 * Observes [SystemConfigViewModel.uiState] and delegates each section to a
 * dedicated card composable, following the same pattern as [WeatherContent].
 *
 * @param viewModel The [SystemConfigViewModel] that provides the UI state and
 *   handles user actions.
 */
@Composable
fun SystemConfigContent(viewModel: AdminPanelViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            SystemConfigTopBar()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "subtitle", // stringResource(R.string.config_subtitle),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

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
                    onChangePassword = viewModel::onChangePassword
                )

                CoordinateManagementCard(
                    coordinates = uiState.coordinates,
                    onEdit = viewModel::onEditCoordinates,
                    onNavigate = viewModel::onOpenCoordinates
                )
            }
        }
    }
}
