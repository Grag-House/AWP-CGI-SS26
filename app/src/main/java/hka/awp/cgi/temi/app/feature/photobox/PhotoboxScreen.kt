package hka.awp.cgi.temi.app.feature.photobox

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Preview
import androidx.camera.core.ViewPort
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NoPhotography
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.feature.photobox.capture.PhotoboxCameraState
import hka.awp.cgi.temi.app.feature.photobox.ui.CaptureFlashOverlay
import hka.awp.cgi.temi.app.feature.photobox.ui.CountdownOverlay
import hka.awp.cgi.temi.app.feature.photobox.ui.IdleOverlay
import hka.awp.cgi.temi.app.feature.photobox.ui.ModeSelectOverlay
import hka.awp.cgi.temi.app.feature.photobox.ui.PreviewOverlay
import hka.awp.cgi.temi.app.feature.photobox.ui.PreviewOverlayCallbacks
import hka.awp.cgi.temi.app.feature.photobox.ui.PreviewPhotoState
import hka.awp.cgi.temi.app.feature.photobox.ui.QrCodeDialog
import hka.awp.cgi.temi.app.feature.photobox.upload.PHOTOBOX_OVERLAY_HEIGHT_FRACTION

private val OVERLAY_HIDDEN_PHASES = setOf(PhotoboxPhase.MODE_SELECT, PhotoboxPhase.PREVIEW)

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
        // MODE_SELECT fully covers the camera feed with its own opaque background.
        if (overlayEnabled && uiState.phase !in OVERLAY_HIDDEN_PHASES) {
            TemiOverlayImage()
        }

        when (uiState.phase) {
            PhotoboxPhase.MODE_SELECT -> ModeSelectOverlay(
                selectedMode = uiState.mode,
                onModeSelect = viewModel::selectMode
            )
            PhotoboxPhase.IDLE -> IdleOverlay(
                uiState = uiState,
                onDurationSelect = viewModel::setDuration,
                onStripDelaySelect = viewModel::setStripDelay,
                onBackToModeSelect = viewModel::backToModeSelect,
                onStart = viewModel::startSession
            )
            PhotoboxPhase.COUNTDOWN -> CountdownOverlay(
                uiState = uiState,
                onCancel = viewModel::reset
            )
            PhotoboxPhase.CAPTURE -> CaptureFlashOverlay(modifier = Modifier.fillMaxSize())
            PhotoboxPhase.PREVIEW -> PreviewOverlay(
                photoState = PreviewPhotoState(
                    capturedBitmap = uiState.capturedBitmap,
                    mode = uiState.mode,
                    overlayEnabled = overlayEnabled,
                    uploadState = uiState.uploadState,
                    selectedFilter = uiState.selectedFilter
                ),
                callbacks = PreviewOverlayCallbacks(
                    onSelectFilter = viewModel.pendingUploadController::selectFilter,
                    onConfirmUpload = viewModel.pendingUploadController::confirmUpload,
                    onShowQrCode = { viewModel.setQrCodeVisible(true) },
                    onTakeAnother = viewModel::reset,
                    onToDashboard = {
                        viewModel.reset()
                        onNavigateToDashboard()
                    }
                )
            )
        }

        val photoUrl = uiState.uploadedPhotoUrl
        if (uiState.showQrCode && photoUrl != null) {
            QrCodeDialog(
                photoUrl = photoUrl,
                expiresAtMillis = uiState.uploadedPhotoExpiresAt,
                onDismiss = { viewModel.setQrCodeVisible(false) }
            )
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

// ─── Shared ──────────────────────────────────────────────────────────────────

/**
 * Renders the Temi cutout at a fixed screen position so it lines up identically whether
 * shown over the live camera feed or over the final captured photo.
 */
@Composable
internal fun BoxScope.TemiOverlayImage() {
    Image(
        painter = painterResource(R.drawable.temi_photo),
        contentDescription = null,
        contentScale = ContentScale.FillHeight,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .fillMaxHeight(PHOTOBOX_OVERLAY_HEIGHT_FRACTION)
    )
}

@Composable
internal fun BottomBar(
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
