package hka.awp.cgi.temi.app.feature.settings.about

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.data.repository.RobotInfo
import hka.awp.cgi.temi.app.feature.settings.SettingsItem

/**
 * Screen for system settings.
 *
 * @param modifier Layout modifier
 * @param aboutInfo Robot information object to show in the dialog
 * @param onItemClick Handler for settings clicks
 * @param onDismissAbout Handler to close the dialog
 */
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
                    }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor =
                    MaterialTheme.colorScheme.surfaceContainerHigh
                       )
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
                    ) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
            )

        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
            )

        Spacer(
            modifier = Modifier.height(4.dp)
              )

        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
                         )
    }
}

@Composable
fun SettingsOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit = {}
                      ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(24.dp)
                 )
            .background(
                MaterialTheme.colorScheme.surfaceVariant
                       )
            .clickable {
                onClick()
            }
            .padding(
                horizontal = 24.dp,
                vertical = 22.dp
                    ),
        verticalAlignment = Alignment.CenterVertically
       ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(32.dp)
            )

        Spacer(
            modifier = Modifier.width(20.dp)
              )

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
                )

            Spacer(
                modifier = Modifier.height(4.dp)
                  )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
                )
        }
    }
}
