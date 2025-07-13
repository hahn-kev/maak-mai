package org.hahn.maakmai.browse

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.hahn.maakmai.R
import org.hahn.maakmai.model.Bookmark
import org.hahn.maakmai.model.TagFolder

@Composable
fun BrowseScreen() {
    var path by remember { mutableStateOf("/") }
    BrowseContent(
        bookmarks = listOf(
            Bookmark(title = "Blue shirt", description = "desc", url = null, tags = listOf("knitting")),
            Bookmark(title = "Red shirt", description = "desc", url = null, tags = listOf("knitting")),
            Bookmark(title = "Green shirt", description = "desc", url = null, tags = listOf("knitting")),

            Bookmark(title = "Blue mittens", description = "desc", url = null, tags = listOf("mittens", "knitting")),
            Bookmark(title = "Red mittens", description = "desc", url = null, tags = listOf("mittens", "knitting")),
            Bookmark(title = "Green mittens", description = "desc", url = null, tags = listOf("mittens", "knitting")),

            Bookmark(title = "Blue mittens", description = "desc", url = null, tags = listOf("mittens", "crochet")),
            Bookmark(title = "Red mittens", description = "desc", url = null, tags = listOf("mittens", "crochet")),
            Bookmark(title = "Green mittens", description = "desc", url = null, tags = listOf("mittens", "crochet")),

            Bookmark(title = "Blue scarf", description = "desc", url = null, tags = listOf("scarf", "crochet")),
            Bookmark(title = "Red scarf", description = "desc", url = null, tags = listOf("scarf", "crochet")),
            Bookmark(title = "Green scarf", description = "desc", url = null, tags = listOf("scarf", "crochet")),

            Bookmark(title = "Blue sweater", description = "desc", url = null, tags = listOf("sweater", "knitting")),
            Bookmark(title = "Red sweater", description = "desc", url = null, tags = listOf("sweater", "knitting")),
            Bookmark(title = "Green sweater", description = "desc", url = null, tags = listOf("sweater", "knitting")),
        ),
        tagFolders = listOf(
            TagFolder(tag = "mittens", children = listOf()),
            TagFolder(tag = "scarf", children = listOf()),
            TagFolder(tag = "sweater", children = listOf()),
            TagFolder(tag = "crochet", children = listOf("mittens", "scarf"), rootFolder = true),
            TagFolder(tag = "knitting", children = listOf("mittens", "sweater"), rootFolder = true),
        ),
        path = path,
        onFolderClick = { tagFolder ->
            if (path == "/") {
                path += tagFolder.tag
            } else {
                path = "$path/${tagFolder.tag}"
            }
        },
        onBookmarkClick = {},
        onBackClick = {
            val lastIndex = path.lastIndexOf("/");
            if (lastIndex == -1) return@BrowseContent;
            path = path.substring(0, lastIndex)
            if (path.isEmpty()) {
                path = "/"
            }
        }
    )
}

@Composable
fun BrowseContent(
    bookmarks: List<Bookmark>,
    tagFolders: List<TagFolder>,
    path: String,
    onFolderClick: (TagFolder) -> Unit,
    onBookmarkClick: (Bookmark) -> Unit,
    onBackClick: () -> Unit = {}
) {
    var showAll by remember { mutableStateOf(false) }
    val tags = path.split("/").filter { f -> f.isNotEmpty() }.toHashSet();
    var currentFolder = getCurrentFolder(path, tagFolders)
    val visibleFolders = getVisibleFolders(currentFolder, tagFolders)
    if (currentFolder == null) {
        currentFolder = TagFolder(tag = "root", children = visibleFolders.map { f -> f.tag })
    }
    val visibleBookmarks = getVisibleBookmarks(
        tags, if (!showAll) {
            currentFolder
        } else {
            null
        }, bookmarks
    )
    Column {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 128.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row {
                    Text(text = path, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.clickable { onBackClick() })
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(showAll, onCheckedChange = { showAll = it })
                        Text(text = "Show all", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            items(visibleFolders) { tagFolder ->
                FolderCard(tagFolder, onFolderClick)
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(
                minSize = 180.dp
            )
        ) {

            items(visibleBookmarks) { bookmark ->
                BookmarkCard(bookmark, onBookmarkClick)
            }
        }
    }
}

private fun getVisibleFolders(currentFolder: TagFolder?, tagFolders: List<TagFolder>): List<TagFolder> {
    return if (currentFolder != null) {
        currentFolder.children.map { c -> tagFolders.single { f -> f.tag == c } }
    } else {
        tagFolders.filter { folder -> folder.rootFolder };
    }
}

private fun getCurrentFolder(path: String, tagFolders: List<TagFolder>): TagFolder? {
    val folder = path.split("/").filter { f -> f.isNotEmpty() }.lastOrNull() ?: return null;
    val currentFolder = tagFolders.singleOrNull { f -> f.tag == folder };
    return currentFolder
}

private fun getVisibleBookmarks(
    tags: HashSet<String>,
    currentFolder: TagFolder?,
    bookmarks: List<Bookmark>
): List<Bookmark> {
    var visibleBookmarks = bookmarks;
    if (currentFolder != null) {
        visibleBookmarks = visibleBookmarks.filter { b -> !b.tags.any { tag -> currentFolder.children.contains(tag) } }
    }
    if (tags.size >= 1) {
        return visibleBookmarks.filter { b -> b.tags.containsAll(tags) };
    } else {
        return visibleBookmarks;
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
                    Bookmark(title = "Blue shirt", description = "desc", url = null, tags = listOf("knitting")),
                    Bookmark(title = "Red shirt", description = "desc", url = null, tags = listOf("knitting")),
                    Bookmark(title = "Green shirt", description = "desc", url = null, tags = listOf("knitting")),

                    Bookmark(title = "Blue mittens", description = "desc", url = null, tags = listOf("mittens", "knitting")),
                    Bookmark(title = "Red mittens", description = "desc", url = null, tags = listOf("mittens", "knitting")),
                    Bookmark(title = "Green mittens", description = "desc", url = null, tags = listOf("mittens", "knitting")),

                    Bookmark(title = "Blue mittens", description = "desc", url = null, tags = listOf("mittens", "crochet")),
                    Bookmark(title = "Red mittens", description = "desc", url = null, tags = listOf("mittens", "crochet")),
                    Bookmark(title = "Green mittens", description = "desc", url = null, tags = listOf("mittens", "crochet")),

                    Bookmark(title = "Blue scarf", description = "desc", url = null, tags = listOf("scarf", "crochet")),
                    Bookmark(title = "Red scarf", description = "desc", url = null, tags = listOf("scarf", "crochet")),
                    Bookmark(title = "Green scarf", description = "desc", url = null, tags = listOf("scarf", "crochet")),

                    Bookmark(title = "Blue sweater", description = "desc", url = null, tags = listOf("sweater", "knitting")),
                    Bookmark(title = "Red sweater", description = "desc", url = null, tags = listOf("sweater", "knitting")),
                    Bookmark(title = "Green sweater", description = "desc", url = null, tags = listOf("sweater", "knitting")),
                ),
                tagFolders = listOf(
                    TagFolder(tag = "mittens", children = listOf()),
                    TagFolder(tag = "scarf", children = listOf()),
                    TagFolder(tag = "sweater", children = listOf()),
                    TagFolder(tag = "crochet", children = listOf("mittens", "scarf")),
                    TagFolder(tag = "knitting", children = listOf("mittens", "sweater")),
                ),
                path = "/knitting",
                onFolderClick = {},
                onBookmarkClick = {}
            )
        }
    }
}