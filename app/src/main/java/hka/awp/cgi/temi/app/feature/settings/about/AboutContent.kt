package hka.awp.cgi.temi.app.feature.settings.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.data.model.RobotInfo
import hka.awp.cgi.temi.app.feature.settings.SettingsItem

/**
 * Screen for system settings.
 *
 * @param modifier Layout modifier
 * @param aboutInfo Robot information object to show in the dialog
 * @param onItemClick Handler for settings clicks
 * @param onDismissAbout Handler to close the dialog
 */
@Suppress("LongMethod")
@Composable
fun AboutContent(
    modifier: Modifier = Modifier,
    onItemClick: (SettingsItem) -> Unit,
    aboutInfo: RobotInfo? = null,
    onDismissAbout: () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 32.dp,
                top = 32.dp,
                end = 32.dp
            )
    ) {
        Spacer(
            modifier = Modifier.height(24.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = stringResource(
                    R.string.settings
                ),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(
                modifier = Modifier.width(16.dp)
            )

            Text(
                text = buildAnnotatedString {
                    append(
                        stringResource(
                            R.string.settings_page_prefix
                        )
                    )

                    append(" ")

                    withStyle(
                        SpanStyle(
                            color = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        append(
                            stringResource(
                                R.string.settings
                            )
                        )
                    }
                },
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(
            modifier = Modifier.height(48.dp)
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            SettingsItem.settingsItems.forEach { item ->
                SettingsOptionCard(
                    title = stringResource(item.titleRes),
                    subtitle = stringResource(item.subtitleRes),
                    icon = item.icon,
                    onClick = { onItemClick(item) }
                )
            }
        }

        if (aboutInfo != null) {
            AlertDialog(
                onDismissRequest = onDismissAbout,
                confirmButton = {
                    TextButton(
                        onClick = onDismissAbout
                    ) {
                        Text(
                            text = stringResource(
                                R.string.close
                            )
                        )
                    }
                },
                title = {
                    Text(
                        text = stringResource(
                            R.string.system_information
                        ),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement =
                        Arrangement.spacedBy(12.dp)
                    ) {
                        InfoItem(
                            label = stringResource(
                                R.string.ip_address
                            ),
                            value = aboutInfo.ip
                        )

                        InfoItem(
                            label = stringResource(
                                R.string.model
                            ),
                            value = aboutInfo.model
                        )

                        InfoItem(
                            label = stringResource(
                                R.string.serial_number
                            ),
                            value = aboutInfo.serial
                        )

                        InfoItem(
                            label = stringResource(
                                R.string.software_version
                            ),
                            value = aboutInfo.appVersion
                        )

                        InfoItem(
                            label = stringResource(
                                R.string.robox_version
                            ),
                            value = aboutInfo.roboxVersion

                        )

                        InfoItem(
                            label = stringResource(
                                R.string.launcher_version
                            ),
                            value = aboutInfo.launcherVersion
                        )
                    }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor =
                MaterialTheme.colorScheme.surfaceContainerHigh
            )
        }
    }
}
