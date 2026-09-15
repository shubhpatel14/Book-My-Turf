package com.bookmyturf.app.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.bookmyturf.app.R
import com.bookmyturf.app.data.Sport
import com.bookmyturf.app.data.Turf
import com.bookmyturf.app.ui.theme.PulseLavender
import com.bookmyturf.app.ui.theme.Steel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

/**
 * Curated public high-resolution sports turf image URLs.
 * Free from Unsplash, fast CDN, zero billing required.
 */
object TurfImagePresets {
    const val FOOTBALL = "https://images.unsplash.com/photo-1575361204480-aadea25e6e68?w=900&q=80"
    const val CRICKET = "https://images.unsplash.com/photo-1540747913346-19e32dc3e97e?w=900&q=80"
    const val BADMINTON = "https://images.unsplash.com/photo-1626224583764-f87db24ac4ea?w=900&q=80"
    const val NIGHT_STADIUM = "https://images.unsplash.com/photo-1508098682722-e99c43a406b2?w=900&q=80"

    val items = listOf(
        "Football Pitch" to FOOTBALL,
        "Box Cricket" to CRICKET,
        "Badminton Court" to BADMINTON,
        "Night Floodlight" to NIGHT_STADIUM
    )
}

private fun decodeStreamWithResize(context: Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) sample *= 2
    val bitmap = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
    } ?: return null
    val orientation = runCatching {
        context.contentResolver.openInputStream(uri)?.use {
            android.media.ExifInterface(it).getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)
        }
    }.getOrNull()
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
    val rotated = if (matrix.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    val maxDim = 960
    if (rotated.width > maxDim || rotated.height > maxDim) {
        val ratio = maxDim.toFloat() / maxOf(rotated.width, rotated.height)
        val targetWidth = (rotated.width * ratio).toInt().coerceAtLeast(1)
        val targetHeight = (rotated.height * ratio).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(rotated, targetWidth, targetHeight, true)
        if (scaled !== rotated && rotated !== bitmap) rotated.recycle()
        if (rotated !== bitmap) bitmap.recycle()
        return scaled
    }
    return rotated
}

/**
 * When admin selects a photo from the device gallery, this downsamples it,
 * compresses it to 75% quality JPEG, and converts it into a Base64 data URI
 * so it can be stored directly in Firestore without requiring Firebase Cloud Storage.
 */
suspend fun importVenuePhoto(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
    val bitmap = decodeStreamWithResize(context, uri) ?: error("This photo could not be opened. Choose a JPG or PNG.")
    val baos = ByteArrayOutputStream()
    try {
        check(bitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)) { "Failed to compress image." }
        val base64 = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.NO_WRAP)
        // Also save to app internal storage for local cache
        val directory = File(context.filesDir, "venue-photos").apply { mkdirs() }
        val file = File(directory, "${UUID.randomUUID()}.jpg")
        file.writeBytes(baos.toByteArray())
        "data:image/jpeg;base64,$base64"
    } finally {
        bitmap.recycle()
    }
}

/**
 * Displays a turf image using Coil with memory and disk caching,
 * crossfade animations, loading placeholders, and automatic fallback
 * to sport-specific vector/drawable graphics if the URL is invalid or offline.
 */
@Composable
fun VenuePhoto(turf: Turf, modifier: Modifier = Modifier, page: Int = 0) {
    val context = LocalContext.current
    val effectiveUrls = turf.photoUris.ifEmpty { turf.imageUrls }
    val uri = effectiveUrls.getOrNull(page) ?: if (page == 0) effectiveUrls.firstOrNull() else null

    val fallback = turf.photoResIds.getOrNull(page) ?: when {
        Sport.BADMINTON in turf.sports -> R.drawable.venue_badminton
        Sport.CRICKET in turf.sports && Sport.FOOTBALL !in turf.sports -> R.drawable.venue_cricket
        turf.visualSeed % 6 == 1 || turf.visualSeed % 6 == 4 -> R.drawable.venue_cricket
        turf.visualSeed % 6 == 5 -> R.drawable.venue_badminton
        else -> R.drawable.venue_football
    }

    if (uri.isNullOrBlank()) {
        Image(
            painter = painterResource(fallback),
            contentDescription = "${turf.name}, photo ${page + 1}",
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else {
        val request = remember(uri) {
            ImageRequest.Builder(context)
                .data(uri)
                .crossfade(300)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }

        SubcomposeAsyncImage(
            model = request,
            contentDescription = "${turf.name}, photo ${page + 1}",
            modifier = modifier,
            contentScale = ContentScale.Crop,
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Steel),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(0.25f),
                        color = PulseLavender.copy(alpha = 0.6f),
                        strokeWidth = 2.dp
                    )
                }
            },
            error = {
                Image(
                    painter = painterResource(fallback),
                    contentDescription = "${turf.name}, fallback ${page + 1}",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        )
    }
}
