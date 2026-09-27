package org.hahn.maakmai.images

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** Longest side, in pixels, of a stored image. Plenty for a phone-width card. */
const val MAX_IMAGE_DIMENSION = 1600
private const val WEBP_QUALITY = 80

class EncodedImage(val bytes: ByteArray, val width: Int, val height: Int)

/** The size that fits [width] x [height] within [maxDimension], keeping the aspect ratio. Never upscales. */
fun scaledSize(width: Int, height: Int, maxDimension: Int = MAX_IMAGE_DIMENSION): Pair<Int, Int> {
    val longest = max(width, height)
    if (longest <= maxDimension) return width to height
    val scale = maxDimension.toFloat() / longest
    return max(1, (width * scale).roundToInt()) to max(1, (height * scale).roundToInt())
}

/** Scales [bitmap] down to [MAX_IMAGE_DIMENSION] if needed and encodes it as WEBP. */
fun shrinkAndEncode(bitmap: Bitmap): EncodedImage {
    val (width, height) = scaledSize(bitmap.width, bitmap.height)
    val scaled = if (width == bitmap.width && height == bitmap.height) {
        bitmap
    } else {
        Bitmap.createScaledBitmap(bitmap, width, height, true)
    }
    val bytes = ByteArrayOutputStream().use { out ->
        scaled.compress(Bitmap.CompressFormat.WEBP_LOSSY, WEBP_QUALITY, out)
        out.toByteArray()
    }
    if (scaled !== bitmap) scaled.recycle()
    return EncodedImage(bytes, width, height)
}

/** The pixel size of encoded image [bytes], or null if they can't be decoded. */
fun imageSize(bytes: ByteArray): Pair<Int, Int>? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    return if (bounds.outWidth > 0 && bounds.outHeight > 0) bounds.outWidth to bounds.outHeight else null
}

/** Decodes stored image [bytes] and re-encodes them within [MAX_IMAGE_DIMENSION], or null if they can't be decoded. */
fun decodeAndShrink(bytes: ByteArray): EncodedImage? {
    val (width, height) = imageSize(bytes) ?: return null

    // Decode at the smallest power-of-two reduction that stays at or above the target,
    // so a large photo never needs a full-size bitmap in memory.
    var sampleSize = 1
    while (max(width, height) / (sampleSize * 2) >= MAX_IMAGE_DIMENSION) {
        sampleSize *= 2
    }
    val bitmap = BitmapFactory.decodeByteArray(
        bytes, 0, bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sampleSize }
    ) ?: return null
    return try {
        shrinkAndEncode(bitmap)
    } finally {
        bitmap.recycle()
    }
}
