package org.hahn.maakmai.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hahn.maakmai.data.BookmarkRepository
import org.hahn.maakmai.data.FolderRepository
import org.hahn.maakmai.data.RenameCheck
import org.hahn.maakmai.data.TagRenamer
import org.hahn.maakmai.model.Bookmark
import org.hahn.maakmai.model.TagFolder
import org.hahn.maakmai.util.WhileUiSubscribed
import javax.inject.Inject

data class TagItem(
    val tag: String,
    val bookmarkCount: Int,
    val hasFolder: Boolean
)

data class PendingRename(val oldTag: String, val newTag: String)

data class TagsUiState(
    val tags: List<TagItem> = emptyList(),
    val loading: Boolean = true,
    val pendingMerge: PendingRename? = null,
    val errorMessage: String? = null
)

/**
 * Every tag used by a bookmark or a folder, sorted by name. Folder tags match bookmark
 * tags ignoring case; a tag used only by folders has a count of 0.
 */
fun buildTagList(bookmarks: List<Bookmark>, rootFolders: List<TagFolder>): List<TagItem> {
    val counts = bookmarks.flatMap { it.tags.distinct() }.groupingBy { it }.eachCount()
    val folderTags = mutableListOf<String>()
    fun collect(folders: List<TagFolder>) {
        folders.forEach { folderTags += it.tag; collect(it.children) }
    }
    collect(rootFolders)

    val items = counts.map { (tag, count) ->
        TagItem(tag, count, folderTags.any { it.equals(tag, ignoreCase = true) })
    }
    val folderOnly = folderTags
        .filter { folderTag -> counts.keys.none { it.equals(folderTag, ignoreCase = true) } }
        .distinctBy { it.lowercase() }
        .map { TagItem(it, 0, true) }
    return (items + folderOnly).sortedBy { it.tag.lowercase() }
}

@HiltViewModel
class TagsViewModel @Inject constructor(
    bookmarkRepository: BookmarkRepository,
    folderRepository: FolderRepository,
    private val tagRenamer: TagRenamer
) : ViewModel() {
    private val _pendingMerge = MutableStateFlow<PendingRename?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _tags = combine(
        bookmarkRepository.getBookmarksStream(),
        folderRepository.getFoldersStream()
    ) { bookmarks, folders -> buildTagList(bookmarks, folders) }
        .flowOn(Dispatchers.Default)

    val uiState: StateFlow<TagsUiState> = combine(_tags, _pendingMerge, _errorMessage) { tags, pendingMerge, error ->
        TagsUiState(tags = tags, loading = false, pendingMerge = pendingMerge, errorMessage = error)
    }.stateIn(
        scope = viewModelScope,
        started = WhileUiSubscribed,
        initialValue = TagsUiState()
    )

    fun requestRename(oldTag: String, newTag: String) {
        viewModelScope.launch {
            when (tagRenamer.check(oldTag, newTag)) {
                RenameCheck.NoChange -> Unit
                RenameCheck.Rename -> rename(oldTag, newTag)
                RenameCheck.Merge -> _pendingMerge.value = PendingRename(oldTag, newTag.trim())
                RenameCheck.Conflict -> _errorMessage.value = conflictMessage(newTag.trim())
            }
        }
    }

    fun confirmMerge() {
        val pending = _pendingMerge.value ?: return
        _pendingMerge.value = null
        viewModelScope.launch { rename(pending.oldTag, pending.newTag) }
    }

    fun cancelMerge() {
        _pendingMerge.value = null
    }

    fun errorShown() {
        _errorMessage.update { null }
    }

    private suspend fun rename(oldTag: String, newTag: String) {
        tagRenamer.rename(oldTag, newTag).onFailure { _errorMessage.value = conflictMessage(newTag) }
    }
}
