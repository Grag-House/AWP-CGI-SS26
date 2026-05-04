package hka.awp.temi_cgi_app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

// 1. Eigene Struktur für Farben, die nicht im Standard-Material-Satz sind
@Immutable
data class CustomDesignTokens(
    val sidepanel: Color,
    val sidepanelHighlight: Color
)

val LocalCustomColors = staticCompositionLocalOf {
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
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = CgiRed,
            onPrimary = Color.White,
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            onSurface = Color.White,
            surfaceVariant = Color(0xFF2C2C2C)
        )
    } else {
        lightColorScheme(
            primary = CgiRed,
            onPrimary = OnPrimary,
            background = AppBackground,
            surface = SidepanelColor,
            onSurface = OnSurface,
            surfaceVariant = Color(0xFFF5F5F5)
        )
    }

    val customColors = if (darkTheme) {
        CustomDesignTokens(
            sidepanel = Color(0xFF1A1A1A),
            sidepanelHighlight = Color(0xFF333333)
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
// Hilfsobjekt für den einfachen Zugriff im Code
object AppTheme {
    val customColors: CustomDesignTokens
        @Composable
        get() = LocalCustomColors.current
}