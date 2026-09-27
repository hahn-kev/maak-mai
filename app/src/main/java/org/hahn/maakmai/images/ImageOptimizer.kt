package org.hahn.maakmai.images

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hahn.maakmai.data.AttachmentRepository
import org.hahn.maakmai.data.BookmarkRepository
import org.hahn.maakmai.model.AttachmentInfo
import org.hahn.maakmai.model.Bookmark
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

data class ImageCleanupPlan(
    /** Attachments no bookmark points at. */
    val unused: List<UUID>,
    /** Attachments that are larger than [MAX_IMAGE_DIMENSION], or whose size isn't recorded. */
    val toShrink: List<UUID>
)

fun planImageCleanup(attachments: List<AttachmentInfo>, bookmarks: List<Bookmark>): ImageCleanupPlan {
    val byAttachment = bookmarks.filter { it.imageAttachmentId != null }.associateBy { it.imageAttachmentId!! }
    val (used, unused) = attachments.partition { it.id in byAttachment }
    val toShrink = used.filter { info ->
        val bookmark = byAttachment.getValue(info.id)
        val width = bookmark.imageWidth
        val height = bookmark.imageHeight
        width == null || height == null || max(width, height) > MAX_IMAGE_DIMENSION
    }
    return ImageCleanupPlan(unused = unused.map { it.id }, toShrink = toShrink.map { it.id })
}

data class ImageCleanupResult(val shrunk: Int, val removed: Int)

/** Shrinks images stored before resizing was added, and removes images no bookmark uses. */
@Singleton
class ImageOptimizer @Inject constructor(
    private val attachmentRepository: AttachmentRepository,
    private val bookmarkRepository: BookmarkRepository
) {
    suspend fun optimize(): ImageCleanupResult = withContext(Dispatchers.IO) {
        val bookmarks = bookmarkRepository.getBookmarksByTags(emptyList())
        val plan = planImageCleanup(attachmentRepository.getAllInfo(), bookmarks)

        var shrunk = 0
        for (id in plan.toShrink) {
            val original = attachmentRepository.getData(id) ?: continue
            val encoded = decodeAndShrink(original) ?: continue
            val bookmark = bookmarks.first { it.imageAttachmentId == id }
            val (width, height) = if (encoded.bytes.size < original.size) {
                attachmentRepository.replaceData(id, encoded.bytes)
                shrunk++
                encoded.width to encoded.height
            } else {
                // Keep the original, but record its size so it isn't picked up again
                imageSize(original) ?: continue
            }
            if (bookmark.imageWidth != width || bookmark.imageHeight != height) {
                bookmarkRepository.updateBookmark(bookmark.copy(imageWidth = width, imageHeight = height))
            }
        }

        plan.unused.forEach { attachmentRepository.delete(it) }
        ImageCleanupResult(shrunk = shrunk, removed = plan.unused.size)
    }
}
