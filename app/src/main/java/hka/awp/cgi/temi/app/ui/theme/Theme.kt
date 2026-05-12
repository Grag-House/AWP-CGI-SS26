package hka.awp.cgi.temi.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// 1. Eigene Struktur für Farben, die nicht im Standard-Material-Satz sind
@Immutable
data class CustomDesignTokens(val sidepanel: Color, val sidepanelHighlight: Color)

val LocalCustomColors =
    staticCompositionLocalOf {
        CustomDesignTokens(
            sidepanel = Color.Unspecified,
            sidepanelHighlight = Color.Unspecified
        )
    }

@Composable
fun CgiTheme(content: @Composable () -> Unit) {
    // 2. Zuordnung deiner Farben zum Material-Farbschema
    val colorScheme =
        lightColorScheme(
            primary = CgiRed,
            onPrimary = OnPrimary,
            background = AppBackground,
            surface = SidepanelColor, // Sidepanel als Standard-Oberfläche
            onSurface = OnSurface
        )

    val customColors =
        CustomDesignTokens(
            sidepanel = SidepanelColor,
            sidepanelHighlight = SidepanelHighlight
        )

    // 3. Bereitstellung über den MaterialTheme-Wrapper
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
