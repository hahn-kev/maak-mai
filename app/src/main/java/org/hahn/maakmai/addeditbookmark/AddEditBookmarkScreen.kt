package org.hahn.maakmai.addeditbookmark

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.hahn.maakmai.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBookmarkScreen(
    topBarTitle: String,
    modifier: Modifier = Modifier,
    onBookmarkUpdate: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: AddEditBookmarkViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
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
            onTitleChanged = viewModel::updateTitle,
            onDescriptionChanged = viewModel::updateDescription,
            onUrlChanged = viewModel::updateUrl,
            onTagsChanged = viewModel::updateTags,
            modifier = Modifier.padding(paddingValues)
        )

        LaunchedEffect(uiState.isBookmarkSaved) {
            if (uiState.isBookmarkSaved) {
                onBookmarkUpdate()
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
    modifier: Modifier = Modifier,
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
            maxLines = 1,
            colors = textFieldColors,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Uri
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) }
            )
        )

        // Simple tags implementation - comma-separated string
        val tagsString = tags.joinToString(", ")
        OutlinedTextField(
            value = tagsString,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            onValueChange = { newTagsString ->
                val newTags = newTagsString.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
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
