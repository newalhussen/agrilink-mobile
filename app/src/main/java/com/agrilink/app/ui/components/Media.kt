package com.agrilink.app.ui.components

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.agrilink.app.BuildConfig
import com.agrilink.app.ui.theme.Caprasimo
import com.agrilink.app.ui.theme.Organic
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object UrlResolver {
    private val origin: String = BuildConfig.API_BASE_URL.substringBefore("/api/")

    /** The API returns paths like /api/v1/files/{id}; make them absolute against the configured server. */
    fun absolute(path: String): String = if (path.startsWith("http")) path else origin.trimEnd('/') + "/" + path.trimStart('/')
}

/** Network image (Coil with the app's authenticated OkHttp client) with a warm placeholder, as washed photography. */
@Composable
fun AgriImage(url: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(20.dp)) {
    Box(modifier.clip(shape).background(Organic.Neutral300), contentAlignment = Alignment.Center) {
        if (url != null) {
            AsyncImage(
                model = UrlResolver.absolute(url),
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.matchParentSize(),
            )
        }
    }
}

/** QR code for a handover token, drawn at crisp resolution. */
@Composable
fun QrCodeImage(content: String, size: Dp = 200.dp, modifier: Modifier = Modifier) {
    val bitmap = remember(content) { qrBitmap(content, 512) }
    Box(modifier.size(size).clip(RoundedCornerShape(20.dp)).background(androidx.compose.ui.graphics.Color.White).padding(10.dp)) {
        Image(bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.matchParentSize(), contentScale = ContentScale.Fit)
    }
}

internal fun qrBitmap(content: String, px: Int): Bitmap {
    val hints = mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 0)
    val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, px, px, hints)
    val pixels = IntArray(px * px) { i -> if (matrix.get(i % px, i / px)) AndroidColor.BLACK else AndroidColor.WHITE }
    return Bitmap.createBitmap(pixels, px, px, Bitmap.Config.ARGB_8888)
}

/** The six-digit handover code, large and spaced ("482 913"). Pass [secret] to hide it behind dots. */
@Composable
fun BigCode(code: String, modifier: Modifier = Modifier, hidden: Boolean = false) {
    val text = code.chunked(3).joinToString(" ").let { if (hidden) it.map { c -> if (c == ' ') ' ' else '•' }.joinToString("") else it }
    Column(modifier.clip(RoundedCornerShape(28.dp)).background(Organic.Surface).padding(horizontal = 24.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text, style = MaterialTheme.typography.displayMedium.copy(fontFamily = Caprasimo, fontSize = 44.sp, letterSpacing = 4.sp), textAlign = TextAlign.Center)
    }
}
