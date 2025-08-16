package org.hahn.maakmai.data

import kotlinx.coroutines.runBlocking
import org.hahn.maakmai.model.Bookmark
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class BookmarkRepositoryRenameTagTest {

    private lateinit var memoryRepository: BookmarkRepositoryMemory

    @Before
    fun setup() {
        memoryRepository = BookmarkRepositoryMemory()
    }

    @Test
    fun `renameTag replaces tags case-insensitively across all bookmarks and updates counts`() = runBlocking {
        // Precondition: count knitting occurrences from seeded data (see existing test expectation = 9)
        val beforeCounts = memoryRepository.getTagsWithCount()
        val knittingCountBefore = beforeCounts["knitting"] ?: 0
        assertEquals(9, knittingCountBefore)

        // Action: rename with different case on purpose
        memoryRepository.renameTag(oldTag = "KnItTiNg", newTag = "crafts")

        val afterCounts = memoryRepository.getTagsWithCount()
        // knitting should be gone
        assertTrue((afterCounts["knitting"] ?: 0) == 0)
        // crafts should have the same count as knitting had before
        assertEquals(knittingCountBefore, afterCounts["crafts"] ?: 0)

        // Spot-check: unrelated tag counts remain the same
        assertEquals(beforeCounts["mittens"], afterCounts["mittens"]) // unaffected
    }

    @Test
    fun `renameTag de-duplicates tags when merge causes duplicates`() = runBlocking {
        // Arrange: add a bookmark that has multiple case-variants of the same tag
        val id = UUID.randomUUID()
        val bookmark = Bookmark(
            id = id,
            title = "Duplicate tag sample",
            description = "",
            url = null,
            tags = listOf("knitting", "KNITTING", "other")
        )
        memoryRepository.createBookmark(bookmark)

        // Act: renaming to a new single tag should collapse duplicates via distinct()
        memoryRepository.renameTag(oldTag = "kNiTtInG", newTag = "knit")

        val updated = memoryRepository.getBookmark(id)!!
        // Expect only one instance of the new tag
        assertEquals(listOf("knit", "other").sorted(), updated.tags.sorted())
    }

    @Test
    fun `renameTag is a no-op when old and new are equal ignoring case`() = runBlocking {
        val beforeCounts = memoryRepository.getTagsWithCount()

        // Action: rename crochet to CROCHET (same ignoring case)
        memoryRepository.renameTag(oldTag = "crochet", newTag = "CROCHET")

        val afterCounts = memoryRepository.getTagsWithCount()
        // Expect no changes in counts map for relevant tags
        assertEquals(beforeCounts["crochet"], afterCounts["crochet"]) // unchanged
        assertEquals(beforeCounts, afterCounts) // full map unchanged
    }
}
