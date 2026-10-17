package org.hahn.maakmai.browse

import androidx.lifecycle.SavedStateHandle
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.hahn.maakmai.MaakMaiArgs
import org.hahn.maakmai.data.BookmarkRepositoryMemory
import org.hahn.maakmai.data.FolderRepositoryMemory
import org.hahn.maakmai.model.Bookmark
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrowseViewModelRetagTest {

    private lateinit var bookmarks: BookmarkRepositoryMemory
    private lateinit var viewModel: BrowseViewModel
    private lateinit var all: List<Bookmark>

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        bookmarks = BookmarkRepositoryMemory()
        viewModel = BrowseViewModel(
            SavedStateHandle(mapOf(MaakMaiArgs.PATH_ARG to "/")),
            bookmarks,
            FolderRepositoryMemory()
        )
        all = runBlocking { bookmarks.getBookmarksByTags(emptyList()) }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun tagsOf(bookmark: Bookmark) = runBlocking { bookmarks.getBookmark(bookmark.id)!!.tags }

    @Test
    fun `adds the picked tags to each selected bookmark and keeps their tags`() {
        val (first, second, other) = all
        viewModel.startSelecting()
        viewModel.toggleSelected(first.id)
        viewModel.toggleSelected(second.id)

        viewModel.openRetag()
        assertThat(viewModel.retagState.value!!.bookmarkCount).isEqualTo(2)
        viewModel.updateRetagTags("imported, todo")
        viewModel.toggleRetagPriorityTag(viewModel.retagState.value!!.tagsPrioritised.first { it.tag == "knitting" })
        viewModel.applyRetag()

        assertThat(tagsOf(first)).containsAtLeastElementsIn((first.tags + listOf("imported", "todo", "knitting")).distinct())
        assertThat(tagsOf(first)).containsNoDuplicates()
        assertThat(tagsOf(second)).containsAtLeastElementsIn((second.tags + listOf("imported", "todo", "knitting")).distinct())
        assertThat(tagsOf(other)).isEqualTo(other.tags)

        // Adding tags ends selecting and reports what was done
        assertThat(viewModel.retagState.value).isNull()
        assertThat(viewModel.userMessage.value).isEqualTo("Added imported, todo, knitting to 2 bookmarks")
    }

    @Test
    fun `toggling a bookmark twice clears it`() {
        val (first, second) = all
        viewModel.startSelecting()
        viewModel.toggleSelected(first.id)
        viewModel.toggleSelected(second.id)
        viewModel.toggleSelected(first.id)

        viewModel.openRetag()

        assertThat(viewModel.retagState.value!!.bookmarkCount).isEqualTo(1)
    }

    @Test
    fun `nothing happens without a selection or without tags`() {
        viewModel.openRetag()
        assertThat(viewModel.retagState.value).isNull()

        viewModel.startSelecting()
        viewModel.openRetag()
        assertThat(viewModel.retagState.value).isNull()

        val first = all.first()
        viewModel.toggleSelected(first.id)
        viewModel.openRetag()
        viewModel.applyRetag()
        assertThat(tagsOf(first)).isEqualTo(first.tags)
        assertThat(viewModel.retagState.value).isNotNull()
    }

    @Test
    fun `cancelling keeps the selection and changes nothing`() {
        val first = all.first()
        viewModel.startSelecting()
        viewModel.toggleSelected(first.id)
        viewModel.openRetag()
        viewModel.updateRetagTags("imported")
        viewModel.dismissRetag()

        assertThat(viewModel.retagState.value).isNull()
        assertThat(tagsOf(first)).isEqualTo(first.tags)
        viewModel.openRetag()
        assertThat(viewModel.retagState.value!!.bookmarkCount).isEqualTo(1)
    }

    @Test
    fun `retag starts with the tags of the current folder picked`() {
        val inFolder = BrowseViewModel(
            SavedStateHandle(mapOf(MaakMaiArgs.PATH_ARG to "/knitting")),
            bookmarks,
            FolderRepositoryMemory()
        )
        inFolder.startSelecting()
        inFolder.toggleSelected(all.first().id)
        inFolder.openRetag()

        assertThat(inFolder.retagState.value!!.tagsToAdd).containsExactly("knitting")
    }
}
