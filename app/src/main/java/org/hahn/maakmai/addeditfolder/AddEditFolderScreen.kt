package org.hahn.maakmai.addeditfolder

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
            showDelete = !uiState.isNew,
            onTagChanged = viewModel::updateTag,
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
    onTagChanged: (String) -> Unit = {},
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

@Composable
@Preview
private fun AddEditFolderContentPreview() {
    MaterialTheme {
        Surface {
            AddEditFolderContent(
                tag = "New Folder",
                parentPath = "/knitting",
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
