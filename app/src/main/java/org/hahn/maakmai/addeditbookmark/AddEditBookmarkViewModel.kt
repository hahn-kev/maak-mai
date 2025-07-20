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
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hahn.maakmai.MaakMaiArgs
import org.hahn.maakmai.data.BookmarkRepository
import org.hahn.maakmai.data.FolderRepository
import org.hahn.maakmai.model.Bookmark
import org.hahn.maakmai.model.TagFolder
import org.hahn.maakmai.util.OpenGraphUtils
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.inject.Inject

data class TagGroup(
    val prefix: String,
    val tags: List<TagUiState>
)

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
    val tagsPrioritised: List<TagUiState> = listOf(),
    val groupedFolderTags: List<TagGroup> = listOf()
)

data class TagUiState(val tag: String, val isSelected: Boolean = false, val label: String? = null)

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
            val tags = bookmarkRepository.getTagsWithCount().entries.sortedByDescending { it.value }.map { tagsWithCount ->
                TagUiState(tag = tagsWithCount.key, label = "${tagsWithCount.key} (${tagsWithCount.value})")
            }
            _uiState.update {
                it.copy(tagsPrioritised = tags)
            }
            folderRepository.getFoldersStream().collectLatest { folders ->
                _uiState.update {
                    it.copy(
                        folders = folders,
                        selectedFolderPath = path?.let { TagFolder(tag = "/", children = folders, id = UUID.randomUUID()).findFolders(it) } ?: emptyList())
                }
                updateFolderTags()
            }
        }
    }

    private fun processSharedContent() {
        if (sharedUrl != null) {
            // If we have a URL, use it and extract a title if needed
            viewModelScope.launch {
                // First try to get title from Open Graph metadata
                val openGraph = OpenGraphUtils.extractUrlOpenGraphMetadata(sharedUrl)
                val title = openGraph.title ?: sharedTitle ?: extractTitleFromUrl(sharedUrl)
                var description =  openGraph.description ?: sharedSubject ?: ""
                if (title == description) description = ""
                _uiState.update {
                    it.copy(
                        title = title,
                        description = description,
                        url = sharedUrl
                    )
                }
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
            // Fall back to extracting from URL structure if Open Graph failed
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

        // Update folder tags based on the new selected folder path
        updateFolderTags()
    }

    /**
     * Updates the folder tags based on the current selected folder path
     */
    private fun updateFolderTags() {
        val currentPath = _uiState.value.selectedFolderPath
        val allTags = _uiState.value.tagsPrioritised.map { it.tag }
        val folderTags = if (currentPath.isNotEmpty()) {
            // Get tag groups from the last selected folder
            currentPath.asReversed().flatMap { it.tagGroups.sorted() }
        } else {
            emptyList()
        }

        // Group tags by prefix
        val groupedTags = mutableListOf<TagGroup>()

        // Process each tag
        folderTags.forEach { tag ->
            // Check if this tag should be a prefix (section header)
            val matchingTags = allTags.filter { it != tag && it.startsWith(tag) }

            if (matchingTags.isEmpty()) {
                return@forEach
            }
            // This tag is a prefix for other tags
            val prefixGroup = TagGroup(
                prefix = tag,
                tags = matchingTags.map { TagUiState(it, false, label = it.substring(tag.length + 1)) }
            )

            // Only add if not already added (avoid duplicates)
            if (!groupedTags.any { it.prefix == tag }) {
                groupedTags.add(prefixGroup)
            }
        }

        _uiState.update {
            it.copy(
                groupedFolderTags = groupedTags
            )
        }
    }

    /**
     * Clears the selected folder path
     */
    fun clearSelectedFolders() {
        _uiState.update {
            it.copy(selectedFolderPath = emptyList())
        }
        updateFolderTags()
    }

    fun removeLastSelectedFolder() {
        val currentPath = _uiState.value.selectedFolderPath
        if (currentPath.isNotEmpty()) {
            _uiState.update {
                it.copy(selectedFolderPath = currentPath.dropLast(1))
            }
            updateFolderTags()
        }
    }

    fun saveBookmark() {
        viewModelScope.launch {

            val folderTags = uiState.value.selectedFolderPath.map { it.tag }
            val priorityTags = uiState.value.tagsPrioritised.filter { it.isSelected }.map { it.tag }
            val selectedFolderTags = uiState.value.groupedFolderTags.map { group ->
                group.tags.filter { it.isSelected }.map { it.tag }
            }.flatten()
            val bookmark =
                Bookmark(
                    bookmarkId ?: UUID.randomUUID(),
                    uiState.value.title,
                    uiState.value.description,
                    uiState.value.url,
                    (uiState.value.tags.filter { it.isNotBlank() } + folderTags + priorityTags + selectedFolderTags).distinct()
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
    fun togglePriorityTag(tag: TagUiState) {
        val isSelected = tag.isSelected
        _uiState.getAndUpdate { state ->
            state.copy(
                tagsPrioritised = state.tagsPrioritised.map { if (it.tag == tag.tag) it.copy(isSelected = !isSelected) else it }
            )
        }
    }

    /**
     * Toggles a folder tag selection
     * @param tag The folder tag to toggle
     */
    fun toggleFolderTag(group: TagGroup, tag: TagUiState) {
        val isSelected = tag.isSelected
        _uiState.getAndUpdate { state ->
            state.copy(
                groupedFolderTags = state.groupedFolderTags.map { if (it.prefix == group.prefix) it.copy(tags = it.tags.map { if (it.tag == tag.tag) it.copy(isSelected = !isSelected) else it }) else it }
            )
        }
    }
}
