package org.hahn.maakmai.bulkadd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.hahn.maakmai.addeditbookmark.TagPicker

/**
 * Adds many bookmarks at once: the user pastes text, the links in it are found and
 * fetched, and the user picks which ones to add.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulkAddScreen(
    onClose: () -> Unit,
    viewModel: BulkAddViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onClose()
    }

    Scaffold(
        modifier = Modifier.imePadding(),
        topBar = {
            TopAppBar(
                title = { Text("Bulk add links") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.items == null) {
                        Button(
                            onClick = viewModel::process,
                            enabled = uiState.text.isNotBlank()
                        ) {
                            Text("Process")
                        }
                    } else {
                        TextButton(onClick = viewModel::editText, enabled = !uiState.isSaving) {
                            Text("Edit text")
                        }
                        Button(
                            onClick = viewModel::addChecked,
                            enabled = !uiState.isProcessing && !uiState.isSaving && uiState.checkedCount > 0
                        ) {
                            if (uiState.isSaving) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text("Add ${uiState.checkedCount}")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        val items = uiState.items
        if (items == null) {
            OutlinedTextField(
                value = uiState.text,
                onValueChange = viewModel::updateText,
                label = { Text("Paste text with links") },
                modifier = Modifier
                    .padding(paddingValues)
                    .padding(16.dp)
                    .fillMaxSize()
            )
        } else {
            BulkLinkList(
                items = items,
                isProcessing = uiState.isProcessing,
                enabled = !uiState.isSaving,
                onCheckedChange = viewModel::setChecked,
                modifier = Modifier.padding(paddingValues),
                header = {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "Tags for all links",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        TagPicker(
                            folders = uiState.folders,
                            selectedFolderPath = uiState.selectedFolderPath,
                            onFolderSelected = viewModel::selectFolder,
                            onRemoveLastFolder = viewModel::removeLastSelectedFolder,
                            groupedFolderTags = uiState.groupedFolderTags,
                            onFolderTagToggled = viewModel::toggleFolderTag,
                            priorityTags = uiState.tagsPrioritised,
                            onPriorityTagToggled = viewModel::togglePriorityTag,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = uiState.tags,
                            onValueChange = viewModel::updateTags,
                            label = { Text("Tags (comma-separated)") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    }
                    HorizontalDivider()
                }
            )
        }
    }
}

@Composable
private fun BulkLinkList(
    items: List<BulkLinkItem>,
    isProcessing: Boolean,
    enabled: Boolean,
    onCheckedChange: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit = {}
) {
    if (items.isEmpty()) {
        Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            Text("No links found in the text")
        }
        return
    }
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item { header() }
        item {
            val failed = items.count { it.error != null }
            val summary = buildString {
                append("${items.size} links found")
                if (isProcessing) append(", fetching…")
                if (failed > 0) append(", $failed failed")
            }
            Text(
                text = summary,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        items(displayOrder(items), key = { it.sourceUrl }) { item ->
            BulkLinkRow(
                item = item,
                enabled = enabled,
                onCheckedChange = { onCheckedChange(item.sourceUrl, it) },
                modifier = Modifier.animateItem()
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun BulkLinkRow(
    item: BulkLinkItem,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val preview = item.preview
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .toggleable(
                value = item.isChecked,
                enabled = enabled && !item.isLoading,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        Checkbox(checked = item.isChecked, onCheckedChange = null, enabled = enabled && !item.isLoading)
        Box(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            when {
                item.isLoading -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                item.error != null -> Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Failed",
                    tint = MaterialTheme.colorScheme.error
                )
                preview?.imageUrl != null -> AsyncImage(
                    model = preview.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                else -> Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preview?.title ?: item.sourceUrl,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = preview?.url ?: item.sourceUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val error = item.error
            val description = preview?.description?.ifBlank { null }
            if (error != null) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
