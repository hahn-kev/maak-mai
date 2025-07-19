package org.hahn.maakmai.browse

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.hahn.maakmai.MaakMaiArgs.PATH_ARG
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
    val showAll: Boolean = false
)

data class FolderViewModel(
    val folder: TagFolder,
    val path: String
)

const val CURRENT_PATH_SAVED_STATE_KEY = "CURRENT_PATH_SAVED_STATE_KEY"
const val SHOW_ALL_SAVED_STATE_KEY = "SHOW_ALL_SAVED_STATE_KEY"

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val bookmarkRepository: BookmarkRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {
    private val currentPath: String = savedStateHandle[PATH_ARG]!!
    private val _showAll = savedStateHandle.getStateFlow(SHOW_ALL_SAVED_STATE_KEY, false)
    private val _bookmarks = bookmarkRepository.getBookmarksStream()
    private val _tagFolders = folderRepository.getFoldersStream()

    private val _tags = currentPath.split("/").filter { f -> f.isNotEmpty() }.toHashSet()
    private val _currentFolder = _tagFolders.map { folders ->
        getCurrentFolder(currentPath, folders)
    }
    private val _visibleFolders = combine(_currentFolder, _tagFolders) { currentFolder, folders -> currentFolder?.children ?: folders }
    private val _visibleBookmarks = combine(_bookmarks, _currentFolder, _showAll, _visibleFolders)
    { bookmarks, currentFolder, showAll, visibleFolders ->
        getVisibleBookmarks(
            _tags, if (!showAll) {
                currentFolder ?: TagFolder(tag = "root", children = visibleFolders, id = UUID.randomUUID())
            } else {
                null
            }, bookmarks
        )
    }
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<BrowseUiState> = combine(_isLoading, _visibleFolders, _visibleBookmarks, _showAll)
    { loading, visibleFolders, visibleBookmarks, showAll ->
        BrowseUiState(
            path = currentPath,
            visibleFolders = visibleFolders.map { folder -> FolderViewModel(folder, openFolderPath(currentPath, folder)) },
            visibleBookmarks = visibleBookmarks,
            loading = loading,
            showAll = showAll
        )
    }.stateIn(
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