package org.hahn.maakmai.addeditbookmark

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                modifier = Modifier.fillMaxWidth()
            )
        },
        floatingActionButton = {
            SmallFloatingActionButton(onClick = viewModel::saveBookmark) {
                Icon(Icons.Filled.Done, "Save Bookmark")
            }
        }
    ) { paddingValues ->
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
    tags: List<String>,
    onTitleChanged: (String) -> Unit = {},
    onDescriptionChanged: (String) -> Unit = {},
    onUrlChanged: (String?) -> Unit = {},
    onTagsChanged: (List<String>) -> Unit = {},
    folders: List<TagFolder> = emptyList(),
    selectedFolderPath: List<TagFolder> = emptyList(),
    onFolderSelected: (TagFolder) -> Unit = {},
    onClearFolders: () -> Unit = {},
    showDelete: Boolean = false,
    onDeleteClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    onRemoveLastFolder: () -> Unit = {},
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
            Text(
                text = "Select Folders:",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            FolderBadgeSelector(
                folders = folders,
                selectedFolderPath = selectedFolderPath,
                onFolderSelected = onFolderSelected,
                onBackClicked = onRemoveLastFolder,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Simple tags implementation - comma-separated string
        val tagsString = tags.joinToString(", ")
        OutlinedTextField(
            value = tagsString,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            onValueChange = { newTagsString ->
                val newTags = newTagsString.split(",").map { it.trim() }
                onTagsChanged(newTags)
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
        if (selectedFolderPath.isNotEmpty()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .horizontalScroll(rememberScrollState())
            ) {
                IconButton(onClick = onBackClicked) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Go back")
                }

                selectedFolderPath.forEachIndexed { index, folder ->
                    if (index > 0) {
                        Text(" > ", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = folder.tag,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Show folder badges
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 8.dp)
        ) {
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

@Composable
@Preview
private fun AddEditBookmarkContentPreview() {
    MaterialTheme {
        Surface {
            AddEditBookmarkContent(
                title = "Awesome sweater",
                description = "This is a very nice sweater",
                url = "https://example.com/sweater",
                tags = listOf("clothing", "winter", "wool"),
            )
        }
    }
}
