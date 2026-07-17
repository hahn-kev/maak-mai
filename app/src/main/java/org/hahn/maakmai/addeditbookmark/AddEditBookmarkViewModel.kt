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
import org.hahn.maakmai.model.Attachment
import org.hahn.maakmai.model.Bookmark
import org.hahn.maakmai.model.TagFolder
import org.hahn.maakmai.util.OpenGraphEnricher
import org.hahn.maakmai.util.OpenGraphUtils
import org.hahn.maakmai.util.ShortLinkResolver
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
    // Only the share-capture flow opts in to OpenGraph enrichment; editing an
    // existing bookmark must never refetch and clobber its saved fields.
    private val shouldEnrich: Boolean = savedStateHandle.get<Boolean?>(MaakMaiArgs.ENRICH_ARG) ?: false

    // Tracks fields the user has manually edited so async enrichment never
    // overwrites them.
    private var titleEdited = false
    private var descriptionEdited = false
    private var imageEdited = false
    private var urlEdited = false

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
                // Load image attachment if it exists
                var imageUri: String? = null
                if (bookmark.imageAttachmentId != null) {
                    try {
                        // Create a content URI for the attachment
                        imageUri = "content://org.hahn.maakmai.attachment/${bookmark.imageAttachmentId}"
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

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

                // Kick off async resolution + OpenGraph enrichment for freshly-
                // captured shares. The URL is already set above and is never nulled
                // by this, so it is present immediately and saving mid-fetch is safe.
                if (shouldEnrich && bookmark.url != null) {
                    resolveAndEnrich(bookmark.url)
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
     * For a freshly-captured share: first resolve redirect shorteners (e.g.
     * share.google) to their real destination, then fetch OpenGraph metadata and
     * merge only the fields the user hasn't edited. All network runs off the main
     * thread. The URL is only ever *replaced* with a resolved destination, never
     * nulled, and a failed fetch leaves the captured URL and text-derived title
     * intact.
     */
    private fun resolveAndEnrich(capturedUrl: String) {
        viewModelScope.launch {
            // Resolve short links to their destination and reflect it in the URL
            // field, unless the user has already edited the URL.
            val resolvedUrl = if (ShortLinkResolver.isShortLink(capturedUrl)) {
                ShortLinkResolver.resolve(capturedUrl)
            } else {
                capturedUrl
            }
            if (resolvedUrl != capturedUrl) {
                if (!urlEdited) {
                    _uiState.update { it.copy(url = resolvedUrl) }
                }
                // The captured title may have been derived from the opaque short
                // link (e.g. the share.google code). If the user hasn't edited it,
                // re-derive from the resolved destination so it's meaningful even
                // when OpenGraph is unavailable. OG below can still improve on it.
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

            // Enrich from the URL that will actually be stored.
            val urlForMetadata = if (urlEdited) _uiState.value.url ?: resolvedUrl else resolvedUrl
            val openGraph = OpenGraphUtils.extractUrlOpenGraphMetadata(urlForMetadata)
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
        }
    }

    fun updateTitle(newTitle: String) {
        titleEdited = true
        _uiState.update {
            it.copy(title = newTitle)
        }
    }

    fun updateDescription(newDescription: String) {
        descriptionEdited = true
        _uiState.update {
            it.copy(description = newDescription)
        }
    }

    fun updateUrl(newUrl: String?) {
        urlEdited = true
        _uiState.update {
            it.copy(url = newUrl)
        }
    }

    fun updateTags(newTags: String) {
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
            // Process image attachment if present
            var imageAttachmentId: UUID? = null

            // Track image dimensions if we create or keep an image
            var imageWidth: Int? = null
            var imageHeight: Int? = null

            // Check if we're editing an existing bookmark
            if (bookmarkId != null) {
                // Get the existing bookmark to check for an existing image attachment
                val existingBookmark = bookmarkRepository.getBookmark(bookmarkId)
                val existingAttachmentId = existingBookmark?.imageAttachmentId
                imageWidth = existingBookmark?.imageWidth
                imageHeight = existingBookmark?.imageHeight

                // If the URI has changed and there was an existing attachment, delete it
                if (existingAttachmentId != null && 
                    uiState.value.selectedImageUri != "content://org.hahn.maakmai.attachment/$existingAttachmentId") {
                    try {
                        attachmentRepository.delete(existingAttachmentId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // If the URI hasn't changed and there was an existing attachment, keep using it
                if (existingAttachmentId != null && 
                    uiState.value.selectedImageUri == "content://org.hahn.maakmai.attachment/$existingAttachmentId") {
                    imageAttachmentId = existingAttachmentId
                }
            }

            // If we have a selected image URI and it's not already an attachment URI (or we need to create a new one)
            if (uiState.value.selectedImageUri != null && imageAttachmentId == null && 
                !uiState.value.selectedImageUri!!.startsWith("content://org.hahn.maakmai.attachment/")) {
                try {
                    val uri = Uri.parse(uiState.value.selectedImageUri)

                    // Use helper to load image bytes and dimensions
                    val loaded = loadImageBytesAndSize(uri)
                    if (loaded != null) {
                        val (imageData, w, h) = loaded
                        imageWidth = w
                        imageHeight = h

                        // Create a new attachment with the image data
                        val attachmentId = UUID.randomUUID()
                        val attachment = Attachment(
                            id = attachmentId,
                            data = imageData,
                            title = "Image for ${uiState.value.title}"
                        )
                        // Save the attachment
                        attachmentRepository.create(attachment)
                        imageAttachmentId = attachmentId
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Continue without the image if there's an error
                }
            }

            val folderTags = uiState.value.selectedFolderPath.map { it.tag }
            val priorityTags = uiState.value.tagsPrioritised.filter { it.isSelected }.map { it.tag }
            val selectedFolderTags = uiState.value.groupedFolderTags.map { group ->
                group.tags.filter { it.isSelected }.map { it.tag }
            }.flatten()
            val rawTags = uiState.value.tags.split(",").map {it.trim()} .filter { it.isNotBlank() }
            val bookmark =
                Bookmark(
                    bookmarkId ?: UUID.randomUUID(),
                    uiState.value.title,
                    uiState.value.description,
                    uiState.value.url,
                    (rawTags + folderTags + priorityTags + selectedFolderTags).distinct(),
                    imageAttachmentId,
                    imageWidth,
                    imageHeight
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

    /**
     * Updates the selected image URI
     * @param uri The URI of the selected image
     */
    fun updateSelectedImageUri(uri: String?) {
        imageEdited = true
        _uiState.update {
            it.copy(selectedImageUri = uri)
        }
    }

    /**
     * Loads an image via Coil and returns its compressed bytes and dimensions.
     * @param uri The source image URI.
     * @return Triple<bytes, width, height> or null if loading/conversion fails.
     */
    private suspend fun loadImageBytesAndSize(uri: Uri): Triple<ByteArray, Int, Int>? = withContext(Dispatchers.IO) {
        try {
            val imageLoader = SingletonImageLoader.get(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .build()
            val result = imageLoader.execute(request)
            val image = result.image
            if (image is BitmapImage) {
                val bitmap = image.bitmap
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(android.graphics.Bitmap.CompressFormat.WEBP_LOSSY, 90, outputStream)
                val bytes = outputStream.toByteArray()
                Triple(bytes, bitmap.width, bitmap.height)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
