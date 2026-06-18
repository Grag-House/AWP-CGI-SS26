@file:Suppress("TooManyFunctions")

package hka.awp.cgi.temi.app.feature.photobox

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Preview
import androidx.camera.core.ViewPort
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NoPhotography
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hka.awp.cgi.temi.app.R

private const val DURATION_SHORT_S = 3
private const val DURATION_MEDIUM_S = 5
private const val DURATION_LONG_S = 10
private const val COLOR_SUCCESS = 0xFF4CAF50L
private val ColorSuccess = Color(COLOR_SUCCESS)

@Composable
fun PhotoboxScreen(
    modifier: Modifier = Modifier,
    viewModel: PhotoboxViewModel,
    onNavigateToDashboard: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cameraState by viewModel.cameraState.collectAsStateWithLifecycle()
    val overlayEnabled by viewModel.overlayEnabled.collectAsStateWithLifecycle()
    val isFrontCamera by viewModel.isFrontCamera.collectAsStateWithLifecycle()

    DisposableEffect(viewModel) {
        onDispose { viewModel.onScreenStopped() }
    }

    Box(modifier = modifier.fillMaxSize()) {
        CameraSection(
            modifier = Modifier.fillMaxSize(),
            cameraState = cameraState,
            isFrontCamera = isFrontCamera,
            onBindCamera = viewModel::bindCamera
        )

        // The PREVIEW phase renders its own copy over the captured photo (see PreviewOverlay).
        if (overlayEnabled && uiState.phase != PhotoboxPhase.PREVIEW) {
            TemiOverlayImage()
        }

        when (uiState.phase) {
            PhotoboxPhase.IDLE -> IdleOverlay(
                uiState = uiState,
                onDurationSelect = viewModel::setDuration,
                onStart = viewModel::startSession
            )
            PhotoboxPhase.COUNTDOWN -> CountdownOverlay(
                uiState = uiState,
                onCancel = viewModel::reset
            )
            PhotoboxPhase.CAPTURE -> CaptureFlashOverlay(modifier = Modifier.fillMaxSize())
            PhotoboxPhase.PREVIEW -> PreviewOverlay(
                capturedBitmap = uiState.capturedBitmap,
                overlayEnabled = overlayEnabled,
                uploadState = uiState.uploadState,
                onShowQrCode = viewModel::showQrCode,
                onTakeAnother = viewModel::takeAnotherPhoto,
                onToDashboard = {
                    viewModel.reset()
                    onNavigateToDashboard()
                }
            )
        }

        val photoUrl = uiState.uploadedPhotoUrl
        if (uiState.showQrCode && photoUrl != null) {
            QrCodeDialog(photoUrl = photoUrl, onDismiss = viewModel::hideQrCode)
        }
    }
}

// ─── Camera ──────────────────────────────────────────────────────────────────

@Composable
private fun CameraSection(
    modifier: Modifier = Modifier,
    cameraState: PhotoboxCameraState,
    isFrontCamera: Boolean,
    onBindCamera: (LifecycleOwner, Preview.SurfaceProvider, ViewPort?) -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { hasCameraPermission = it }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    when {
        !hasCameraPermission -> CameraMessageOverlay(
            modifier = modifier,
            textRes = R.string.photobox_camera_permission_required
        )
        cameraState == PhotoboxCameraState.UNAVAILABLE -> CameraMessageOverlay(
            modifier = modifier,
            textRes = R.string.photobox_camera_unavailable
        )
        else -> CameraPreviewView(
            modifier = modifier,
            isFrontCamera = isFrontCamera,
            onBindCamera = onBindCamera
        )
    }
}

@Composable
private fun CameraMessageOverlay(modifier: Modifier = Modifier, textRes: Int) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Rounded.NoPhotography,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(textRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun CameraPreviewView(
    modifier: Modifier = Modifier,
    isFrontCamera: Boolean,
    onBindCamera: (LifecycleOwner, Preview.SurfaceProvider, ViewPort?) -> Unit
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                // SurfaceView (the PERFORMANCE default) ignores scaleX/scaleY since it's
                // composited as its own layer outside the normal View pipeline — switch to
                // TextureView so the mirroring below actually has an effect.
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                // Deferred to post() so the view is laid out and viewPort reflects its real size.
                post { onBindCamera(lifecycleOwner, surfaceProvider, viewPort) }
            }
        },
        // Mirror the live feed for the front camera — otherwise moving your head left makes the
        // preview look like you moved right, the opposite of how a mirror (or a selfie cam) works.
        update = { previewView -> previewView.scaleX = if (isFrontCamera) -1f else 1f },
        modifier = modifier
    )
}

// ─── IDLE overlay ────────────────────────────────────────────────────────────

@Composable
private fun IdleOverlay(
    uiState: PhotoboxUiState,
    onDurationSelect: (Int) -> Unit,
    onStart: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        BottomBar(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.photobox_countdown_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                Spacer(Modifier.weight(1f))
                listOf(DURATION_SHORT_S, DURATION_MEDIUM_S, DURATION_LONG_S).forEach { seconds ->
                    val isSelected = uiState.selectedDuration == seconds
                    Surface(
                        onClick = { onDurationSelect(seconds) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        modifier = Modifier.height(40.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.photobox_duration_seconds, seconds),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) {
                                    Color.White
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.photobox_start_button),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ─── COUNTDOWN overlay ───────────────────────────────────────────────────────

@Suppress("MagicNumber")
@Composable
private fun CountdownOverlay(
    uiState: PhotoboxUiState,
    onCancel: () -> Unit
) {
    val progress = uiState.countdownRemaining.toFloat() / uiState.selectedDuration.toFloat()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f)
            ) {
                Text(
                    text = stringResource(R.string.photobox_smile_label),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    modifier = Modifier.size(200.dp),
                    strokeWidth = 10.dp,
                    color = Color.White.copy(alpha = 0.25f),
                    trackColor = Color.Transparent
                )
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(200.dp),
                    strokeWidth = 10.dp,
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
                AnimatedContent(
                    targetState = uiState.countdownRemaining,
                    transitionSpec = {
                        (
                            scaleIn(initialScale = 1.5f, animationSpec = tween(280)) +
                                fadeIn(animationSpec = tween(180))
                            )
                            .togetherWith(
                                scaleOut(targetScale = 0.6f, animationSpec = tween(280)) +
                                    fadeOut(animationSpec = tween(180))
                            )
                    },
                    label = "countdown_number"
                ) { count ->
                    Text(
                        text = count.toString(),
                        fontSize = 80.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        TextButton(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        ) {
            Text(
                text = stringResource(R.string.photobox_cancel),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

// ─── CAPTURE flash ───────────────────────────────────────────────────────────

@Composable
private fun CaptureFlashOverlay(modifier: Modifier = Modifier) {
    val alpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        alpha.animateTo(targetValue = 0f, animationSpec = tween(durationMillis = 500))
    }
    Box(modifier = modifier.background(Color.White.copy(alpha = alpha.value)))
}

// ─── Temi overlay ────────────────────────────────────────────────────────────

/**
 * Renders the Temi cutout at a fixed screen position so it lines up identically whether
 * shown over the live camera feed or over the final captured photo.
 */
@Composable
private fun BoxScope.TemiOverlayImage() {
    Image(
        painter = painterResource(R.drawable.temi_photo),
        contentDescription = null,
        contentScale = ContentScale.FillHeight,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .fillMaxHeight(PHOTOBOX_OVERLAY_HEIGHT_FRACTION)
    )
}

// ─── PREVIEW overlay ─────────────────────────────────────────────────────────

@Suppress("LongParameterList")
@Composable
private fun PreviewOverlay(
    capturedBitmap: Bitmap?,
    overlayEnabled: Boolean,
    uploadState: PhotoboxUploadState,
    onShowQrCode: () -> Unit,
    onTakeAnother: () -> Unit,
    onToDashboard: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (capturedBitmap != null) {
            Image(
                bitmap = capturedBitmap.asImageBitmap(),
                contentDescription = null,
                // Matches the live preview's FILL_CENTER behavior: fill the screen, crop instead
                // of letterboxing, so no black bars appear if the aspect ratio doesn't match exactly.
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        if (overlayEnabled) {
            TemiOverlayImage()
        }

        BottomBar(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = ColorSuccess,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = stringResource(R.string.photobox_preview_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = onTakeAnother,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoCamera,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.photobox_new_photo_button),
                        fontWeight = FontWeight.Bold
                    )
                }
                OutlinedButton(
                    onClick = onToDashboard,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = stringResource(R.string.photobox_to_dashboard))
                }
            }

            Spacer(Modifier.height(4.dp))

            UploadStatusRow(uploadState = uploadState, onShowQrCode = onShowQrCode)
        }
    }
}

@Composable
private fun UploadStatusRow(
    uploadState: PhotoboxUploadState,
    onShowQrCode: () -> Unit
) {
    when (uploadState) {
        PhotoboxUploadState.UPLOADING -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            Text(
                text = stringResource(R.string.photobox_upload_in_progress),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        PhotoboxUploadState.SUCCESS -> OutlinedButton(
            onClick = onShowQrCode,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.QrCode2,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(text = stringResource(R.string.photobox_show_qr_button))
        }

        PhotoboxUploadState.FAILED -> Text(
            text = stringResource(R.string.photobox_upload_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )

        PhotoboxUploadState.NONE -> Unit
    }
}

@Composable
private fun QrCodeDialog(photoUrl: String, onDismiss: () -> Unit) {
    val qrBitmap = remember(photoUrl) { generateQrCodeBitmap(photoUrl) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.photobox_qr_dialog_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.photobox_qr_dialog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(240.dp)
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.close))
                }
            }
        }
    }
}

// ─── Shared ──────────────────────────────────────────────────────────────────

@Composable
private fun BottomBar(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.93f))
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
    }
}
