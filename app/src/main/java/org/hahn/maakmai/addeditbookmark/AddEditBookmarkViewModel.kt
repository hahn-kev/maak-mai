package org.hahn.maakmai.addeditbookmark

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.BitmapImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.request.allowRgb565
import coil3.size.Precision
import coil3.size.Scale
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hahn.maakmai.MaakMaiArgs
import org.hahn.maakmai.data.AttachmentRepository
import org.hahn.maakmai.data.BookmarkRepository
import org.hahn.maakmai.data.FolderRepository
import org.hahn.maakmai.images.AttachmentImages
import org.hahn.maakmai.images.EncodedImage
import org.hahn.maakmai.images.MAX_IMAGE_DIMENSION
import org.hahn.maakmai.images.shrinkAndEncode
import org.hahn.maakmai.model.Attachment
import org.hahn.maakmai.model.Bookmark
import org.hahn.maakmai.model.TagFolder
import org.hahn.maakmai.util.OpenGraphEnricher
import org.hahn.maakmai.util.OpenGraphUtils
import org.hahn.maakmai.util.ShortLinks
import org.hahn.maakmai.util.UrlTitleExtractor
import java.io.ByteArrayOutputStream
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
    val tags: String = "",
    val isLoading: Boolean = false,
    val isEnriching: Boolean = false,
    val isBookmarkSaved: Boolean = false,
    val isBookmarkDeleted: Boolean = false,
    val isNew: Boolean = true,
    val selectedFolderPath: List<TagFolder> = listOf(),
    val folders: List<TagFolder> = listOf(),
    val tagsPrioritised: List<TagUiState> = listOf(),
    val groupedFolderTags: List<TagGroup> = listOf(),
    val selectedImageUri: String? = null
)

data class TagUiState(val tag: String, val isSelected: Boolean = false, val label: String? = null)

@HiltViewModel
class AddEditBookmarkViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val bookmarkRepository: BookmarkRepository,
    private val folderRepository: FolderRepository,
    private val attachmentRepository: AttachmentRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val bookmarkId: UUID? = savedStateHandle.get<String?>(MaakMaiArgs.BOOKMARK_ID_ARG).let { id -> if (id.isNullOrBlank()) null else UUID.fromString(id) }
    private val path: String? = savedStateHandle[MaakMaiArgs.PATH_ARG]
    // True when the screen was opened from a share action (see shareCaptureRoute).
    // A share capture both enriches via OpenGraph and auto-saves edits, so the user
    // never has to tap Save; editing an existing bookmark does neither.
    private val isShareCapture: Boolean = savedStateHandle.get<Boolean?>(MaakMaiArgs.SHARE_CAPTURE_ARG) ?: false

    // Tracks fields the user has manually edited so async enrichment never
    // overwrites them.
    private var titleEdited = false
    private var descriptionEdited = false
    private var imageEdited = false
    private var urlEdited = false

    // Debounces auto-save writes so only the latest change is persisted.
    private var autoSaveJob: Job? = null

    private val _uiState = MutableStateFlow(
        AddEditBookmarkUiState(
            isNew = bookmarkId == null
        )
    )
    val uiState = _uiState.asStateFlow()


    init {
        // Shared links are auto-captured and persisted before this screen opens, so
        // the Add screen is always an edit of an existing bookmark id (or a blank new
        // one when adding manually). See SharedBookmarkFactory / ShareUrlActivity.
        if (bookmarkId != null) {
            loadBookmark(bookmarkId)
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

    private fun loadBookmark(bookmarkId: UUID) {
        _uiState.update {
            it.copy(
                isLoading = true
            )
        }
        viewModelScope.launch {
            val bookmark = bookmarkRepository.getBookmark(bookmarkId)
            if (bookmark != null) {
                val imageUri = bookmark.imageAttachmentId?.let(AttachmentImages::uriFor)

                _uiState.update {
                    it.copy(
                        title = bookmark.title,
                        description = bookmark.description,
                        url = bookmark.url,
                        tags = bookmark.tags.joinToString(", "),
                        selectedImageUri = imageUri,
                        isLoading = false
                    )
                }

                // Kick off async OpenGraph enrichment (which also resolves redirect
                // shorteners) for freshly-captured shares. The URL is already set
                // above and is never nulled by this, so it is present immediately and
                // saving mid-fetch is safe.
                if (isShareCapture && bookmark.url != null) {
                    enrichCapturedShare(bookmark.url)
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

    /**
     * For a freshly-captured share: fetch OpenGraph metadata (a single request that
     * also follows redirects) and merge only the fields the user hasn't edited. For
     * known redirect shorteners (e.g. share.google) the URL the fetch landed on is
     * adopted as the real destination — no separate resolution round-trip needed.
     *
     * All network runs off the main thread. The URL is only ever *replaced* with a
     * resolved destination, never nulled, and a failed fetch leaves the captured URL
     * and text-derived title intact. `isEnriching` gates a progress indicator.
     */
    private fun enrichCapturedShare(capturedUrl: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEnriching = true) }
            try {
                val openGraph = OpenGraphUtils.extractUrlOpenGraphMetadata(capturedUrl)

                // Adopt the resolved destination for known short links only, so
                // ordinary links are never rewritten by an incidental redirect.
                val resolvedUrl = openGraph.finalUrl
                    ?.takeIf { it.isNotBlank() && ShortLinks.isShortLink(capturedUrl) }
                    ?: capturedUrl
                if (resolvedUrl != capturedUrl) {
                    if (!urlEdited) {
                        _uiState.update { it.copy(url = resolvedUrl) }
                    }
                    // The captured title may have been derived from the opaque short
                    // link (e.g. the share.google code). If the user hasn't edited it,
                    // re-derive from the resolved destination so it's meaningful even
                    // when OpenGraph is unavailable.
                    if (!titleEdited) {
                        val shortLinkTitle = UrlTitleExtractor.fromUrl(capturedUrl)
                        _uiState.update { state ->
                            if (state.title == shortLinkTitle) {
                                state.copy(title = UrlTitleExtractor.fromUrl(resolvedUrl))
                            } else {
                                state
                            }
                        }
                    }
                }

                _uiState.update { state ->
                    val enriched = OpenGraphEnricher.enrich(
                        current = OpenGraphEnricher.Fields(
                            title = state.title,
                            description = state.description,
                            imageUri = state.selectedImageUri
                        ),
                        ogTitle = openGraph.title,
                        ogDescription = openGraph.description,
                        ogImage = openGraph.image,
                        edited = OpenGraphEnricher.Edited(
                            title = titleEdited,
                            description = descriptionEdited,
                            image = imageEdited
                        )
                    )
                    state.copy(
                        title = enriched.title,
                        description = enriched.description,
                        selectedImageUri = enriched.imageUri
                    )
                }

                // Persist the enriched data (resolved URL, title, description,
                // image) so it survives even if the user backs out without saving.
                scheduleAutoSave()
            } finally {
                _uiState.update { it.copy(isEnriching = false) }
            }
        }
    }

    fun updateTitle(newTitle: String) {
        titleEdited = true
        _uiState.update {
            it.copy(title = newTitle)
        }
        scheduleAutoSave()
    }

    fun updateDescription(newDescription: String) {
        descriptionEdited = true
        _uiState.update {
            it.copy(description = newDescription)
        }
        scheduleAutoSave()
    }

    fun updateUrl(newUrl: String?) {
        urlEdited = true
        _uiState.update {
            it.copy(url = newUrl)
        }
        scheduleAutoSave()
    }

    fun updateTags(newTags: String) {
        _uiState.update {
            it.copy(tags = newTags)
        }
        scheduleAutoSave()
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
        scheduleAutoSave()
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
        scheduleAutoSave()
    }

    fun removeLastSelectedFolder() {
        val currentPath = _uiState.value.selectedFolderPath
        if (currentPath.isNotEmpty()) {
            _uiState.update {
                it.copy(selectedFolderPath = currentPath.dropLast(1))
            }
            updateFolderTags()
            scheduleAutoSave()
        }
    }

    /**
     * Debounced auto-save for the share-capture flow: persists the current edits
     * (and enriched data) without navigating away, so backing out or closing never
     * loses changes. Only the latest change is written — a new edit cancels the
     * pending write. No-op when the screen wasn't opened from a share.
     */
    private fun scheduleAutoSave() {
        if (!isShareCapture) return
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(AUTO_SAVE_DEBOUNCE_MS)
            writeBookmark()
        }
    }

    /**
     * Persists any pending auto-save immediately. Called when the screen stops so
     * an edit made within the debounce window still survives an abrupt exit.
     */
    fun flushAutoSave() {
        if (!isShareCapture) return
        autoSaveJob?.cancel()
        viewModelScope.launch { writeBookmark() }
    }

    fun saveBookmark() {
        viewModelScope.launch {
            autoSaveJob?.cancel()
            writeBookmark()
            _uiState.update {
                it.copy(
                    isBookmarkSaved = true
                )
            }
        }
    }

    /**
     * Writes the current UI state to the repository without any navigation side
     * effects. Shared by explicit Save and auto-save. Creating an image attachment
     * repoints [AddEditBookmarkUiState.selectedImageUri] at the stored attachment so
     * repeated auto-saves don't recreate it.
     */
    private suspend fun writeBookmark() {
            var createdAt = System.currentTimeMillis()
            val existingBookmark = bookmarkId?.let { bookmarkRepository.getBookmark(it) }
            if (existingBookmark != null) {
                createdAt = existingBookmark.createdAt
            }
            val image = resolveImage(existingBookmark)

            val folderTags = uiState.value.selectedFolderPath.map { it.tag }
            val priorityTags = uiState.value.tagsPrioritised.filter { it.isSelected }.map { it.tag }
            val selectedFolderTags = uiState.value.groupedFolderTags.map { group ->
                group.tags.filter { it.isSelected }.map { it.tag }
            }.flatten()
            val rawTags = uiState.value.tags.split(",").map {it.trim()} .filter { it.isNotBlank() }
            val bookmark =
                Bookmark(
                    id = bookmarkId ?: UUID.randomUUID(),
                    title = uiState.value.title,
                    description = uiState.value.description,
                    url = uiState.value.url,
                    tags = (rawTags + folderTags + priorityTags + selectedFolderTags).distinct(),
                    imageAttachmentId = image?.id,
                    imageWidth = image?.width,
                    imageHeight = image?.height,
                    createdAt = createdAt
                )
            if (bookmarkId == null) {
                bookmarkRepository.createBookmark(bookmark)
            } else {
                bookmarkRepository.updateBookmark(bookmark)
            }

            // Only drop the old image once the bookmark points at its replacement
            val oldAttachmentId = existingBookmark?.imageAttachmentId
            if (oldAttachmentId != null && oldAttachmentId != image?.id) {
                attachmentRepository.delete(oldAttachmentId)
            }
    }

    private data class StoredImage(val id: UUID, val width: Int?, val height: Int?)

    // The attachment most recently created by this screen, so repeated saves reuse it
    private var createdImage: StoredImage? = null

    /**
     * Works out which stored image the bookmark should point at, storing a newly
     * picked or fetched image first. If a new image can't be loaded, the existing one is kept.
     */
    private suspend fun resolveImage(existing: Bookmark?): StoredImage? {
        val selected = uiState.value.selectedImageUri ?: return null
        val existingImage = existing?.imageAttachmentId?.let {
            StoredImage(it, existing.imageWidth, existing.imageHeight)
        }

        val storedId = AttachmentImages.idFrom(selected)
        if (storedId != null) {
            return listOfNotNull(existingImage, createdImage).firstOrNull { it.id == storedId }
                ?: StoredImage(storedId, null, null)
        }

        val encoded = loadAndShrink(Uri.parse(selected)) ?: return existingImage
        val attachmentId = UUID.randomUUID()
        attachmentRepository.create(
            Attachment(id = attachmentId, data = encoded.bytes, title = "Image for ${uiState.value.title}")
        )
        val stored = StoredImage(attachmentId, encoded.width, encoded.height)
        createdImage = stored
        // Repoint the UI at the stored attachment so a later auto-save reuses it
        _uiState.update { it.copy(selectedImageUri = AttachmentImages.uriFor(attachmentId)) }
        return stored
    }

    fun deleteBookmark() {
        if (bookmarkId == null) {
            return
        }

        viewModelScope.launch {
            autoSaveJob?.cancel()
            val attachmentId = bookmarkRepository.getBookmark(bookmarkId)?.imageAttachmentId
            bookmarkRepository.deleteBookmark(bookmarkId)
            attachmentId?.let { attachmentRepository.delete(it) }
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
        scheduleAutoSave()
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
        scheduleAutoSave()
    }

    /**
     * Updates the selected image URI
     * @param uri The URI of the selected image
     */
    fun updateSelectedImageUri(uri: String?) {
        imageEdited = true
        _uiState.update {
            it.copy(selectedImageUri = uri)
        }
        scheduleAutoSave()
    }

    /**
     * Loads an image via Coil, scaled down to [MAX_IMAGE_DIMENSION], and encodes it for storage.
     * Returns null if the image can't be loaded.
     */
    private suspend fun loadAndShrink(uri: Uri): EncodedImage? = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(context)
                .data(uri)
                // Without a size Coil decodes at full resolution. Inexact lets it decode
                // cheaply at up to twice the size; shrinkAndEncode then scales exactly.
                .size(MAX_IMAGE_DIMENSION)
                .precision(Precision.INEXACT)
                .allowHardware(false)
                .build()
            val image = SingletonImageLoader.get(context).execute(request).image
            (image as? BitmapImage)?.let { shrinkAndEncode(it.bitmap) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private companion object {
        const val AUTO_SAVE_DEBOUNCE_MS = 500L
    }
}
