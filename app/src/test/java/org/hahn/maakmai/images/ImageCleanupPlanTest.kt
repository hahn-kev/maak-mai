package org.hahn.maakmai.images

import org.hahn.maakmai.model.AttachmentInfo
import org.hahn.maakmai.model.Bookmark
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class ImageCleanupPlanTest {

    private fun bookmark(imageId: UUID?, width: Int? = null, height: Int? = null) = Bookmark(
        id = UUID.randomUUID(), title = "", description = "", url = null, tags = emptyList(),
        imageAttachmentId = imageId, imageWidth = width, imageHeight = height
    )

    @Test
    fun `finds unused images and images that need shrinking`() {
        val small = UUID.randomUUID()
        val large = UUID.randomUUID()
        val unknownSize = UUID.randomUUID()
        val unused = UUID.randomUUID()
        val attachments = listOf(small, large, unknownSize, unused).map { AttachmentInfo(it, 1000) }
        val bookmarks = listOf(
            bookmark(small, 1600, 900),
            bookmark(large, 4000, 3000),
            bookmark(unknownSize),
            bookmark(null)
        )

        val plan = planImageCleanup(attachments, bookmarks)

        assertEquals(listOf(unused), plan.unused)
        assertEquals(listOf(large, unknownSize), plan.toShrink)
    }
}
