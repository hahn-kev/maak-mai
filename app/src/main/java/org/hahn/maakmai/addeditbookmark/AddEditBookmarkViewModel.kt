package org.hahn.maakmai.addeditbookmark

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hahn.maakmai.MaakMaiArgs
import org.hahn.maakmai.data.BookmarkRepository
import org.hahn.maakmai.data.FolderRepository
import org.hahn.maakmai.model.Bookmark
import org.hahn.maakmai.model.TagFolder
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject

data class AddEditBookmarkUiState(
    val title: String = "",
    val description: String = "",
    val url: String? = null,
    val tags: List<String> = listOf(),
    val isLoading: Boolean = false,
    val isBookmarkSaved: Boolean = false,
    val isBookmarkDeleted: Boolean = false,
    val isNew: Boolean = true,
    val selectedFolderPath: List<TagFolder> = listOf(),
    val folders: List<TagFolder> = listOf(),
    val tagsPrioritised: List<TagPrioritised> = listOf(),
    val selectedPriorityTags: List<TagPrioritised> = listOf()
)

data class TagPrioritised(val tag: String, val count: Int)

@HiltViewModel
class AddEditBookmarkViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookmarkRepository: BookmarkRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {
    private val bookmarkId: UUID? = savedStateHandle.get<String?>(MaakMaiArgs.BOOKMARK_ID_ARG).let { id -> if (id.isNullOrBlank()) null else UUID.fromString(id) }
    private val path: String? = savedStateHandle[MaakMaiArgs.PATH_ARG]
    private val sharedUrl: String? = savedStateHandle.get<String?>(MaakMaiArgs.URL_ARG)?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
    private val sharedTitle: String? = savedStateHandle.get<String?>(MaakMaiArgs.BOOKMARK_TITLE_ARG)?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
    private val sharedSubject: String? = savedStateHandle.get<String?>(MaakMaiArgs.SUBJECT_ARG)?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }

    private val _uiState = MutableStateFlow(
        AddEditBookmarkUiState(
            isNew = bookmarkId == null
        )
    )
    val uiState = _uiState.asStateFlow()


    init {
        if (bookmarkId != null) {
            loadBookmark(bookmarkId)
        } else {
            // Handle shared URL if available
            processSharedContent()
        }
        viewModelScope.launch {
            val tags = bookmarkRepository.getTagsWithCount().map { tagsWithCount ->
                TagPrioritised(tagsWithCount.key, tagsWithCount.value)
            }
            _uiState.update {
                it.copy(tagsPrioritised = tags)
            }
        }
        viewModelScope.launch {
            folderRepository.getFoldersStream().collectLatest { folders ->
                _uiState.update {
                    it.copy(
                        folders = folders,
                        selectedFolderPath = path?.let { TagFolder(tag = "/", children = folders, id = UUID.randomUUID()).findFolders(it) } ?: emptyList())
                }
            }
        }
    }

    private fun processSharedContent() {
        if (sharedUrl != null) {
            // If we have a URL, use it and extract a title if needed
            val title = sharedTitle ?: extractTitleFromUrl(sharedUrl)
            val description = sharedSubject ?: ""
            _uiState.update {
                it.copy(
                    title = title,
                    description = description,
                    url = sharedUrl
                )
            }
        } else if (sharedTitle != null) {
            // If we have a title but no URL, use it as the title
            _uiState.update {
                it.copy(
                    title = sharedTitle,
                    description = sharedSubject ?: ""
                )
            }
        }
    }

    private fun extractLastPathSegment(uri: Uri): String? {
        return try {
            val path = uri.path ?: return null
            path.split("/").last().takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    private fun isAllNumbers(str: String): Boolean {
        return str.all { it.isDigit() || it == '.' || it == ',' }
    }

    private fun cleanupTitle(title: String): String {
        return title
            .replace("[_-]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            .trim()
    }

    private fun extractTitleFromUrl(url: String): String {
        try {
            val uri = url.toUri()

            // Try last path segment first
            extractLastPathSegment(uri)?.let { segment ->
                if (!isAllNumbers(segment)) {
                    return cleanupTitle(segment)
                }
            }

            // Try hostname without TLD
            uri.host?.let { host ->
                val parts = host.split(".")
                if (parts.size >= 2) {
                    val domain = parts[parts.size - 2]
                    if (!isAllNumbers(domain)) {
                        return cleanupTitle(domain)
                    }
                }
            }

            // Fallback to full hostname
            return uri.host ?: url
        } catch (e: Exception) {
            return url
        }
    }

    private fun loadBookmark(bookmarkId: UUID) {
        _uiState.update {
            it.copy(
                isLoading = true
            )
        }
        viewModelScope.launch {
            val bookmark = bookmarkRepository.getBookmark(bookmarkId)
            if (bookmark != null) {
                _uiState.update {
                    it.copy(
                        title = bookmark.title,
                        description = bookmark.description,
                        url = bookmark.url,
                        tags = bookmark.tags,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateTitle(newTitle: String) {
        _uiState.update {
            it.copy(title = newTitle)
        }
    }

    fun updateDescription(newDescription: String) {
        _uiState.update {
            it.copy(description = newDescription)
        }
    }

    fun updateUrl(newUrl: String?) {
        _uiState.update {
            it.copy(url = newUrl)
        }
    }

    fun updateTags(newTags: List<String>) {
        _uiState.update {
            it.copy(tags = newTags)
        }
    }

    /**
     * Selects a folder and updates the selected folder path
     * @param folder The folder to select
     */
    fun selectFolder(folder: TagFolder) {
        val currentPath = _uiState.value.selectedFolderPath

        // Check if this folder is already in the path
        val existingIndex = currentPath.indexOfFirst { it.id == folder.id }
        if (existingIndex != -1) {
            // If it's already in the path, truncate the path to this folder
            _uiState.update {
                it.copy(selectedFolderPath = currentPath.subList(0, existingIndex + 1))
            }
        } else {
            // Otherwise, add it to the path
            _uiState.update {
                it.copy(selectedFolderPath = currentPath + folder)
            }
        }
    }

    /**
     * Clears the selected folder path
     */
    fun clearSelectedFolders() {
        _uiState.update {
            it.copy(selectedFolderPath = emptyList())
        }
    }

    fun removeLastSelectedFolder() {
        val currentPath = _uiState.value.selectedFolderPath
        if (currentPath.isNotEmpty()) {
            _uiState.update {
                it.copy(selectedFolderPath = currentPath.dropLast(1))
            }
        }
    }

    fun saveBookmark() {
        viewModelScope.launch {

            val folderTags = uiState.value.selectedFolderPath.map { it.tag }
            val priorityTags = uiState.value.selectedPriorityTags.map {it.tag}
            val bookmark =
                Bookmark(
                    bookmarkId ?: UUID.randomUUID(),
                    uiState.value.title,
                    uiState.value.description,
                    uiState.value.url,
                    (uiState.value.tags.filter { it.isNotBlank() } + folderTags + priorityTags).distinct()
                )
            if (bookmarkId == null) {
                bookmarkRepository.createBookmark(bookmark)
            } else {
                bookmarkRepository.updateBookmark(bookmark)
            }
            _uiState.update {
                it.copy(
                    isBookmarkSaved = true
                )
            }
        }
    }

    fun deleteBookmark() {
        if (bookmarkId == null) {
            return
        }

        viewModelScope.launch {
            bookmarkRepository.deleteBookmark(bookmarkId)
            _uiState.update {
                it.copy(
                    isBookmarkDeleted = true
                )
            }
        }
    }

    /**
     * Toggles a priority tag selection
     * @param tag The priority tag to toggle
     */
    fun togglePriorityTag(tag: TagPrioritised) {
        val currentSelectedTags = _uiState.value.selectedPriorityTags
        val isSelected = currentSelectedTags.any { it.tag == tag.tag }

        _uiState.update {
            if (isSelected) {
                // If already selected, remove it
                it.copy(selectedPriorityTags = currentSelectedTags.filter { t -> t.tag != tag.tag })
            } else {
                // If not selected, add it
                it.copy(selectedPriorityTags = currentSelectedTags + tag)
            }
        }
    }
}
