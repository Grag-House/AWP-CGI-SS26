package hka.awp.cgi.temi.app.utils

import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

/**
 * Hides the system status bar (top bar) for the given [Window].
 *
 * This function configures the window to lay out its content under the system bars, sets the
 * behavior to reveal the bars temporarily upon swiping, and explicitly hides the status bars.
 *
 * @param window The [Window] instance in which the top bar should be hidden.
 */
fun hideTopBar(window: Window) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    windowInsetsController.systemBarsBehavior = BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
}
