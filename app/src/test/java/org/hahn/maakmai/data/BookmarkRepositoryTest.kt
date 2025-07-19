package org.hahn.maakmai.data

import kotlinx.coroutines.runBlocking
import org.hahn.maakmai.model.Bookmark
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.UUID

class BookmarkRepositoryTest {

    private lateinit var memoryRepository: BookmarkRepositoryMemory

    @Before
    fun setup() {
        // Setup memory repository
        memoryRepository = BookmarkRepositoryMemory()
    }

    @Test
    fun `getTagsWithCount returns correct counts for memory repository`() = runBlocking {
        // The memory repository is pre-populated with bookmarks
        // Test the getTagsWithCount method
        val tagCounts = memoryRepository.getTagsWithCount()

        // Verify expected counts based on the pre-populated data
        assertEquals(9, tagCounts["knitting"]) // 9 bookmarks have "knitting" tag
        assertEquals(6, tagCounts["mittens"]) // 6 bookmarks have "mittens" tag
        assertEquals(6, tagCounts["crochet"]) // 6 bookmarks have "crochet" tag
        assertEquals(3, tagCounts["scarf"]) // 3 bookmarks have "scarf" tag
        assertEquals(3, tagCounts["sweater"]) // 3 bookmarks have "sweater" tag
    }
}
