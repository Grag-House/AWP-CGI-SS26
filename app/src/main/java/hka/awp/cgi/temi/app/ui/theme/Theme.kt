package hka.awp.cgi.temi.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class CustomDesignTokens(
    val sidepanel: Color,
    val sidepanelHighlight: Color
                             )

val LocalCustomColors =
    staticCompositionLocalOf {
        CustomDesignTokens(
            sidepanel = Color.Unspecified,
            sidepanelHighlight = Color.Unspecified
                          )
    }

@Composable
fun CgiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
            ) {
    val colorScheme =
        if (darkTheme) {
            darkColorScheme(
                primary = primary,
                onPrimary = Color.White,
                background = DarkBackground,
                surface = DarkSurface,
                onSurface = Color.White,
                surfaceVariant = DarkSurfaceVariant
                           )
        } else {
            lightColorScheme(
                primary = primary,
                onPrimary = OnPrimary,
                background = AppBackground,
                surface = SidepanelColor,
                onSurface = OnSurface,
                surfaceVariant = LightSurfaceVariant
                            )
        }

    val customColors =
        if (darkTheme) {
            CustomDesignTokens(
                sidepanel = DarkSidepanel,
                sidepanelHighlight = DarkSidepanelHighlight
                              )
        } else {
            CustomDesignTokens(
                sidepanel = SidepanelColor,
                sidepanelHighlight = SidepanelHighlight
                              )
        }

    CompositionLocalProvider(LocalCustomColors provides customColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
                     )
    }
}

object AppTheme {
    val customColors: CustomDesignTokens
        @Composable
        get() = LocalCustomColors.current
}
