package org.hahn.maakmai.images

import android.graphics.Bitmap
import android.os.Build
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.hahn.maakmai.data.AttachmentRepository
import org.hahn.maakmai.data.BookmarkRepositoryMemory
import org.hahn.maakmai.model.Attachment
import org.hahn.maakmai.model.AttachmentInfo
import org.hahn.maakmai.model.Bookmark
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE]) // API 34
class RecordImageSizesTest {

    private class Attachments(private val data: Map<UUID, ByteArray>) : AttachmentRepository {
        override suspend fun create(attachment: Attachment) {}
        override suspend fun getData(id: UUID): ByteArray? = data[id]
        override suspend fun getAllInfo(): List<AttachmentInfo> = emptyList()
        override suspend fun replaceData(id: UUID, data: ByteArray) = false
        override suspend fun delete(id: UUID) = false
    }

    private fun png(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        return ByteArrayOutputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it); it.toByteArray() }
    }

    private fun bookmark(imageId: UUID?, width: Int? = null, height: Int? = null) = Bookmark(
        id = UUID.randomUUID(), title = "t", description = "", url = null, tags = emptyList(),
        imageAttachmentId = imageId, imageWidth = width, imageHeight = height
    )

    @Test
    fun `only bookmarks with an image but no recorded size are missing a size`() {
        val noImage = bookmark(null)
        val sized = bookmark(UUID.randomUUID(), 10, 20)
        val noSize = bookmark(UUID.randomUUID())
        val halfSize = bookmark(UUID.randomUUID(), width = 10)

        assertThat(bookmarksMissingImageSize(listOf(noImage, sized, noSize, halfSize)))
            .containsExactly(noSize, halfSize)
    }

    @Test
    fun `records the size of stored images that have none`() = runBlocking {
        val imageId = UUID.randomUUID()
        val sizedId = UUID.randomUUID()
        val bookmarks = BookmarkRepositoryMemory()
        val noSize = bookmark(imageId)
        val sized = bookmark(sizedId, 7, 7)
        val missingData = bookmark(UUID.randomUUID())
        listOf(noSize, sized, missingData).forEach { bookmarks.createBookmark(it) }
        val optimizer = ImageOptimizer(Attachments(mapOf(imageId to png(40, 30), sizedId to png(40, 30))), bookmarks)

        val recorded = optimizer.recordMissingSizes()

        assertThat(recorded).isEqualTo(1)
        assertThat(bookmarks.getBookmark(noSize.id)!!.let { it.imageWidth to it.imageHeight }).isEqualTo(40 to 30)
        // A recorded size is left alone, and a missing attachment is skipped
        assertThat(bookmarks.getBookmark(sized.id)).isEqualTo(sized)
        assertThat(bookmarks.getBookmark(missingData.id)).isEqualTo(missingData)
    }
}
