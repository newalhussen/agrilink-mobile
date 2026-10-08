package com.agrilink.app.ui.common

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import com.agrilink.app.R
import java.io.File

/**
 * Returns a function that asks "Camera or gallery?" and delivers the chosen photo as a Uri.
 * Camera photos are written to a cache file through FileProvider (no CAMERA permission needed).
 */
@Composable
fun rememberPhotoPicker(onPicked: (Uri) -> Unit): () -> Unit {
    val context = LocalContext.current
    var asking by remember { mutableStateOf(false) }
    var pendingCapture by remember { mutableStateOf<Uri?>(null) }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onPicked) }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = pendingCapture
        pendingCapture = null
        if (ok && uri != null) onPicked(uri)
    }

    if (asking) {
        AlertDialog(
            onDismissRequest = { asking = false },
            title = { Text(stringResource(R.string.photo_source_title)) },
            confirmButton = {
                TextButton(onClick = {
                    asking = false
                    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
                    val file = File.createTempFile("photo_", ".jpg", dir)
                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
                    pendingCapture = uri
                    camera.launch(uri)
                }) { Text(stringResource(R.string.photo_camera)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    asking = false
                    gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(stringResource(R.string.photo_gallery)) }
            },
        )
    }
    return { asking = true }
}
