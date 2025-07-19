package org.hahn.maakmai.browse

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.hahn.maakmai.R
import org.hahn.maakmai.model.Bookmark
import org.hahn.maakmai.model.TagFolder
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowseScreen(
    onBookmarkClick: (Bookmark) -> Unit,
    onFolderClick: (FolderViewModel) -> Unit,
    onAddBookmark: () -> Unit,
    onBack: () -> Unit,
    viewModel: BrowseViewModel = hiltViewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            SmallFloatingActionButton(onClick = onAddBookmark) {
                Icon(Icons.Default.Add, contentDescription = "Add bookmark")
            }
        },
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.path) },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(uiState.showAll, onCheckedChange = viewModel::setShowAll)
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
            showAll = uiState.showAll,
            path = uiState.path,
            onSetShowAll = viewModel::setShowAll,
            onBackClick = viewModel::onBack,
            onFolderClick = onFolderClick,
            onBookmarkClick = onBookmarkClick,
            modifier = Modifier.padding(paddingValues),
        )
    }

}

@Composable
fun BrowseContent(
    bookmarks: List<Bookmark>,
    tagFolders: List<FolderViewModel>,
    path: String,
    showAll: Boolean,
    modifier: Modifier = Modifier,
    onFolderClick: (FolderViewModel) -> Unit = {},
    onBookmarkClick: (Bookmark) -> Unit = {},
    onSetShowAll: (Boolean) -> Unit = {},
    onBackClick: () -> Unit = {},
) {
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
                BookmarkCard(bookmark, onBookmarkClick)
            }
        }
    }
}


@Composable
fun BookmarkCard(bookmark: Bookmark, onOpen: (Bookmark) -> Unit = {}) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .clickable { onOpen(bookmark) }
    ) {
        Image(
            painter = painterResource(R.drawable.teddy),
            contentDescription = bookmark.description,
            contentScale = ContentScale.Crop,
            modifier = Modifier.size(180.dp)
        )
        Text(text = bookmark.title, style = MaterialTheme.typography.titleMedium)
        Text(text = bookmark.description, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(horizontal = 4.dp))
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
        Text(text = folder.tag, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 4.dp))
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
                path = "/knitting",
                onFolderClick = {},
                onBookmarkClick = {},
                showAll = true
            )
        }
    }
}