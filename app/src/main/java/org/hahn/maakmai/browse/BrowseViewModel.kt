package org.hahn.maakmai.browse

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.hahn.maakmai.MaakMaiArgs.PATH_ARG
import org.hahn.maakmai.data.BookmarkRepository
import org.hahn.maakmai.data.FolderRepository
import org.hahn.maakmai.model.Bookmark
import org.hahn.maakmai.model.TagFolder
import org.hahn.maakmai.util.WhileUiSubscribed
import java.io.File
import java.util.UUID
import javax.inject.Inject

data class BrowseUiState(
    val path: String,
    val visibleFolders: List<FolderViewModel>,
    val visibleBookmarks: List<Bookmark>,
    val loading: Boolean = false,
    val showAll: Boolean = false,
    val currentFolderId: UUID? = null,
    val searchQuery: String = ""
)

data class FolderViewModel(
    val folder: TagFolder,
    val path: String
)

const val CURRENT_PATH_SAVED_STATE_KEY = "CURRENT_PATH_SAVED_STATE_KEY"
const val SHOW_ALL_SAVED_STATE_KEY = "SHOW_ALL_SAVED_STATE_KEY"
const val SEARCH_QUERY_SAVED_STATE_KEY = "SEARCH_QUERY_SAVED_STATE_KEY"

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val bookmarkRepository: BookmarkRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {
    private val currentPath: String = savedStateHandle[PATH_ARG]!!
    private val _showAll = savedStateHandle.getStateFlow(SHOW_ALL_SAVED_STATE_KEY, false)
    private val _searchQuery = savedStateHandle.getStateFlow(SEARCH_QUERY_SAVED_STATE_KEY, "")
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
    private val _visibleBookmarks = combine(_bookmarks, _currentFolder, _showAll, _searchQuery)
    { bookmarks, currentFolder, showAll, searchQuery ->
        val filteredBookmarks = getVisibleBookmarks(
            _tags, if (!showAll) {
                currentFolder
            } else {
                null
            }, bookmarks
        )

        // Apply search filter if search query is not empty
        if (searchQuery.isNotEmpty()) {
            filteredBookmarks.filter { bookmark ->
                bookmark.title?.contains(searchQuery, ignoreCase = true) == true ||
                bookmark.url?.contains(searchQuery, ignoreCase = true) == true ||
                bookmark.description?.contains(searchQuery, ignoreCase = true) == true
            }
        } else {
            filteredBookmarks
        }
    }
    .distinctUntilChanged()
    .flowOn(Dispatchers.Default)
    private val _isLoading = MutableStateFlow(false)

    val uiState: StateFlow<BrowseUiState> = combine(_isLoading, _visibleFolders, _visibleBookmarks, _showAll, _currentFolder, _searchQuery)
    { arr ->
        val loading = arr[0] as Boolean
        val visibleFolders = arr[1] as List<TagFolder>
        val visibleBookmarks = arr[2] as List<Bookmark>
        val showAll = arr[3] as Boolean
        val currentFolder = arr[4] as TagFolder
        val searchQuery = arr[5] as String

        BrowseUiState(
            path = currentPath,
            visibleFolders = visibleFolders
                .sortedBy { it.tag.lowercase() }
                .map { folder -> FolderViewModel(folder, openFolderPath(currentPath, folder)) },
            visibleBookmarks = visibleBookmarks,
            loading = loading,
            showAll = showAll,
            currentFolderId = if (currentPath != "/") currentFolder.id else null,
            searchQuery = searchQuery
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

    fun shareDatabase(context: Context) {
        val dbName = "MaakMai.db"
        val dbFile = context.getDatabasePath(dbName)

        if (!dbFile.exists()) {
            // Handle case where database file doesn't exist
            return
        }

        val cacheDir = context.cacheDir
        val sharedDb = File(cacheDir, "shared_db.db")

        dbFile.copyTo(sharedDb, true)

        val uri = FileProvider.getUriForFile(context, "org.hahn.maakmai.provider", sharedDb)

        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "application/x-sqlite3"
        intent.putExtra(Intent.EXTRA_STREAM, uri)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

        val chooser = Intent.createChooser(intent, "Share Database")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
