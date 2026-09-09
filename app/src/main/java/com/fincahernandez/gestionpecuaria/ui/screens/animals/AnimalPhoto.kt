package com.fincahernandez.gestionpecuaria.ui.screens.animals

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.LruCache
import androidx.annotation.RequiresApi
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    placeholderText: String = "Sin fotografía registrada",
    maxDecodeDimensionPx: Int = 720
) {
    val context = LocalContext.current
    val cacheKey = remember(photoUri, maxDecodeDimensionPx) {
        AnimalPhotoMemoryCache.key(photoUri, maxDecodeDimensionPx)
    }
    val bitmap by produceState<Bitmap?>(
        initialValue = AnimalPhotoMemoryCache.get(cacheKey),
        key1 = cacheKey
    ) {
        value = if (photoUri.isBlank()) {
            null
        } else {
            loadAnimalImagePreview(
                context = context.applicationContext,
                uriValue = photoUri,
                maxDimensionPx = maxDecodeDimensionPx
            )
        }
    }
    val preview = remember(bitmap) { bitmap?.asImageBitmap() }

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

/**
 * Conserva miniaturas ya procesadas mientras la aplicación está abierta. La clave incluye
 * el tamaño solicitado para que una tarjeta pequeña no obligue al perfil a usar una imagen
 * borrosa y para que volver a una pantalla no decodifique otra vez el mismo archivo.
 */
private object AnimalPhotoMemoryCache {
    private val cacheSizeKb = (Runtime.getRuntime().maxMemory() / 1024L / 16L)
        .coerceIn(8L * 1024L, 24L * 1024L)
        .toInt()

    private val bitmaps = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.allocationByteCount / 1024).coerceAtLeast(1)
    }

    fun key(uriValue: String, maxDimensionPx: Int): String = "$uriValue#$maxDimensionPx"

    fun get(key: String): Bitmap? = bitmaps.get(key)

    fun put(key: String, bitmap: Bitmap) {
        bitmaps.put(key, bitmap)
    }
}

/** Carga en segundo plano una miniatura del tamaño que realmente necesita la vista. */
private suspend fun loadAnimalImagePreview(
    context: Context,
    uriValue: String,
    maxDimensionPx: Int
): Bitmap? = withContext(Dispatchers.IO) {
    val safeMaxDimension = maxDimensionPx.coerceAtLeast(128)
    val cacheKey = AnimalPhotoMemoryCache.key(uriValue, maxDimensionPx)
    AnimalPhotoMemoryCache.get(cacheKey)?.let { return@withContext it }

    runCatching {
        val uri = Uri.parse(uriValue)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(context, uri, safeMaxDimension)
        } else {
            decodeLegacyBitmap(context, uri, safeMaxDimension)
        }
    }.getOrNull()?.also { bitmap ->
        AnimalPhotoMemoryCache.put(cacheKey, bitmap)
    }
}

/**
 * Desde Android 9, ImageDecoder lee tamaño, orientación EXIF y píxeles en una sola carga.
 * Esto evita abrir tres veces la misma fotografía, que era el principal costo al navegar.
 */
@RequiresApi(Build.VERSION_CODES.P)
private fun decodeWithImageDecoder(
    context: Context,
    uri: Uri,
    maxDimensionPx: Int
): Bitmap {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val sourceWidth = info.size.width
        val sourceHeight = info.size.height
        val largestSide = maxOf(sourceWidth, sourceHeight)
        if (largestSide > maxDimensionPx) {
            val scale = maxDimensionPx.toFloat() / largestSide.toFloat()
            decoder.setTargetSize(
                (sourceWidth * scale).toInt().coerceAtLeast(1),
                (sourceHeight * scale).toInt().coerceAtLeast(1)
            )
        }
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
}

/** Compatibilidad para Android 7 y 8, donde ImageDecoder todavía no existe. */
private fun decodeLegacyBitmap(
    context: Context,
    uri: Uri,
    maxDimensionPx: Int
): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, bounds)
    }

    var sampleSize = 1
    while (
        bounds.outWidth / sampleSize > maxDimensionPx ||
        bounds.outHeight / sampleSize > maxDimensionPx
    ) {
        sampleSize *= 2
    }

    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val orientation = runCatching {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
        }
    }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

    return context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, options)
            ?.correctExifOrientation(orientation)
    }
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
