package com.bookmyturf.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.bookmyturf.app.R
import com.bookmyturf.app.data.Turf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

private fun decode(context: Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
    val bitmap = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null
    val orientation = runCatching { context.contentResolver.openInputStream(uri)?.use { android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1) } }.getOrNull()
    val matrix = android.graphics.Matrix().apply {
        when (orientation) {
            2 -> setScale(-1f, 1f)
            3 -> setRotate(180f)
            4 -> setScale(1f, -1f)
            5 -> { setRotate(90f); postScale(-1f, 1f) }
            6 -> setRotate(90f)
            7 -> { setRotate(-90f); postScale(-1f, 1f) }
            8 -> setRotate(-90f)
        }
    }
    if (matrix.isIdentity) return bitmap
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also { if (it !== bitmap) bitmap.recycle() }
}

/** Copies into app storage so photos survive picker permission changes and restarts. */
suspend fun importVenuePhoto(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val bitmap = decode(context, uri) ?: error("This photo could not be opened. Choose a JPG or PNG.")
    val directory = File(context.filesDir, "venue-photos").apply { mkdirs() }
    val file = File(directory, "${UUID.randomUUID()}.jpg")
    try { file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) } }
    finally { bitmap.recycle() }
    Uri.fromFile(file).toString()
}

@Composable
fun VenuePhoto(turf: Turf, modifier: Modifier = Modifier, page: Int = 0) {
    val context = LocalContext.current
    val uri = turf.photoUris.getOrNull(page) ?: turf.photoUris.firstOrNull()
    val bitmap by produceState<Bitmap?>(null, uri) {
        value = null
        if (uri != null) value = withContext(Dispatchers.IO) {
            runCatching {
                if (uri.startsWith("gs://")) {
                    val bytes = com.google.firebase.storage.FirebaseStorage.getInstance().getReferenceFromUrl(uri).getBytes(5L * 1024 * 1024).await()
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } else decode(context, Uri.parse(uri))
            }.getOrNull()
        }
    }
    val fallback = turf.photoResIds.getOrNull(page) ?: when (turf.visualSeed.mod(6)) {
        1, 4 -> R.drawable.venue_cricket
        5 -> R.drawable.venue_badminton
        else -> R.drawable.venue_football
    }
    if (bitmap != null) Image(bitmap!!.asImageBitmap(), "${turf.name}, photo ${page + 1}", modifier, contentScale = ContentScale.Crop)
    else Image(painterResource(fallback), "${turf.name}, photo ${page + 1}", modifier, contentScale = ContentScale.Crop)
}
