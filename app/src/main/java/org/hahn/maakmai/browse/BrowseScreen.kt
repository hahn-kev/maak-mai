package org.hahn.maakmai.browse

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import coil3.compose.AsyncImage
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.hahn.maakmai.R
import org.hahn.maakmai.model.Bookmark
import org.hahn.maakmai.model.TagFolder
import java.util.UUID
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onBookmarkClick: (Bookmark) -> Unit,
    onFolderClick: (FolderViewModel) -> Unit,
    onAddBookmark: () -> Unit,
    onEditBookmark: (UUID) -> Unit,
    onAddFolder: () -> Unit,
    onEditFolder: (UUID) -> Unit,
    onBack: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // FAB for adding a folder
                SmallFloatingActionButton(
                    onClick = { onAddFolder() },
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "Add folder",
                        modifier = Modifier.size(24.dp)
                    )
                }

                // FAB for adding a bookmark
                SmallFloatingActionButton(onClick = onAddBookmark) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = "Add bookmark",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.path) },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Edit folder button - only visible when not at root
                        if (uiState.path != "/") {
                            uiState.currentFolderId?.let { folderId ->
                                IconButton(onClick = { onEditFolder(folderId) }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit folder"
                                    )
                                }
                            }
                        }
                        Switch(uiState.showAll, onCheckedChange = viewModel::setShowAll, modifier = Modifier.padding(end = 8.dp))
                        Text(text = "Show all", style = MaterialTheme.typography.bodySmall)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
        },
    ) { paddingValues ->
        BrowseContent(
            bookmarks = uiState.visibleBookmarks,
            tagFolders = uiState.visibleFolders,
            isLoading = uiState.loading,
            modifier = Modifier.padding(paddingValues),
            onFolderClick = onFolderClick,
            onBookmarkClick = { bookmark ->
                if (!bookmark.url.isNullOrEmpty() && Patterns.WEB_URL.matcher(bookmark.url).matches()) {
                    // Open URL in browser
                    val intent = Intent(Intent.ACTION_VIEW, bookmark.url.toUri())
                    context.startActivity(intent)
                } else {
                    // Call onEdit if URL is not valid
                    onEditBookmark(bookmark.id)
                }
            },
            onBookmarkEdit = { onEditBookmark(it.id) },
            onAddBookmark = onAddBookmark,
            onAddFolder = onAddFolder
        )
    }

}

@Composable
fun BrowseContent(
    bookmarks: List<Bookmark>,
    tagFolders: List<FolderViewModel>,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
    onFolderClick: (FolderViewModel) -> Unit = {},
    onBookmarkClick: (Bookmark) -> Unit = {},
    onBookmarkEdit: (Bookmark) -> Unit = {},
    onAddBookmark: () -> Unit = {},
    onAddFolder: () -> Unit = {},
) {
    if (isLoading) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else if (tagFolders.isEmpty() && bookmarks.isEmpty()) {
        EmptyStateView(modifier = modifier, onAddBookmark = onAddBookmark, onAddFolder = onAddFolder)
    } else {
        Column(
            modifier = modifier
        ) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 128.dp)
            ) {
                items(tagFolders) { tagFolder ->
                    FolderCard(tagFolder.folder, { onFolderClick(tagFolder) })
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(
                    minSize = 180.dp
                )
            ) {
                items(bookmarks) { bookmark ->
                    BookmarkCard(
                        bookmark = bookmark,
                        onOpen = onBookmarkClick,
                        onEdit = onBookmarkEdit
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStateView(modifier: Modifier = Modifier, onAddBookmark: () -> Unit = {}, onAddFolder: () -> Unit = {}) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Bookmark,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No bookmarks or folders",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = "Add a bookmark or folder to get started",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp)
            )
            Row(
                modifier = Modifier.padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onAddBookmark)
                ) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = "Add bookmark",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Add Bookmark",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable(onClick = onAddFolder)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreateNewFolder,
                        contentDescription = "Add folder",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Add Folder",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookmarkCard(bookmark: Bookmark, onOpen: (Bookmark) -> Unit = {}, onEdit: (Bookmark) -> Unit = {}) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .combinedClickable(
                onClick = { onOpen(bookmark) },
                onLongClick = { onEdit(bookmark) }
            )
    ) {
        // Check if the bookmark has an attachment image
        if (bookmark.imageAttachmentId != null) {
            // Construct the URI for the attachment
            val attachmentUri = Uri.parse("content://org.hahn.maakmai.attachment/${bookmark.imageAttachmentId}")

            // Use AsyncImage to load the image from the URI
            AsyncImage(
                model = attachmentUri,
                contentDescription = bookmark.description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(180.dp),
                fallback = painterResource(R.drawable.teddy) // Fallback to placeholder if loading fails
            )
        } else {
            // Use the placeholder image if there's no attachment
            Image(
                painter = painterResource(R.drawable.teddy),
                contentDescription = bookmark.description,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(180.dp)
            )
        }

        Text(
            text = bookmark.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 3
        )
        Text(
            text = bookmark.description,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 4.dp),
            maxLines = 3
        )
    }
}

@Composable
fun FolderCard(folder: TagFolder, onOpen: (TagFolder) -> Unit = {}) {
    Card(
        modifier = Modifier
            .width(180.dp)
            .padding(8.dp)
            .clickable { onOpen(folder) }
    ) {
        Image(
            painter = painterResource(R.drawable.teddy),
            contentDescription = folder.tag,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(120.dp)
        )
        Text(
            text = folder.tag,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Preview
@Composable
fun BrowseContentPreview() {
    MaterialTheme {
        Surface {
            BrowseContent(
                bookmarks = listOf(
                    Bookmark(title = "Blue shirt", description = "desc", url = null, tags = listOf("knitting"), id = UUID.randomUUID()),
                    Bookmark(title = "Red shirt", description = "desc", url = null, tags = listOf("knitting"), id = UUID.randomUUID()),
                    Bookmark(title = "Green shirt", description = "desc", url = null, tags = listOf("knitting"), id = UUID.randomUUID()),
                    Bookmark(title = "Blue mittens", description = "desc", url = null, tags = listOf("mittens", "knitting"), id = UUID.randomUUID()),
                    Bookmark(title = "Red mittens", description = "desc", url = null, tags = listOf("mittens", "knitting"), id = UUID.randomUUID()),
                    Bookmark(title = "Green mittens", description = "desc", url = null, tags = listOf("mittens", "knitting"), id = UUID.randomUUID()),
                    Bookmark(title = "Blue mittens", description = "desc", url = null, tags = listOf("mittens", "crochet"), id = UUID.randomUUID()),
                    Bookmark(title = "Red mittens", description = "desc", url = null, tags = listOf("mittens", "crochet"), id = UUID.randomUUID()),
                    Bookmark(title = "Green mittens", description = "desc", url = null, tags = listOf("mittens", "crochet"), id = UUID.randomUUID()),
                    Bookmark(title = "Blue scarf", description = "desc", url = null, tags = listOf("scarf", "crochet"), id = UUID.randomUUID()),
                    Bookmark(title = "Red scarf", description = "desc", url = null, tags = listOf("scarf", "crochet"), id = UUID.randomUUID()),
                    Bookmark(title = "Green scarf", description = "desc", url = null, tags = listOf("scarf", "crochet"), id = UUID.randomUUID()),
                    Bookmark(title = "Blue sweater", description = "desc", url = null, tags = listOf("sweater", "knitting"), id = UUID.randomUUID()),
                    Bookmark(title = "Red sweater", description = "desc", url = null, tags = listOf("sweater", "knitting"), id = UUID.randomUUID()),
                    Bookmark(title = "Green sweater", description = "desc", url = null, tags = listOf("sweater", "knitting"), id = UUID.randomUUID()),
                    Bookmark(title = "Applesauce", description = "desc", url = null, tags = listOf(), id = UUID.randomUUID()),
                ),
                tagFolders = listOf(
                    FolderViewModel(
                        TagFolder(
                            id = UUID.randomUUID(),
                            tag = "crochet",
                            children = listOf(
                                TagFolder(id = UUID.randomUUID(), tag = "mittens", children = listOf()),
                                TagFolder(id = UUID.randomUUID(), tag = "scarf", children = listOf())
                            ),
                            rootFolder = true
                        ), "/crochet"
                    ),
                    FolderViewModel(
                        TagFolder(
                            id = UUID.randomUUID(),
                            tag = "knitting",
                            children = listOf(
                                TagFolder(id = UUID.randomUUID(), tag = "mittens", children = listOf()),
                                TagFolder(id = UUID.randomUUID(), tag = "sweater", children = listOf())
                            ),
                            rootFolder = true
                        ), "/knitting"
                    )
                ),
                onFolderClick = {},
                onBookmarkClick = {},
            )
        }
    }
}

@Preview
@Composable
fun LongBookmarkText() {
    MaterialTheme {
        Surface {
            BrowseContent(
                bookmarks = listOf(
                    Bookmark(
                        title = "this is a very long title, it has many lines and should be cut off at some point",
                        description = "desc",
                        url = null,
                        tags = listOf(),
                        id = UUID.randomUUID()
                    ),
                    Bookmark(
                        title = "short title",
                        description = "this is a very long description, it has many lines and should be cut off at some point",
                        url = null,
                        tags = listOf(),
                        id = UUID.randomUUID()
                    )
                ),
                tagFolders = emptyList(),
                onFolderClick = {},
                onBookmarkClick = {},
            )
        }
    }
}

@Preview
@Composable
fun EmptyStatePreview() {
    MaterialTheme {
        Surface {
            BrowseContent(
                bookmarks = emptyList(),
                tagFolders = emptyList(),
                onFolderClick = {},
                onBookmarkClick = {},
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyStateViewPreview() {
    MaterialTheme {
        Surface {
            EmptyStateView()
        }
    }
}
