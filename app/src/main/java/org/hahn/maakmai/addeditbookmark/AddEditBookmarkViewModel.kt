package org.hahn.maakmai.addeditbookmark

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

data class AddEditBookmarkUiState(
    val title: String = "",
    val description: String = "",
    val url: String? = null,
    val tags: List<String> = listOf(),
    val isLoading: Boolean = false,
    val isBookmarkSaved: Boolean = false
)

@HiltViewModel
class AddEditBookmarkViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookmarkRepository: BookmarkRepository
) : ViewModel() {
    private val bookmarkId: UUID? = savedStateHandle[MaakMaiArgs.BOOKMARK_ID_ARG]

    private val _uiState = MutableStateFlow(AddEditBookmarkUiState())
    val uiState = _uiState.asStateFlow();

    init {
        if (bookmarkId != null) {
            loadBookmark(bookmarkId)
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

    fun saveBookmark() {
        viewModelScope.launch {
            val bookmark = Bookmark(bookmarkId ?: UUID.randomUUID(), uiState.value.title, uiState.value.description, uiState.value.url, uiState.value.tags)
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
}