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
import org.hahn.maakmai.model.Bookmark
import org.hahn.maakmai.model.TagFolder
import org.hahn.maakmai.util.WhileUiSubscribed
import javax.inject.Inject

data class BrowseUiState(
    val path: String,
    val visibleFolders: List<TagFolder>,
    val visibleBookmarks: List<Bookmark>,
    val loading: Boolean = false,
    val showAll: Boolean = false
)

const val CURRENT_PATH_SAVED_STATE_KEY = "CURRENT_PATH_SAVED_STATE_KEY"
const val SHOW_ALL_SAVED_STATE_KEY = "SHOW_ALL_SAVED_STATE_KEY"

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val _savedPath = savedStateHandle.getStateFlow(CURRENT_PATH_SAVED_STATE_KEY, "/")
    private val _showAll = savedStateHandle.getStateFlow(SHOW_ALL_SAVED_STATE_KEY, false)
    private val _bookmarks = MutableStateFlow(listOf<Bookmark>())
    private val _tagFolders = MutableStateFlow(listOf<TagFolder>())

    private val _tags = _savedPath.map { path -> path.split("/").filter { f -> f.isNotEmpty() }.toHashSet() }
    private val _currentFolder = _savedPath.combine(_tagFolders) { path, folders ->
        getCurrentFolder(path, folders)
    }
    private val _visibleFolders = _currentFolder.combine(_tagFolders) { currentFolder, folders ->
        getVisibleFolders(currentFolder, folders)
    }
    private val _visibleBookmarks = combine(_bookmarks, _currentFolder, _showAll, _tags, _visibleFolders)
    { bookmarks, currentFolder, showAll, tags, visibleFolders ->
        getVisibleBookmarks(
            tags, if (!showAll) {
                currentFolder ?: TagFolder(tag = "root", children = visibleFolders.map { f -> f.tag })
            } else {
                null
            }, bookmarks
        )
    }
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<BrowseUiState> = combine(_savedPath, _isLoading, _visibleFolders, _visibleBookmarks, _showAll)
    { path, loading, visibleFolders, visibleBookmarks, showAll ->
        BrowseUiState(
            path = path,
            visibleFolders = visibleFolders,
            visibleBookmarks = visibleBookmarks,
            loading = loading,
            showAll = showAll
        )
    }.stateIn(
        scope = viewModelScope,
        started = WhileUiSubscribed,
        initialValue = BrowseUiState("/", listOf(), listOf(), true, false)
    )

    fun openFolder(tagFolder: TagFolder) {
        var path = _savedPath.value
        if (path == "/") {
            path += tagFolder.tag
        } else {
            path = "$path/${tagFolder.tag}"
        }
        setPath(path)
    }

    fun onBack() {
        var path = _savedPath.value
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


    private fun getVisibleFolders(currentFolder: TagFolder?, tagFolders: List<TagFolder>): List<TagFolder> {
        return if (currentFolder != null) {
            currentFolder.children.map { c -> tagFolders.single { f -> f.tag == c } }
        } else {
            tagFolders.filter { folder -> folder.rootFolder };
        }
    }

    private fun getCurrentFolder(path: String, tagFolders: List<TagFolder>): TagFolder? {
        val folder = path.split("/").filter { f -> f.isNotEmpty() }.lastOrNull() ?: return null;
        val currentFolder = tagFolders.singleOrNull { f -> f.tag == folder };
        return currentFolder
    }

    private fun getVisibleBookmarks(
        tags: HashSet<String>,
        currentFolder: TagFolder?,
        bookmarks: List<Bookmark>
    ): List<Bookmark> {
        var visibleBookmarks = bookmarks;
        if (currentFolder != null) {
            visibleBookmarks = visibleBookmarks.filter { b -> !b.tags.any { tag -> currentFolder.children.contains(tag) } }
        }
        if (tags.size >= 1) {
            return visibleBookmarks.filter { b -> b.tags.containsAll(tags) };
        } else {
            return visibleBookmarks;
        }
    }
}