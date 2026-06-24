package hka.awp.cgi.temi.app.feature.webserver

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Entry point composable that gates [WebViewScreen] behind a password prompt.
 *
 * Drop-in replacement for direct calls to [WebViewScreen]: just pass the same
 * [url] you would have passed before.
 *
 * The [PasswordGateViewModel] is scoped to the caller's NavBackStackEntry
 * (or the Activity if no nav graph is used) so the 10-minute session timer
 * survives recomposition but resets correctly across navigation.
 */
@Composable
fun PasswordGatedWebView(
    url: String,
    viewModel: PasswordGateViewModel,
                        ) {
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()

    // Lifecycle observer — starts/cancels the lock timer when the screen hides/shows
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.onScreenVisible()
                Lifecycle.Event.ON_PAUSE -> viewModel.onScreenHidden()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (isAuthenticated) {
        WebViewScreen(url = url)
    } else {
        PasswordScreen(
            onSubmit = { viewModel.submitPassword(it) },
            errorMessage = viewModel.errorMessage.collectAsStateWithLifecycle().value,
            onErrorShown = { viewModel.clearError() },
                      )
    }
}

// ─── Shared card shell (mirrors WeatherCard) ─────────────────────────────────

@Composable
private fun PasswordCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
                        ) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
                                        ),
        ) {
        Column(modifier = Modifier.padding(24.dp), content = content)
    }
}

// ─── Top bar (mirrors WetterTopBar) ──────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PasswordTopBar() {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                    )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Zugang",
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
                                                  ),
             )
}

// ─── Password screen ─────────────────────────────────────────────────────────

@Composable
private fun PasswordScreen(
    onSubmit: (String) -> Unit,
    errorMessage: String?,
    onErrorShown: () -> Unit,
                          ) {
    var input by rememberSaveable { mutableStateOf("") }
    val hasError = errorMessage != null

    // Outer layout mirrors WeatherContent: background + TopBar + scrollable column
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
       ) {
        Column(modifier = Modifier.weight(1f)) {
            PasswordTopBar()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                  ) {
                // ── Header card ──────────────────────────────────────────────
                PasswordCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                       ) {
                        Column {
                            Text(
                                text = "Zugang gesperrt",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Bitte Passwort eingeben,\num fortzufahren",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp,
                                )
                        }
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(80.dp),
                            )
                    }
                }

                // ── Input card ───────────────────────────────────────────────
                PasswordCard {
                    Text(
                        text = "Passwort",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            if (hasError) onErrorShown()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Passwort eingeben",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                                                         ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                onSubmit(input)
                                input = ""
                            }
                                                         ),
                        singleLine = true,
                        isError = hasError,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            errorTextColor = MaterialTheme.colorScheme.onSurface,
                                                                 ),
                        shape = RoundedCornerShape(10.dp),
                                     )

                    // Inline error message
                    AnimatedVisibility(
                        visible = hasError,
                        enter = fadeIn(),
                        exit = fadeOut(),
                                      ) {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            )
                    }

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            onSubmit(input)
                            input = ""
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                                            ),
                          ) {
                        Text(
                            text = "Entsperren",
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp,
                            )
                    }
                }
            }
        }
    }
}
