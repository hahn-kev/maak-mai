package org.hahn.maakmai.images

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class AttachmentImagesTest {

    @Test
    fun `round-trips an attachment id`() {
        val id = UUID.randomUUID()

        assertEquals(id, AttachmentImages.idFrom(AttachmentImages.uriFor(id)))
        assertEquals(AttachmentImage(id), AttachmentImages.imageModel(AttachmentImages.uriFor(id)))
    }

    @Test
    fun `passes other uris through unchanged`() {
        val url = "https://example.com/image.png"

        assertNull(AttachmentImages.idFrom(url))
        assertEquals(url, AttachmentImages.imageModel(url))
        assertNull(AttachmentImages.imageModel(null))
    }
}
