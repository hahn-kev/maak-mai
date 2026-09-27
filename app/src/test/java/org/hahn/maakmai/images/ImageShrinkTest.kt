package org.hahn.maakmai.images

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageShrinkTest {

    @Test
    fun `leaves images within the limit alone`() {
        assertEquals(1600 to 900, scaledSize(1600, 900))
        assertEquals(10 to 10, scaledSize(10, 10))
    }

    @Test
    fun `scales the longest side down to the limit`() {
        assertEquals(1600 to 1200, scaledSize(4000, 3000))
        assertEquals(1200 to 1600, scaledSize(3000, 4000))
    }

    @Test
    fun `never scales a side below one pixel`() {
        assertEquals(1600 to 1, scaledSize(100_000, 10))
    }
}
