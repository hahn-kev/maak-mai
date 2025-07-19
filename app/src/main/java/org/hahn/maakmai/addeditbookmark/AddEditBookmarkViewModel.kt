package org.hahn.maakmai.addeditbookmark

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hahn.maakmai.MaakMaiArgs
import org.hahn.maakmai.data.BookmarkRepository
import org.hahn.maakmai.model.Bookmark
import java.util.UUID
import javax.inject.Inject
import androidx.core.net.toUri
import timber.log.Timber
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class AddEditBookmarkUiState(
    val title: String = "",
    val description: String = "",
    val url: String? = null,
    val tags: List<String> = listOf(),
    val isLoading: Boolean = false,
    val isBookmarkSaved: Boolean = false,
    val isBookmarkDeleted: Boolean = false,
    val isNew: Boolean = true
)

@HiltViewModel
class AddEditBookmarkViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {
    private val bookmarkId: UUID? = savedStateHandle.get<String?>(MaakMaiArgs.BOOKMARK_ID_ARG).let { id -> if (id.isNullOrBlank()) null else UUID.fromString(id) }
    private val path: String? = savedStateHandle[MaakMaiArgs.PATH_ARG]
    private val sharedUrl: String? = savedStateHandle.get<String?>(MaakMaiArgs.URL_ARG)?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
    private val sharedTitle: String? = savedStateHandle.get<String?>(MaakMaiArgs.BOOKMARK_TITLE_ARG)?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
    private val sharedSubject: String? = savedStateHandle.get<String?>(MaakMaiArgs.SUBJECT_ARG)?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }

    private val _uiState = MutableStateFlow(AddEditBookmarkUiState(
        tags = path?.split("/")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList(),
        isNew = bookmarkId == null
    ))
    val uiState = _uiState.asStateFlow();

    init {
        if (bookmarkId != null) {
            loadBookmark(bookmarkId)
        } else {
            // Handle shared URL if available
            processSharedContent()
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


    fun saveBookmark() {
        viewModelScope.launch {
            val bookmark = Bookmark(bookmarkId ?: UUID.randomUUID(), uiState.value.title, uiState.value.description, uiState.value.url, uiState.value.tags.filter { it.isNotBlank() })
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
}
