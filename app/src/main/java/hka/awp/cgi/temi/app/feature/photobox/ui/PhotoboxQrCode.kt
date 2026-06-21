package hka.awp.cgi.temi.app.feature.photobox.ui

import android.graphics.Bitmap
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

private const val QR_CODE_SIZE_PX = 512
private const val BLACK = 0xFF000000.toInt()
private const val WHITE = 0xFFFFFFFF.toInt()

internal fun generateQrCodeBitmap(content: String, sizePx: Int = QR_CODE_SIZE_PX): Bitmap {
    val bitMatrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
    val bitmap = createBitmap(bitMatrix.width, bitMatrix.height, Bitmap.Config.RGB_565)
    for (x in 0 until bitMatrix.width) {
        for (y in 0 until bitMatrix.height) {
            bitmap[x, y] = if (bitMatrix[x, y]) BLACK else WHITE
        }
    }
    return bitmap
}
