package hka.awp.cgi.temi.app.feature.photobox

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

private const val COLOR_SUCCESS = 0xFF4CAF50L
private val ColorSuccess = Color(COLOR_SUCCESS)
private const val QR_DIALOG_IMAGE_SIZE_DP = 240

/** Everything [PreviewOverlay] needs to render the finished photo, grouped to keep it to one param. */
internal data class PreviewPhotoState(
    val capturedBitmap: Bitmap?,
    val mode: PhotoboxMode,
    val overlayEnabled: Boolean,
    val uploadState: PhotoboxUploadState
)

@Composable
internal fun PreviewOverlay(
    photoState: PreviewPhotoState,
    onShowQrCode: () -> Unit,
    onTakeAnother: () -> Unit,
    onToDashboard: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (photoState.capturedBitmap != null) {
            Image(
                bitmap = photoState.capturedBitmap.asImageBitmap(),
                contentDescription = null,
                // A single photo roughly matches the screen's aspect ratio, so Crop fills the
                // screen without visible black bars. A strip is tall and narrow — cropping it
                // the same way would zoom in until only one of the three shots is visible, so
                // it needs Fit instead to keep the whole strip on screen.
                contentScale = if (photoState.mode == PhotoboxMode.STRIP) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // For a strip, Temi is already baked into each individual frame (see PhotoboxViewModel) —
        // showing it again here would add one oversized Temi floating next to the whole strip.
        if (photoState.overlayEnabled && photoState.mode == PhotoboxMode.STANDARD) {
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

            UploadStatusRow(uploadState = photoState.uploadState, onShowQrCode = onShowQrCode)
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
internal fun QrCodeDialog(photoUrl: String, onDismiss: () -> Unit) {
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
