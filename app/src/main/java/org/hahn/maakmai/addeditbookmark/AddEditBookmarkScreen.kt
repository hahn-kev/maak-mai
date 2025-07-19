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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun AddEditBookmarkScreen(
    topBarTitle: String,
    modifier: Modifier = Modifier,
    onBookmarkUpdate: () -> Unit = {},
    viewModel: AddEditBookmarkViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            SmallFloatingActionButton(onClick = viewModel::saveBookmark) {
                Icon(Icons.Filled.Done, "Save Bookmark")
            }
        }
    ) { paddingValues ->
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        AddEditBookmarkContent(
            title = uiState.title,
            onTitleChanged = viewModel::updateTitle,
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
    onTitleChanged: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        val textFieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            cursorColor = MaterialTheme.colorScheme.onSecondary
        )
        OutlinedTextField(
            value = title,
            modifier = Modifier.fillMaxWidth(),
            onValueChange = onTitleChanged,
            placeholder = {
                Text(
                    text = "Title",
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            textStyle = MaterialTheme.typography.headlineSmall
                .copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            colors = textFieldColors
        )
//        OutlinedTextField(
//            value = description,
//            onValueChange = onDescriptionChanged,
//            placeholder = { Text(stringResource(id = R.string.description_hint)) },
//            modifier = Modifier
//                .height(350.dp)
//                .fillMaxWidth(),
//            colors = textFieldColors
//        )
    }
}

@Composable
@Preview
private fun AddEditBookmarkContentPreview() {
    MaterialTheme {
        Surface {
            AddEditBookmarkContent(
                title = "",
            )

        }
    }
}