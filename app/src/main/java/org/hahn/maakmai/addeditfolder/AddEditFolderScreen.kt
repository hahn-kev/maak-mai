package org.hahn.maakmai.addeditfolder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import org.hahn.maakmai.ui.theme.FolderColors
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import org.hahn.maakmai.model.TagFolder
import java.util.UUID
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import okhttp3.internal.toHexString
import org.hahn.maakmai.ui.theme.DefaultFolderColorStr

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditFolderScreen(
    topBarTitle: String,
    modifier: Modifier = Modifier,
    onFolderUpdate: () -> Unit = {},
    onFolderDelete: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: AddEditFolderViewModel = hiltViewModel(),
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
            SmallFloatingActionButton(onClick = viewModel::saveFolder) {
                Icon(Icons.Filled.Done, "Save Folder")
            }
        }
    ) { paddingValues ->
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        AddEditFolderContent(
            tag = uiState.tag,
            parentPath = uiState.parentPath,
            childFolders = uiState.childFolders,
            tagGroups = uiState.tagGroups,
            color = uiState.color,
            showDelete = !uiState.isNew,
            onTagChanged = viewModel::updateTag,
            onTagGroupsChanged = viewModel::updateTagGroups,
            onColorChanged = viewModel::updateColor,
            onDeleteClick = { showDeleteConfirmation = true },
            modifier = Modifier.padding(paddingValues)
        )

        // Delete confirmation dialog
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                title = { Text("Delete Folder") },
                text = { 
                    Text(
                        if (uiState.childFolders.isEmpty()) 
                            "Are you sure you want to delete this folder?" 
                        else 
                            "Are you sure you want to delete this folder and its ${uiState.childFolders.size} child folders?"
                    ) 
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteConfirmation = false
                            viewModel.deleteFolder()
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

        LaunchedEffect(uiState.isFolderSaved) {
            if (uiState.isFolderSaved) {
                onFolderUpdate()
            }
        }

        LaunchedEffect(uiState.isFolderDeleted) {
            if (uiState.isFolderDeleted) {
                onFolderDelete()
            }
        }
    }
}

@Composable
private fun AddEditFolderContent(
    tag: String,
    parentPath: String,
    modifier: Modifier = Modifier,
    showDelete: Boolean = true,
    childFolders: List<TagFolder> = emptyList(),
    tagGroups: String = "",
    color: String = DefaultFolderColorStr,
    onTagChanged: (String) -> Unit = {},
    onTagGroupsChanged: (String) -> Unit = {},
    onColorChanged: (String) -> Unit = {},
    onDeleteClick: () -> Unit = {},
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

        // Display parent path (read-only)
        Text(
            text = "Parent Folder: $parentPath",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Folder name field
        OutlinedTextField(
            value = tag,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onTagChanged,
            label = { Text(text = "Folder Name") },
            textStyle = MaterialTheme.typography.headlineSmall
                .copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            colors = textFieldColors,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )

        // Tag groups field (comma-separated)
        OutlinedTextField(
            value = tagGroups,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            onValueChange = onTagGroupsChanged,
            label = { Text(text = "Tag Groups (comma-separated)") },
            textStyle = MaterialTheme.typography.bodyLarge,
            colors = textFieldColors,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { focusManager.clearFocus() }
            )
        )

        // Color picker
        ColorPicker(
            selectedColor = color,
            onColorSelected = onColorChanged,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
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
                Text("Delete Folder")
            }

        }
        // Child folders section
        if (childFolders.isNotEmpty()) {
            Text(
                text = "Child Folders (will also be deleted):",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                ChildFolders(childFolders, "");
            }
        }
    }
}

@Composable
private fun ChildFolders(folders: List<TagFolder>, prefix: String) {
    folders.forEach { childFolder ->
        Text(
            text = "•$prefix ${childFolder.tag}",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(vertical = 4.dp)
        )
        ChildFolders(childFolder.children, "$prefix    ")
    }
}

@OptIn(ExperimentalStdlibApi::class)
@Composable
fun ColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Folder Color",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyRow {
            items(FolderColors) { color ->
                val isSelected = selectedColor == color.toArgb().toHexString()
                val borderWidth = if (isSelected) 3.dp else 1.dp
                val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.LightGray

                Surface(
                    shape = CircleShape,
                    color = color,
                    border = BorderStroke(borderWidth, borderColor),
                    modifier = Modifier
                        .padding(4.dp)
                        .size(40.dp)
                        .clickable { onColorSelected(color.toArgb().toHexString()) }
                ) {}

                Spacer(modifier = Modifier.width(4.dp))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
@Preview
private fun AddEditFolderContentPreview() {
    MaterialTheme {
        Surface {
            AddEditFolderContent(
                tag = "New Folder",
                parentPath = "/knitting",
                tagGroups = "group1, group2, group3",
                color = FolderColors.random().toArgb().toHexString(), // Blue
                childFolders = listOf(
                    TagFolder(
                        id = UUID.randomUUID(),
                        tag = "Child Folder 1",
                        children = emptyList()
                    ),
                    TagFolder(
                        id = UUID.randomUUID(),
                        tag = "Child Folder 2",
                        children = emptyList()
                    )
                )
            )
        }
    }
}
