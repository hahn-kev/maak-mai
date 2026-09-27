package org.hahn.maakmai.images

import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.key.Keyer
import coil3.request.Options
import okio.Buffer
import org.hahn.maakmai.data.AttachmentRepository
import java.util.UUID

/** Coil model for an image stored in the attachments table. */
data class AttachmentImage(val id: UUID)

/**
 * The add/edit screen tracks its image as a string: a gallery URI, a web URL, or a stored
 * attachment. Stored attachments use this scheme, and [imageModel] turns them into an
 * [AttachmentImage] for Coil.
 */
object AttachmentImages {
    private const val PREFIX = "maakmai-attachment:"

    fun uriFor(id: UUID): String = "$PREFIX$id"

    fun idFrom(uri: String?): UUID? {
        if (uri == null || !uri.startsWith(PREFIX)) return null
        return runCatching { UUID.fromString(uri.removePrefix(PREFIX)) }.getOrNull()
    }

    fun imageModel(uri: String?): Any? = idFrom(uri)?.let(::AttachmentImage) ?: uri
}

class AttachmentFetcher(
    private val image: AttachmentImage,
    private val options: Options,
    private val attachmentRepository: AttachmentRepository
) : Fetcher {
    override suspend fun fetch(): FetchResult? {
        val bytes = attachmentRepository.getData(image.id) ?: return null
        return SourceFetchResult(
            source = ImageSource(Buffer().write(bytes), options.fileSystem),
            mimeType = null,
            dataSource = DataSource.DISK
        )
    }

    class Factory(private val attachmentRepository: AttachmentRepository) : Fetcher.Factory<AttachmentImage> {
        override fun create(data: AttachmentImage, options: Options, imageLoader: ImageLoader): Fetcher =
            AttachmentFetcher(data, options, attachmentRepository)
    }
}

/** Lets Coil's memory cache hold attachment images. */
class AttachmentKeyer : Keyer<AttachmentImage> {
    override fun key(data: AttachmentImage, options: Options): String = "attachment:${data.id}"
}
