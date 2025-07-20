package org.hahn.maakmai.addeditbookmark

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import org.hahn.maakmai.model.TagFolder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBookmarkScreen(
    topBarTitle: String,
    modifier: Modifier = Modifier,
    onBookmarkUpdate: () -> Unit = {},
    onBookmarkDelete: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: AddEditBookmarkViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = topBarTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, "Cancel")
                    }
                },
                actions = {
                    if (!uiState.isNew) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, "Delete Bookmark")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(onClick = viewModel::saveBookmark) {
                Icon(Icons.Filled.Done, "Save Bookmark")
            }
        }
    ) { paddingValues ->

        AddEditBookmarkContent(
            title = uiState.title,
            description = uiState.description,
            url = uiState.url,
            tags = uiState.tags,
            folders = uiState.folders,
            selectedFolderPath = uiState.selectedFolderPath,
            showDelete = !uiState.isNew,
            onTitleChanged = viewModel::updateTitle,
            onDescriptionChanged = viewModel::updateDescription,
            onUrlChanged = viewModel::updateUrl,
            onTagsChanged = viewModel::updateTags,
            onFolderSelected = viewModel::selectFolder,
            onClearFolders = viewModel::clearSelectedFolders,
            onRemoveLastFolder = viewModel::removeLastSelectedFolder,
            priorityTags = uiState.tagsPrioritised,
            onPriorityTagToggled = viewModel::togglePriorityTag,
            onFolderTagToggled = viewModel::toggleFolderTag,
            groupedFolderTags = uiState.groupedFolderTags,
            selectedImageUri = uiState.selectedImageUri,
            onImageSelected = viewModel::updateSelectedImageUri,
            onDeleteClick = { showDeleteConfirmation = true },
            modifier = Modifier.padding(paddingValues)
        )

        // Delete confirmation dialog
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Bookmark") },
                text = { Text("Are you sure you want to delete this bookmark?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            viewModel.deleteBookmark()
                        }
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        LaunchedEffect(uiState.isBookmarkSaved) {
            if (uiState.isBookmarkSaved) {
                onBookmarkUpdate()
            }
        }

        LaunchedEffect(uiState.isBookmarkDeleted) {
            if (uiState.isBookmarkDeleted) {
                onBookmarkDelete()
            }
        }
    }
}

@Composable
private fun AddEditBookmarkContent(
    title: String,
    description: String,
    url: String?,
    tags: String,
    onTitleChanged: (String) -> Unit = {},
    onDescriptionChanged: (String) -> Unit = {},
    onUrlChanged: (String?) -> Unit = {},
    onTagsChanged: (String) -> Unit = {},
    folders: List<TagFolder> = emptyList(),
    selectedFolderPath: List<TagFolder> = emptyList(),
    onFolderSelected: (TagFolder) -> Unit = {},
    onClearFolders: () -> Unit = {},
    showDelete: Boolean = false,
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onRemoveLastFolder: () -> Unit = {},
    priorityTags: List<TagUiState> = emptyList(),
    onPriorityTagToggled: (TagUiState) -> Unit = {},
    onFolderTagToggled: (TagGroup, TagUiState) -> Unit = { _, _ -> },
    groupedFolderTags: List<TagGroup> = emptyList(),
    selectedImageUri: String? = null,
    onImageSelected: (String?) -> Unit = {},
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier
            .fillMaxWidth()
            .padding(all = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
        )
        OutlinedTextField(
            value = title,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onTitleChanged,
            label = { Text(text = "Title") },
            textStyle = MaterialTheme.typography.headlineSmall
                .copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            colors = textFieldColors,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        OutlinedTextField(
            value = description,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            onValueChange = onDescriptionChanged,
            label = { Text(text = "Description") },
            textStyle = MaterialTheme.typography.bodyLarge,
            minLines = 3,
            colors = textFieldColors,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        OutlinedTextField(
            value = url ?: "",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            onValueChange = { onUrlChanged(it.ifEmpty { null }) },
            label = { Text(text = "URL") },
            textStyle = MaterialTheme.typography.bodyLarge,
            singleLine = true,
            colors = textFieldColors,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Uri
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        // Folder badge selector
        if (folders.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            FolderBadgeSelector(
                folders = folders,
                selectedFolderPath = selectedFolderPath,
                onFolderSelected = onFolderSelected,
                onBackClicked = onRemoveLastFolder,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Folder tags selector
        if (groupedFolderTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            FolderTagSelector(
                onFolderTagToggled = onFolderTagToggled,
                groupedFolderTags = groupedFolderTags,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Priority tags selector
        if (priorityTags.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            TagSection(
                tags = priorityTags,
                onTagToggled = onPriorityTagToggled,
                modifier = Modifier.fillMaxWidth(),
                sectionTitle = "Priority Tags:"
            )
        }

        // Image picker and preview
        Spacer(modifier = Modifier.height(16.dp))
        ImagePickerAndPreview(
            selectedImageUri = selectedImageUri,
            onImageSelected = onImageSelected,
            modifier = Modifier.fillMaxWidth()
        )

        // Simple tags implementation - comma-separated string
        OutlinedTextField(
            value = tags,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            onValueChange = { newTagsString ->
                onTagsChanged(newTagsString)
            },
            label = { Text(text = "Tags (comma-separated)") },
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = textFieldColors,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )

        if (showDelete) {
            Button(
                onClick = onDeleteClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Delete Bookmark")
            }
        }
    }
}

@Composable
private fun FolderBadgeSelector(
    folders: List<TagFolder>,
    selectedFolderPath: List<TagFolder>,
    onFolderSelected: (TagFolder) -> Unit,
    onBackClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Show navigation path
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            Text(
                text = "Select Folders:",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )

        }


        // Show folder badges
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp)
        ) {
            selectedFolderPath.forEachIndexed { index, folder ->
                val last = index == selectedFolderPath.lastIndex
                if (index > 0) {
                    Text(" / ", style = MaterialTheme.typography.bodyMedium)
                }
                if (!last) {
                    FilterChip(
                        selected = true,
                        enabled = false,
                        onClick = { },
                        label = { Text(folder.tag) },
                    )
                } else {
                    FilterChip(
                        selected = true,
                        onClick = { onBackClicked() },
                        label = { Text(folder.tag) },
                        trailingIcon = { Icon(Icons.Default.Close, "Back") }
                    )
                }
            }

            Text(" / ", style = MaterialTheme.typography.bodyMedium)

            // Get the current level folders
            val currentFolders = if (selectedFolderPath.isEmpty()) {
                // If no folder is selected, show root folders
                folders.filter { it.rootFolder }
            } else {
                // Otherwise, show children of the last selected folder
                selectedFolderPath.last().children
            }

            currentFolders.forEach { folder ->
                FilterChip(
                    selected = selectedFolderPath.any { it.id == folder.id },
                    onClick = { onFolderSelected(folder) },
                    label = { Text(folder.tag) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagSection(
    tags: List<TagUiState>,
    onTagToggled: (TagUiState) -> Unit,
    modifier: Modifier = Modifier,
    sectionTitle: String,
    titleStyle: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    Column(modifier = modifier) {
        Text(
            text = sectionTitle,
            style = titleStyle,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Show priority tags
        FlowRow(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.forEach { tag ->
                FilterChip(
                    selected = tag.isSelected,
                    onClick = { onTagToggled(tag) },
                    label = {
                        Text(tag.label ?: tag.tag)
                    }
                )
            }
        }
    }
}

@Composable
private fun FolderTagSelector(
    onFolderTagToggled: (TagGroup, TagUiState) -> Unit,
    modifier: Modifier = Modifier,
    groupedFolderTags: List<TagGroup> = emptyList()
) {
    Column(modifier = modifier) {
        Text(
            text = "Folder Tags:",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        groupedFolderTags.forEach { group ->
            TagSection(
                tags = group.tags,
                onTagToggled = { onFolderTagToggled(group, it) },
                modifier = Modifier.fillMaxWidth(),
                sectionTitle = group.prefix,
                titleStyle = MaterialTheme.typography.bodyMedium
            )
        }

    }
}

@Composable
private fun ImagePickerAndPreview(
    selectedImageUri: String?,
    onImageSelected: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null)
            onImageSelected(uri.toString())
    }

    Column(modifier = modifier) {
        Text(
            text = "Image",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Image preview or placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .border(
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    shape = RoundedCornerShape(8.dp)
                )
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable { imagePicker.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            if (selectedImageUri != null) {
                val painter = rememberAsyncImagePainter(
                    ImageRequest.Builder(context)
                        .data(selectedImageUri)
                        .crossfade(true)
                        .build()
                )
                val state by painter.state.collectAsState()
                when (state) {
                    is AsyncImagePainter.State.Loading,
                    is AsyncImagePainter.State.Empty -> {
                        CircularProgressIndicator()
                    }

                    is AsyncImagePainter.State.Success -> {
                        Image(
                            painter = painter,
                            contentDescription = "Selected image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    is AsyncImagePainter.State.Error -> {
                        // Display error message
                        Text(
                            text = "Error loading image",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

            } else {
                // Display placeholder
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "Add image",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to select an image",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Button to clear selected image
        if (selectedImageUri != null) {
            TextButton(
                onClick = { onImageSelected(null) },
                modifier = Modifier.align(Alignment.End)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear image",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear image")
            }
        }
    }
}

@Composable
@Preview
private fun AddEditBookmarkContentPreview() {
    MaterialTheme {
        Surface {
            AddEditBookmarkContent(
                title = "Awesome sweater",
                description = "This is a very nice sweater",
                url = "https://example.com/sweater",
                tags = "clothing, winter, wool",
                priorityTags = listOf(
                    TagUiState(
                        tag = "clothing",
                        label = "Clothing",
                        isSelected = true
                    ),
                    TagUiState(
                        tag = "winter",
                        label = "Winter",
                        isSelected = true
                    ),
                    TagUiState(
                        tag = "clothing",
                        label = "Clothing",
                        isSelected = true
                    ),
                    TagUiState(
                        tag = "winter",
                        label = "Winter",
                        isSelected = true
                    ),
                    TagUiState(
                        tag = "clothing",
                        label = "Clothing",
                        isSelected = true
                    ),
                    TagUiState(
                        tag = "winter",
                        label = "Winter",
                        isSelected = true
                    )
                )
            )
        }
    }
}
