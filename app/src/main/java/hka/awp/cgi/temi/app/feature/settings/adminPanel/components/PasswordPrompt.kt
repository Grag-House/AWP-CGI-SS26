package hka.awp.cgi.temi.app.feature.settings.adminPanel.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R

/**
 * Renders a full-screen gateway password barrier to intercept unauthenticated
 * users attempting to open the admin console.
 *
 * It manages an internal state variable for masking user input via [PasswordVisualTransformation]. If an
 * external auth sequence flags an invalid attempt via [isError], it surfaces an error layout state
 * and resets its state tracking upon character mutational changes via [onValueChange].
 *
 * @param isError Flag indicating whether the last submitted credential match failed.
 * @param onConfirm Callback supplying the unmasked password string token for verification backend layers.
 * @param onBackClick Intercepts the rejection or back-navigation button step to exit the barrier.
 * @param onValueChange Callback fired immediately when user keystrokes occur to clear external error flags.
 */
@Composable
fun AdminPasswordPrompt(
    isError: Boolean,
    onConfirm: (String) -> Unit,
    onBackClick: () -> Unit,
    onValueChange: () -> Unit
) {
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.admin_panel_password_input),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                onValueChange()
            },
            label = { Text(stringResource(R.string.admin_panel_enter_password)) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            isError = isError,
            supportingText = {
                if (isError) {
                    Text(text = stringResource(R.string.password_wrong), color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = onBackClick) {
                Text(stringResource(R.string.admin_panel_cancel))
            }
            Button(
                onClick = { onConfirm(password) },
                enabled = password.isNotBlank()
            ) {
                Text(stringResource(R.string.admin_panel_confirm))
            }
        }
    }
}
