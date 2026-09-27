package org.hahn.maakmai.tags

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TagsScreen(
    onBack: () -> Unit,
    viewModel: TagsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var renaming by remember { mutableStateOf<String?>(null) }

    uiState.errorMessage?.let { message ->
        LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            viewModel.errorShown()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Tags") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.loading -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            uiState.tags.isEmpty() -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No tags yet")
            }
            else -> LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(uiState.tags, key = { it.tag }) { item ->
                    ListItem(
                        headlineContent = { Text(item.tag) },
                        supportingContent = {
                            Text(if (item.bookmarkCount == 1) "1 bookmark" else "${item.bookmarkCount} bookmarks")
                        },
                        leadingContent = if (item.hasFolder) {
                            { Icon(Icons.Default.Folder, contentDescription = "Has folder") }
                        } else null,
                        modifier = Modifier.clickable { renaming = item.tag }
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    renaming?.let { oldTag ->
        RenameTagDialog(
            oldTag = oldTag,
            onRename = { newTag ->
                renaming = null
                viewModel.requestRename(oldTag, newTag)
            },
            onDismiss = { renaming = null }
        )
    }

    uiState.pendingMerge?.let { pending ->
        MergeTagConfirmDialog(
            oldTag = pending.oldTag,
            newTag = pending.newTag,
            onConfirm = viewModel::confirmMerge,
            onDismiss = viewModel::cancelMerge
        )
    }
}

@Composable
private fun RenameTagDialog(
    oldTag: String,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(TextFieldValue(oldTag, selection = TextRange(0, oldTag.length))) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val canRename = text.text.isNotBlank() && !text.text.trim().equals(oldTag, ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename tag") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Tag") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { if (canRename) onRename(text.text) })
            )
        },
        confirmButton = {
            TextButton(onClick = { onRename(text.text) }, enabled = canRename) { Text("Rename") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
