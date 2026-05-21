package hka.awp.cgi.temi.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import hka.awp.cgi.temi.app.R

/**
 * A composable that displays a stacked icon representing a mode switch,
 * combining both "on" and "off" toggle visual elements.
 *
 * @param modifier The [Modifier] to be applied to the layout.
 * @param tint The [Color] used to tint the icons. If null, [LocalContentColor] is used.
 */
@Composable
fun ModeIcon(modifier: Modifier = Modifier, tint: Color? = null) {
    val description = stringResource(R.string.switch_mode_description)
    Box(
        contentAlignment = Alignment.Center,
        modifier =
            modifier.semantics {
                contentDescription = description
            }
       ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_toggle_on),
            contentDescription = null,
            modifier =
                Modifier
                    .size(24.dp)
                    .offset(y = (-7).dp),
            tint = tint ?: LocalContentColor.current
            )
        Icon(
            painter = painterResource(id = R.drawable.ic_toggle_off),
            contentDescription = null,
            modifier =
                Modifier
                    .size(24.dp)
                    .offset(y = 7.dp),
            tint = tint ?: LocalContentColor.current
            )
    }
}
