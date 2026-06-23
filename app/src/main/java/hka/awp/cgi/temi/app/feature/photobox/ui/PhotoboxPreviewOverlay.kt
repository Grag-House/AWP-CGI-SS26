package hka.awp.cgi.temi.app.feature.photobox.ui

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import hka.awp.cgi.temi.app.R
import hka.awp.cgi.temi.app.feature.photobox.BottomBar
import hka.awp.cgi.temi.app.feature.photobox.PhotoboxMode
import hka.awp.cgi.temi.app.feature.photobox.PhotoboxUploadState
import hka.awp.cgi.temi.app.feature.photobox.TemiOverlayImage
import hka.awp.cgi.temi.app.feature.photobox.TemiOverlayPosition
import hka.awp.cgi.temi.app.feature.photobox.filter.PhotoboxPhotoFilter
import hka.awp.cgi.temi.app.feature.photobox.filter.toComposeColorFilter

private const val COLOR_SUCCESS = 0xFF4CAF50L
private val ColorSuccess = Color(COLOR_SUCCESS)
private const val QR_DIALOG_IMAGE_SIZE_DP = 240

/** Everything [PreviewOverlay] needs to render the finished photo, grouped to keep it to one param. */
internal data class PreviewPhotoState(
    val capturedBitmap: Bitmap?,
    val mode: PhotoboxMode,
    val overlayEnabled: Boolean,
    val overlayPosition: TemiOverlayPosition,
    val uploadState: PhotoboxUploadState,
    val selectedFilter: PhotoboxPhotoFilter
)

/** Everything [PreviewOverlay] needs to report user actions, grouped to keep it to one param. */
internal data class PreviewOverlayCallbacks(
    val onSelectFilter: (PhotoboxPhotoFilter) -> Unit,
    val onConfirmUpload: () -> Unit,
    val onShowQrCode: () -> Unit,
    val onTakeAnother: () -> Unit,
    val onToDashboard: () -> Unit
)

@Composable
internal fun PreviewOverlay(
    photoState: PreviewPhotoState,
    callbacks: PreviewOverlayCallbacks
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (photoState.capturedBitmap != null) {
            Image(
                bitmap = photoState.capturedBitmap.asImageBitmap(),
                contentDescription = null,
                // A single photo roughly matches the screen's aspect ratio, so Crop fills the
                // screen without visible black bars. A strip/grid composite has a different
                // aspect ratio — cropping it the same way would zoom in until only part of it is
                // visible, so it needs Fit instead to keep the whole composite on screen.
                contentScale = if (photoState.mode == PhotoboxMode.STANDARD) ContentScale.Crop else ContentScale.Fit,
                // Filter is only baked into the actual file on upload (see PhotoboxViewModel) —
                // here it's just a cheap GPU-composited preview so switching filters is instant.
                colorFilter = photoState.selectedFilter.toComposeColorFilter(),
                modifier = Modifier.fillMaxSize()
            )
        }

        // For a strip/grid, Temi is already baked into each individual frame (see
        // PhotoboxSessionFinalizer) — showing it again here would add one oversized Temi
        // floating next to the whole composite.
        if (photoState.overlayEnabled && photoState.mode == PhotoboxMode.STANDARD) {
            TemiOverlayImage(photoState.overlayPosition)
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
                    onClick = callbacks.onTakeAnother,
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
                    onClick = callbacks.onToDashboard,
                    modifier = Modifier.weight(1f).height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(text = stringResource(R.string.photobox_to_dashboard))
                }
            }

            Spacer(Modifier.height(4.dp))

            UploadStatusRow(
                uploadState = photoState.uploadState,
                selectedFilter = photoState.selectedFilter,
                onSelectFilter = callbacks.onSelectFilter,
                onConfirmUpload = callbacks.onConfirmUpload,
                onShowQrCode = callbacks.onShowQrCode
            )
        }
    }
}

@Composable
private fun UploadStatusRow(
    uploadState: PhotoboxUploadState,
    selectedFilter: PhotoboxPhotoFilter,
    onSelectFilter: (PhotoboxPhotoFilter) -> Unit,
    onConfirmUpload: () -> Unit,
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

        PhotoboxUploadState.QUEUED -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            Text(
                text = stringResource(R.string.photobox_upload_queued),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        // Upload hasn't started yet — let the user pick a filter and confirm before anything
        // is sent out (see PhotoboxViewModel.confirmUpload).
        PhotoboxUploadState.NONE -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterPicker(selectedFilter = selectedFilter, onSelectFilter = onSelectFilter)
            Button(
                onClick = onConfirmUpload,
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = stringResource(R.string.photobox_generate_qr_button))
            }
        }
    }
}

@Composable
private fun FilterPicker(
    selectedFilter: PhotoboxPhotoFilter,
    onSelectFilter: (PhotoboxPhotoFilter) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PhotoboxPhotoFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onSelectFilter(filter) },
                label = { Text(text = stringResource(filter.labelRes)) }
            )
        }
    }
}

private val PhotoboxPhotoFilter.labelRes: Int
    get() = when (this) {
        PhotoboxPhotoFilter.NONE -> R.string.photobox_filter_none
        PhotoboxPhotoFilter.GRAYSCALE -> R.string.photobox_filter_grayscale
        PhotoboxPhotoFilter.SEPIA -> R.string.photobox_filter_sepia
        PhotoboxPhotoFilter.VINTAGE -> R.string.photobox_filter_vintage
    }

@Composable
internal fun QrCodeDialog(photoUrl: String, expiresAtMillis: Long?, onDismiss: () -> Unit) {
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
                if (expiresAtMillis != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.photobox_qr_dialog_expiry),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(Modifier.height(16.dp))
                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(QR_DIALOG_IMAGE_SIZE_DP.dp)
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(R.string.close))
                }
            }
        }
    }
}
