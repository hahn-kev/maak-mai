package org.hahn.maakmai.browse

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowColumn
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.toLowerCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import org.hahn.maakmai.R
import org.hahn.maakmai.model.Bookmark
import org.hahn.maakmai.model.TagFolder
import org.hahn.maakmai.ui.theme.FolderColors
import java.util.UUID

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

    // State for search dialog
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchText by remember {
        mutableStateOf(
            TextFieldValue(
                text = ""
            )
        )
    }

    // Function to handle search button click
    val onSearch = {
        searchText = TextFieldValue(uiState.searchQuery, selection = TextRange(0, uiState.searchQuery.length)) // Initialize with current search query
        showSearchDialog = true
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // FAB for adding a folder
                SmallFloatingActionButton(
                    onClick = { onAddFolder() }
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
                FloatingActionButton(onClick = { onSearch() }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = uiState.path)
                        // Show search indicator if search query is not empty
                        if (uiState.searchQuery.isNotEmpty()) {
                            Text(
                                text = " (Search: ${uiState.searchQuery})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
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

    // Search dialog
    if (showSearchDialog) {
        val focusRequester = remember { FocusRequester() }
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }

        val onSearchSubmit = {
            viewModel.setSearchQuery(searchText.text)
            viewModel.setShowAll(true)
            showSearchDialog = false
        }
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text("Search Bookmarks") },
            text = {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Search") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { onSearchSubmit() }
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onSearchSubmit()
                    }
                ) {
                    Text("Search")
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            searchText = TextFieldValue()
                            viewModel.setSearchQuery("")
                            viewModel.setShowAll(false)
                            showSearchDialog = false
                        }
                    ) {
                        Text("Clear")
                    }
                    TextButton(
                        onClick = {
                            showSearchDialog = false
                        }
                    ) {
                        Text("Cancel")
                    }
                }
            }
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
        LazyVerticalStaggeredGrid(
            modifier = modifier,
            columns = StaggeredGridCells.Adaptive(
                minSize = 180.dp
            ),
            content = {
                item(span = StaggeredGridItemSpan.FullLine) {
                    val foldersPerRow = 3;
                    FlowRow(
                        maxItemsInEachRow = foldersPerRow,
                    ) {
                        for (tagFolder in tagFolders.sortedBy { it.folder.tag.lowercase() }) {
                            FolderCard(
                                modifier = Modifier
                                    .height(120.dp)
                                    .weight(1f),
                                folder = tagFolder.folder,
                                onOpen = { onFolderClick(tagFolder) }
                            )
                        }
                        // Fill the remaining space with empty cards
                        repeat(tagFolders.size % foldersPerRow) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
                items(bookmarks) { bookmark ->
                    BookmarkCard(
                        bookmark = bookmark,
                        onOpen = onBookmarkClick,
                        onEdit = onBookmarkEdit
                    )
                }
            }
        )
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
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .fillMaxWidth()
            )
        } else {
            // Calculate deterministic color from bookmark title
            val colorSeed = bookmark.title.hashCode()
            val folderColor = FolderColors[Math.abs(colorSeed) % FolderColors.size]

            // Use the placeholder with calculated background color if there's no attachment
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(folderColor)
                    .height(100.dp)
                    .fillMaxWidth()
            )
        }

        Text(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 4.dp),
            text = bookmark.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 3
        )
        if (bookmark.description != null && !bookmark.description.isEmpty()) {
            Text(
                text = bookmark.description,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 4.dp),
                maxLines = 3
            )
        }
    }
}

@OptIn(ExperimentalStdlibApi::class, ExperimentalLayoutApi::class)
@Composable
fun FolderCard(modifier: Modifier, folder: TagFolder, onOpen: (TagFolder) -> Unit = {}) {
    ElevatedCard(
        modifier = modifier
            .padding(4.dp)
            .clickable { onOpen(folder) }
    ) {
        var folderColor = 0L;
        try {
            folderColor = folder.color.hexToLong()
        } catch (e: Exception) {

        }
        FlowColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(Color(folderColor))
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                // Optional: Display the first letter of the folder tag
//            Text(
//                text = folder.tag.take(1).uppercase(),
//                style = MaterialTheme.typography.headlineLarge,
//                color = Color.White
//            )
            }
            Text(
                text = folder.tag,
                style = MaterialTheme.typography.titleMedium.copy(
                    textAlign = TextAlign.Left
                ),
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .fillMaxWidth()
            )
        }
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
