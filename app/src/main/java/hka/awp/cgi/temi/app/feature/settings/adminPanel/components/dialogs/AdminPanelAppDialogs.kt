package hka.awp.cgi.temi.app.feature.settings.adminPanel.components.dialogs

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import hka.awp.cgi.temi.app.R

/**
 * Shows an alert dialog asking the administrator for confirmation before shutting down the application.
 *
 * This component uses error-themed styling to highlight the destructive nature of closing the process.
 *
 * @param onConfirm Callback triggered when the user explicitly agrees to exit the application.
 * @param onDismiss Callback triggered when the user rejects the action or clicks outside the modal layout boundaries.
 */
@Composable
fun CloseAppConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.admin_panel_close_app)) },
        text = { Text(stringResource(R.string.admin_panel_close_app_confirm_text)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.admin_panel_confirm_close_app))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.admin_panel_cancel)) } }
    )
}

/**
 * Shows an alert dialog asking the administrator for confirmation before rebooting the application.
 *
 * This component acts as a protective barrier to avoid accidental application restarts during active operations.
 *
 * @param onConfirm Callback triggered when the user confirms they want to proceed with the app reboot.
 * @param onDismiss Callback triggered when the user cancels the operation or dismisses the modal interface.
 */
@Composable
fun RestartAppConfirmationDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Refresh, null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.admin_panel_restart_confirm_title)) },
        text = { Text(stringResource(R.string.admin_panel_restart_confirm_text)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.admin_panel_confirm_restart))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.admin_panel_cancel)) } }
    )
}
