package org.hahn.maakmai.addeditbookmark

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.hahn.maakmai.model.TagFolder

/**
 * Picks tags by browsing folders, by the tag groups of the picked folders, and from
 * the most used tags. Used by the Add screen and by bulk add.
 */
@Composable
fun TagPicker(
    folders: List<TagFolder>,
    selectedFolderPath: List<TagFolder>,
    onFolderSelected: (TagFolder) -> Unit,
    onRemoveLastFolder: () -> Unit,
    groupedFolderTags: List<TagGroup>,
    onFolderTagToggled: (TagGroup, TagUiState) -> Unit,
    priorityTags: List<TagUiState>,
    onPriorityTagToggled: (TagUiState) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
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
