package org.hahn.maakmai.tags

import org.hahn.maakmai.model.Bookmark
import org.hahn.maakmai.model.TagFolder
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class TagListTest {

    private fun bookmark(vararg tags: String) =
        Bookmark(id = UUID.randomUUID(), title = "", description = "", url = null, tags = tags.toList())

    private fun folder(tag: String, vararg children: TagFolder) =
        TagFolder(id = UUID.randomUUID(), tag = tag, children = children.toList())

    @Test
    fun `counts bookmarks per tag and flags folder tags`() {
        val list = buildTagList(
            bookmarks = listOf(bookmark("knitting", "mittens"), bookmark("knitting"), bookmark("red")),
            rootFolders = listOf(folder("Knitting", folder("mittens")))
        )

        assertEquals(
            listOf(
                TagItem("knitting", 2, hasFolder = true),
                TagItem("mittens", 1, hasFolder = true),
                TagItem("red", 1, hasFolder = false)
            ),
            list
        )
    }

    @Test
    fun `includes folder-only tags with a zero count, once`() {
        val list = buildTagList(
            bookmarks = emptyList(),
            rootFolders = listOf(folder("crochet", folder("hats")), folder("knitting", folder("Hats")))
        )

        assertEquals(listOf("crochet", "hats", "knitting"), list.map { it.tag })
        assertEquals(listOf(0, 0, 0), list.map { it.bookmarkCount })
    }

    @Test
    fun `sorts ignoring case`() {
        val list = buildTagList(listOf(bookmark("beta", "Alpha", "gamma")), emptyList())

        assertEquals(listOf("Alpha", "beta", "gamma"), list.map { it.tag })
    }

    @Test
    fun `counts a bookmark once even if it repeats a tag`() {
        val list = buildTagList(listOf(bookmark("knit", "knit")), emptyList())

        assertEquals(1, list.single().bookmarkCount)
    }
}
