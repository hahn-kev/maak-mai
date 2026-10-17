package org.hahn.maakmai.bulkadd

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.hahn.maakmai.MaakMaiArgs
import org.hahn.maakmai.data.AttachmentRepository
import org.hahn.maakmai.addeditbookmark.TagGroup
import org.hahn.maakmai.addeditbookmark.TagSelection
import org.hahn.maakmai.addeditbookmark.TagUiState
import org.hahn.maakmai.data.BookmarkRepository
import org.hahn.maakmai.data.FolderRepository
import org.hahn.maakmai.images.loadAndShrink
import org.hahn.maakmai.model.Attachment
import org.hahn.maakmai.model.Bookmark
import org.hahn.maakmai.model.TagFolder
import org.hahn.maakmai.util.LinkPreview
import org.hahn.maakmai.util.LinkTextParser
import java.util.UUID
import javax.inject.Inject

data class BulkLinkItem(
    /** The link as found in the pasted text. Identifies the item in the list. */
    val sourceUrl: String,
    /** The fetched preview, or null while the fetch runs. */
    val preview: LinkPreview? = null,
    val isChecked: Boolean = false
) {
    val isLoading: Boolean get() = preview == null
    val error: String? get() = preview?.error
}

data class BulkAddUiState(
    val text: String = "",
    /** The parsed links, or null before the text is processed. */
    val items: List<BulkLinkItem>? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    // Tags given to every added link, picked as on the Add screen
    val tags: String = "",
    val folders: List<TagFolder> = listOf(),
    val selectedFolderPath: List<TagFolder> = listOf(),
    val tagsPrioritised: List<TagUiState> = listOf(),
    val groupedFolderTags: List<TagGroup> = listOf()
) {
    val isProcessing: Boolean get() = items?.any { it.isLoading } == true
    val checkedCount: Int get() = items?.count { it.isChecked } ?: 0
}

/** Shows links that fetched first and links that failed last, keeping each group in pasted order. */
fun displayOrder(items: List<BulkLinkItem>): List<BulkLinkItem> = items.sortedBy { it.error != null }

@HiltViewModel
class BulkAddViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookmarkRepository: BookmarkRepository,
    private val folderRepository: FolderRepository,
    private val attachmentRepository: AttachmentRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    // The folder bulk add was opened from starts out selected, as on the Add screen
    private val path: String? = savedStateHandle[MaakMaiArgs.PATH_ARG]

    private val _uiState = MutableStateFlow(BulkAddUiState())
    val uiState = _uiState.asStateFlow()

    private var processJob: Job? = null

    init {
        viewModelScope.launch {
            val tags = TagSelection.prioritisedTags(bookmarkRepository.getTagsWithCount())
            _uiState.update { it.copy(tagsPrioritised = tags) }
            // Only the first list sets the folder path, so a later folder change keeps the user's pick
            val folders = folderRepository.getFoldersStream().first()
            _uiState.update {
                it.copy(folders = folders, selectedFolderPath = TagSelection.folderPathFor(folders, path))
            }
            updateFolderTags()
        }
    }

    fun updateTags(tags: String) {
        _uiState.update { it.copy(tags = tags) }
    }

    fun selectFolder(folder: TagFolder) {
        _uiState.update { it.copy(selectedFolderPath = TagSelection.selectFolder(it.selectedFolderPath, folder)) }
        updateFolderTags()
    }

    fun removeLastSelectedFolder() {
        _uiState.update { it.copy(selectedFolderPath = it.selectedFolderPath.dropLast(1)) }
        updateFolderTags()
    }

    private fun updateFolderTags() {
        _uiState.update {
            it.copy(groupedFolderTags = TagSelection.groupFolderTags(it.selectedFolderPath, it.tagsPrioritised.map { tag -> tag.tag }))
        }
    }

    fun togglePriorityTag(tag: TagUiState) {
        _uiState.update { it.copy(tagsPrioritised = TagSelection.togglePriorityTag(it.tagsPrioritised, tag)) }
    }

    fun toggleFolderTag(group: TagGroup, tag: TagUiState) {
        _uiState.update { it.copy(groupedFolderTags = TagSelection.toggleFolderTag(it.groupedFolderTags, group, tag)) }
    }

    fun updateText(text: String) {
        _uiState.update { it.copy(text = text) }
    }

    /** Finds the links in the text and fetches a preview for each, a few at a time. */
    fun process() {
        processJob?.cancel()
        val urls = LinkTextParser.extractUrls(_uiState.value.text)
        _uiState.update { state -> state.copy(items = urls.map { BulkLinkItem(sourceUrl = it) }) }
        val permits = Semaphore(MAX_PARALLEL_FETCHES)
        processJob = viewModelScope.launch {
            urls.forEach { url ->
                launch {
                    val preview = permits.withPermit { LinkPreview.fetch(url) }
                    // Links that failed stay in the list but are not added unless the user checks them
                    updateItem(url) { it.copy(preview = preview, isChecked = preview.error == null) }
                }
            }
        }
    }

    /** Goes back to the text so the user can change it and process again. */
    fun editText() {
        processJob?.cancel()
        _uiState.update { it.copy(items = null) }
    }

    fun setChecked(sourceUrl: String, checked: Boolean) {
        updateItem(sourceUrl) { it.copy(isChecked = checked) }
    }

    private fun updateItem(sourceUrl: String, transform: (BulkLinkItem) -> BulkLinkItem) {
        _uiState.update { state ->
            state.copy(items = state.items?.map { if (it.sourceUrl == sourceUrl) transform(it) else it })
        }
    }

    fun addChecked() {
        val checked = _uiState.value.items
            ?.filter { it.isChecked }
            ?.mapNotNull { it.preview }
            ?: return
        if (checked.isEmpty() || _uiState.value.isSaving) return
        _uiState.update { it.copy(isSaving = true) }
        val state = _uiState.value
        val tags = TagSelection.selectedTags(
            typedTags = state.tags,
            path = state.selectedFolderPath,
            priorityTags = state.tagsPrioritised,
            groupedFolderTags = state.groupedFolderTags
        )
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            checked.forEachIndexed { index, preview ->
                // Step back one millisecond per link so "newest first" keeps the pasted order
                save(preview, tags, createdAt = now - index)
            }
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
    }

    private suspend fun save(preview: LinkPreview, tags: List<String>, createdAt: Long) {
        val image = preview.imageUrl?.let { loadAndShrink(context, Uri.parse(it)) }
        val imageId = image?.let {
            val id = UUID.randomUUID()
            attachmentRepository.create(Attachment(id = id, data = it.bytes, title = "Image for ${preview.title}"))
            id
        }
        bookmarkRepository.createBookmark(
            Bookmark(
                id = UUID.randomUUID(),
                title = preview.title,
                description = preview.description.takeIf { it != preview.title } ?: "",
                url = preview.url,
                tags = tags,
                imageAttachmentId = imageId,
                imageWidth = image?.width,
                imageHeight = image?.height,
                createdAt = createdAt
            )
        )
    }

    private companion object {
        const val MAX_PARALLEL_FETCHES = 4
    }
}
