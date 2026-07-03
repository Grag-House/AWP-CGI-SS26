package hka.awp.cgi.temi.app.feature.photobox.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.feature.photobox.PhotoboxUiState

private const val COUNTDOWN_RING_SIZE_DP = 200
private const val COUNTDOWN_RING_STROKE_DP = 10
private const val COUNTDOWN_RING_TRACK_ALPHA = 0.25f
private const val COUNTDOWN_LABEL_BACKGROUND_ALPHA = 0.75f
private const val COUNTDOWN_NUMBER_FONT_SP = 80
private const val COUNTDOWN_CANCEL_BOTTOM_PADDING_DP = 24
private const val COUNTDOWN_SCALE_IN_FACTOR = 1.5f
private const val COUNTDOWN_SCALE_OUT_FACTOR = 0.6f
private const val COUNTDOWN_SCALE_ANIMATION_MS = 280
private const val COUNTDOWN_FADE_ANIMATION_MS = 180
private const val CAPTURE_FLASH_FADE_MS = 500

@Composable
internal fun CountdownOverlay(
    uiState: PhotoboxUiState,
    onCancel: () -> Unit
) {
    val totalDuration = if (uiState.isBetweenShots) uiState.stripDelaySeconds else uiState.selectedDuration
    val progress = uiState.countdownRemaining.toFloat() / totalDuration.toFloat()
    val label = if (uiState.isBetweenShots) {
        stringResource(R.string.photobox_change_pose_label)
    } else {
        stringResource(R.string.photobox_smile_label)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (uiState.totalShots > 1) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = COUNTDOWN_LABEL_BACKGROUND_ALPHA)
                ) {
                    Text(
                        text = stringResource(
                            R.string.photobox_shot_progress,
                            minOf(uiState.shotsTaken + 1, uiState.totalShots),
                            uiState.totalShots
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = COUNTDOWN_LABEL_BACKGROUND_ALPHA)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            CountdownProgressRing(progress = progress, countdownRemaining = uiState.countdownRemaining)
        }
        TextButton(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = COUNTDOWN_CANCEL_BOTTOM_PADDING_DP.dp)
        ) {
            Text(
                text = stringResource(R.string.photobox_cancel),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun CountdownProgressRing(progress: Float, countdownRemaining: Int) {
    Box(contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.size(COUNTDOWN_RING_SIZE_DP.dp),
            strokeWidth = COUNTDOWN_RING_STROKE_DP.dp,
            color = Color.White.copy(alpha = COUNTDOWN_RING_TRACK_ALPHA),
            trackColor = Color.Transparent
        )
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(COUNTDOWN_RING_SIZE_DP.dp),
            strokeWidth = COUNTDOWN_RING_STROKE_DP.dp,
            color = MaterialTheme.colorScheme.primary,
            trackColor = Color.Transparent
        )
        AnimatedContent(
            targetState = countdownRemaining,
            transitionSpec = {
                (
                    scaleIn(
                        initialScale = COUNTDOWN_SCALE_IN_FACTOR,
                        animationSpec = tween(COUNTDOWN_SCALE_ANIMATION_MS)
                    ) +
                        fadeIn(animationSpec = tween(COUNTDOWN_FADE_ANIMATION_MS))
                    )
                    .togetherWith(
                        scaleOut(
                            targetScale = COUNTDOWN_SCALE_OUT_FACTOR,
                            animationSpec = tween(COUNTDOWN_SCALE_ANIMATION_MS)
                        ) +
                            fadeOut(animationSpec = tween(COUNTDOWN_FADE_ANIMATION_MS))
                    )
            },
            label = "countdown_number"
        ) { count ->
            Text(
                text = count.toString(),
                fontSize = COUNTDOWN_NUMBER_FONT_SP.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
internal fun CaptureFlashOverlay(modifier: Modifier = Modifier) {
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = CAPTURE_FLASH_FADE_MS))
    }
    Box(modifier = modifier.background(Color.White.copy(alpha = alpha.value)))
}
