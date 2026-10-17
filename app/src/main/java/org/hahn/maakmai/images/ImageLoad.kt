package org.hahn.maakmai.images

import android.content.Context
import android.net.Uri
import coil3.BitmapImage
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.size.Precision
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads an image via Coil, scaled down to [MAX_IMAGE_DIMENSION], and encodes it for storage.
 * Returns null if the image can't be loaded.
 */
suspend fun loadAndShrink(context: Context, uri: Uri): EncodedImage? = withContext(Dispatchers.IO) {
    try {
        val request = ImageRequest.Builder(context)
            .data(uri)
            // Without a size Coil decodes at full resolution. Inexact lets it decode
            // cheaply at up to twice the size; shrinkAndEncode then scales exactly.
            .size(MAX_IMAGE_DIMENSION)
            .precision(Precision.INEXACT)
            .allowHardware(false)
            .build()
        val image = SingletonImageLoader.get(context).execute(request).image
        (image as? BitmapImage)?.let { shrinkAndEncode(it.bitmap) }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
