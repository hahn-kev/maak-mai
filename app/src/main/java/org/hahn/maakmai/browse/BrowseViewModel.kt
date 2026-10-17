package org.hahn.maakmai.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hahn.maakmai.MaakMaiArgs.PATH_ARG
import org.hahn.maakmai.addeditbookmark.TagGroup
import org.hahn.maakmai.addeditbookmark.TagSelection
import org.hahn.maakmai.addeditbookmark.TagUiState
import org.hahn.maakmai.data.BookmarkRepository
import org.hahn.maakmai.data.FolderRepository
import org.hahn.maakmai.model.Bookmark
import org.hahn.maakmai.model.TagFolder
import org.hahn.maakmai.util.WhileUiSubscribed
import java.util.UUID
import javax.inject.Inject

data class BrowseUiState(
    val path: String,
    val visibleFolders: List<FolderViewModel>,
    val visibleBookmarks: List<Bookmark>,
    val loading: Boolean = false,
    val showAll: Boolean = false,
    val currentFolderId: UUID? = null,
    val searchQuery: String = "",
    val sortOrder: BookmarkSort = BookmarkSort.CREATED_NEWEST,
    /** Ids of the selected bookmarks while selecting bookmarks, or null when not selecting. */
    val selectedIds: Set<UUID>? = null
)

/** The pop-up that adds tags to all selected bookmarks. */
data class RetagUiState(
    val bookmarkCount: Int,
    val tags: String = "",
    val folders: List<TagFolder> = listOf(),
    val selectedFolderPath: List<TagFolder> = listOf(),
    val tagsPrioritised: List<TagUiState> = listOf(),
    val groupedFolderTags: List<TagGroup> = listOf(),
    val isSaving: Boolean = false
) {
    val tagsToAdd: List<String>
        get() = TagSelection.selectedTags(tags, selectedFolderPath, tagsPrioritised, groupedFolderTags)
}

data class FolderViewModel(
    val folder: TagFolder,
    val path: String
)

const val CURRENT_PATH_SAVED_STATE_KEY = "CURRENT_PATH_SAVED_STATE_KEY"
const val SHOW_ALL_SAVED_STATE_KEY = "SHOW_ALL_SAVED_STATE_KEY"
const val SEARCH_QUERY_SAVED_STATE_KEY = "SEARCH_QUERY_SAVED_STATE_KEY"
const val SORT_ORDER_SAVED_STATE_KEY = "SORT_ORDER_SAVED_STATE_KEY"

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val bookmarkRepository: BookmarkRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {
    private val currentPath: String = savedStateHandle[PATH_ARG]!!
    private val _showAll = savedStateHandle.getStateFlow(SHOW_ALL_SAVED_STATE_KEY, false)
    private val _searchQuery = savedStateHandle.getStateFlow(SEARCH_QUERY_SAVED_STATE_KEY, "")
    private val _sortOrder = savedStateHandle
        .getStateFlow(SORT_ORDER_SAVED_STATE_KEY, BookmarkSort.CREATED_NEWEST.name)
        .map { value -> runCatching { BookmarkSort.valueOf(value) }.getOrDefault(BookmarkSort.CREATED_NEWEST) }
        .distinctUntilChanged()
    private val _bookmarks = bookmarkRepository.getBookmarksStream()
    private val _rootFolder = folderRepository.getFoldersStream()
        .map { folders -> TagFolder(tag = "root", children = folders, id = UUID.randomUUID()) }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    private val _tags = currentPath.split("/").filter { f -> f.isNotEmpty() }.toHashSet()
    private val _currentFolder = _rootFolder
        .map { root -> root.findFolder(currentPath) ?: root }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
    private val _visibleFolders = _currentFolder
        .map { folder -> folder.children }
        .flowOn(Dispatchers.Default)
    private val _visibleBookmarks = combine(_bookmarks, _currentFolder, _showAll, _searchQuery, _sortOrder)
    { bookmarks, currentFolder, showAll, searchQuery, sortOrder ->
        val filteredBookmarks = getVisibleBookmarks(
            _tags, if (!showAll) {
                currentFolder
            } else {
                null
            }, bookmarks
        )

        val searchedBookmarks = if (searchQuery.isNotEmpty()) {
            filteredBookmarks.filter { bookmark ->
                bookmark.title?.contains(searchQuery, ignoreCase = true) == true ||
                bookmark.url?.contains(searchQuery, ignoreCase = true) == true ||
                bookmark.description?.contains(searchQuery, ignoreCase = true) == true ||
                bookmark.tags.any { tag -> tag.contains(searchQuery, ignoreCase = true) }
            }
        } else {
            filteredBookmarks
        }
        sortBookmarks(searchedBookmarks, sortOrder)
    }
    .distinctUntilChanged()
    .flowOn(Dispatchers.Default)
    private val _isLoading = MutableStateFlow(false)
    private val _selectedIds = MutableStateFlow<Set<UUID>?>(null)

    private val _retagState = MutableStateFlow<RetagUiState?>(null)
    val retagState: StateFlow<RetagUiState?> = _retagState.asStateFlow()

    /** A short message for the snackbar, cleared by [userMessageShown]. */
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    val uiState: StateFlow<BrowseUiState> = combine(_isLoading, _visibleFolders, _visibleBookmarks, _showAll, _currentFolder, _searchQuery, _sortOrder, _selectedIds)
    { arr ->
        val loading = arr[0] as Boolean
        val visibleFolders = arr[1] as List<TagFolder>
        val visibleBookmarks = arr[2] as List<Bookmark>
        val showAll = arr[3] as Boolean
        val currentFolder = arr[4] as TagFolder
        val searchQuery = arr[5] as String
        val sortOrder = arr[6] as BookmarkSort
        val selectedIds = arr[7] as Set<UUID>?

        BrowseUiState(
            path = currentPath,
            visibleFolders = visibleFolders
                .sortedBy { it.tag.lowercase() }
                .map { folder -> FolderViewModel(folder, openFolderPath(currentPath, folder)) },
            visibleBookmarks = visibleBookmarks,
            loading = loading,
            showAll = showAll,
            currentFolderId = if (currentPath != "/") currentFolder.id else null,
            searchQuery = searchQuery,
            sortOrder = sortOrder,
            selectedIds = selectedIds
        )
    }
    .flowOn(Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = WhileUiSubscribed,
        initialValue = BrowseUiState("/", listOf(), listOf(), true, false)
    )

    private fun openFolderPath(currentPath: String, tagFolder: TagFolder): String {
        var path = currentPath;
        if (path == "/") {
            path += tagFolder.tag
        } else {
            path = "$path/${tagFolder.tag}"
        }
        return path
    }

    fun onBack() {
        var path = currentPath
        val lastIndex = path.lastIndexOf("/");
        if (lastIndex == -1) return
        path = path.substring(0, lastIndex)
        if (path.isEmpty()) {
            path = "/"
        }
        setPath(path)
    }

    private fun setPath(path: String) {
        savedStateHandle[CURRENT_PATH_SAVED_STATE_KEY] = path
    }

    fun setShowAll(showAll: Boolean) {
        savedStateHandle[SHOW_ALL_SAVED_STATE_KEY] = showAll
    }

    fun setSearchQuery(query: String) {
        savedStateHandle[SEARCH_QUERY_SAVED_STATE_KEY] = query
    }

    fun setSortOrder(sortOrder: BookmarkSort) {
        savedStateHandle[SORT_ORDER_SAVED_STATE_KEY] = sortOrder.name
    }

    fun startSelecting() {
        _selectedIds.value = emptySet()
    }

    fun stopSelecting() {
        _selectedIds.value = null
        _retagState.value = null
    }

    fun toggleSelected(id: UUID) {
        _selectedIds.update { selected -> selected?.let { if (id in it) it - id else it + id } }
    }

    fun selectAllVisible() {
        viewModelScope.launch {
            val visibleIds = _visibleBookmarks.first().map { it.id }
            _selectedIds.update { selected -> selected?.let { it + visibleIds } }
        }
    }

    /** Opens the pop-up to add tags to the selected bookmarks, starting at the current folder as the Add screen does. */
    fun openRetag() {
        val count = _selectedIds.value?.size ?: return
        if (count == 0) return
        _retagState.value = RetagUiState(bookmarkCount = count)
        viewModelScope.launch {
            val tags = TagSelection.prioritisedTags(bookmarkRepository.getTagsWithCount())
            val folders = folderRepository.getFoldersStream().first()
            val path = TagSelection.folderPathFor(folders, currentPath)
            updateRetag {
                it.copy(
                    tagsPrioritised = tags,
                    folders = folders,
                    selectedFolderPath = path,
                    groupedFolderTags = TagSelection.groupFolderTags(path, tags.map { tag -> tag.tag })
                )
            }
        }
    }

    fun dismissRetag() {
        _retagState.value = null
    }

    private fun updateRetag(transform: (RetagUiState) -> RetagUiState) {
        _retagState.update { it?.let(transform) }
    }

    fun updateRetagTags(tags: String) = updateRetag { it.copy(tags = tags) }

    fun selectRetagFolder(folder: TagFolder) = updateRetag {
        val path = TagSelection.selectFolder(it.selectedFolderPath, folder)
        it.copy(selectedFolderPath = path, groupedFolderTags = TagSelection.groupFolderTags(path, it.tagsPrioritised.map { tag -> tag.tag }))
    }

    fun removeLastRetagFolder() = updateRetag {
        val path = it.selectedFolderPath.dropLast(1)
        it.copy(selectedFolderPath = path, groupedFolderTags = TagSelection.groupFolderTags(path, it.tagsPrioritised.map { tag -> tag.tag }))
    }

    fun toggleRetagPriorityTag(tag: TagUiState) = updateRetag {
        it.copy(tagsPrioritised = TagSelection.togglePriorityTag(it.tagsPrioritised, tag))
    }

    fun toggleRetagFolderTag(group: TagGroup, tag: TagUiState) = updateRetag {
        it.copy(groupedFolderTags = TagSelection.toggleFolderTag(it.groupedFolderTags, group, tag))
    }

    /** Adds the picked tags to every selected bookmark, keeping the tags each one already has. */
    fun applyRetag() {
        val retag = _retagState.value ?: return
        val ids = _selectedIds.value ?: return
        val tagsToAdd = retag.tagsToAdd
        if (tagsToAdd.isEmpty() || retag.isSaving) return
        updateRetag { it.copy(isSaving = true) }
        viewModelScope.launch {
            var updated = 0
            ids.forEach { id ->
                val bookmark = bookmarkRepository.getBookmark(id) ?: return@forEach
                bookmarkRepository.updateBookmark(bookmark.copy(tags = (bookmark.tags + tagsToAdd).distinct()))
                updated++
            }
            stopSelecting()
            val bookmarks = if (updated == 1) "1 bookmark" else "$updated bookmarks"
            _userMessage.value = "Added ${tagsToAdd.joinToString(", ")} to $bookmarks"
        }
    }

    fun userMessageShown() {
        _userMessage.value = null
    }

    private fun getCurrentFolder(path: String, tagFolders: List<TagFolder>): TagFolder? {
        return tagFolders.stream()
            .map { f -> f.findFolder(path) }
            .filter { f -> f != null }
            .findFirst()
            .orElse(null)
    }

    private fun getVisibleBookmarks(
        tags: HashSet<String>,
        currentFolder: TagFolder?,
        bookmarks: List<Bookmark>
    ): List<Bookmark> {
        var visibleBookmarks = bookmarks;
        if (currentFolder != null) {
            visibleBookmarks = visibleBookmarks.filter { b -> !b.tags.any { tag -> currentFolder.children.any { f -> f.tag == tag } } }
        }
        if (tags.size >= 1) {
            return visibleBookmarks.filter { b -> b.tags.containsAll(tags) };
        } else {
            return visibleBookmarks;
        }
    }
}
