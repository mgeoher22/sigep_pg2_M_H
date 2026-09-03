package com.fincahernandez.gestionpecuaria.ui.screens.animals

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Presenta la misma fotografía tanto en el formulario como en el perfil.
 * Si la referencia ya no está disponible, muestra un indicador comprensible.
 */
@Composable
internal fun AnimalPhoto(
    photoUri: String,
    modifier: Modifier = Modifier,
    placeholderIcon: ImageVector = Icons.Default.Pets,
    placeholderText: String = "Sin fotografía registrada"
) {
    val context = LocalContext.current
    val preview by produceState<ImageBitmap?>(initialValue = null, key1 = photoUri) {
        value = if (photoUri.isBlank()) null else loadAnimalImagePreview(context, photoUri)
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (preview != null) {
            Image(
                bitmap = preview!!,
                contentDescription = "Fotografía del animal",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    placeholderIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(placeholderText, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/** Carga una miniatura reducida para evitar usar memoria innecesaria con fotos grandes. */
private suspend fun loadAnimalImagePreview(
    context: Context,
    uriValue: String
): ImageBitmap? = withContext(Dispatchers.IO) {
    runCatching {
        val uri = Uri.parse(uriValue)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }

        var sampleSize = 1
        while (
            bounds.outWidth / sampleSize > 1200 ||
            bounds.outHeight / sampleSize > 1200
        ) {
            sampleSize *= 2
        }

        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        // Algunos formatos no contienen metadatos EXIF. En ese caso se conserva
        // la orientación normal y la fotografía continúa mostrándose.
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
                ?.correctExifOrientation(orientation)
                ?.asImageBitmap()
        }
    }.getOrNull()
}

/**
 * Aplica la rotación o reflejo guardado por la cámara en los metadatos EXIF.
 * BitmapFactory ignora esa información y por eso algunas fotos aparecían de lado.
 */
private fun Bitmap.correctExifOrientation(orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
            matrix.setRotate(180f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(-90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        else -> return this
    }

    val corrected = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    if (corrected !== this) recycle()
    return corrected
}
