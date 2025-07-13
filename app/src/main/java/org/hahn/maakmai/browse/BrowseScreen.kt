package org.hahn.maakmai.browse

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.hahn.maakmai.R
import org.hahn.maakmai.model.Bookmark
import org.hahn.maakmai.model.TagFolder

@Composable
fun BrowseScreen(path: String, onFolderClick: (String) -> Unit, onFileClick: (String) -> Unit) {

}

@Composable
fun BrowseContent(
    bookmarks: List<Bookmark>,
    tagFolders: List<TagFolder>,
    path: String,
    onFolderClick: (String) -> Unit,
    onFileClick: (String) -> Unit
) {
    val tags = path.split("/").filter { f -> f.isNotEmpty() }.toHashSet();
    val visibleBookmarks = if (tags.size >= 1) {
        bookmarks.filter { b -> b.tags.containsAll(tags) };
    } else {
        bookmarks;
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp)
    ) {
        items(visibleBookmarks) { bookmark ->
            BookmarkCard(bookmark)
        }
    }
}

@Composable
fun BookmarkCard(bookmark: Bookmark) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
            painter = painterResource(R.drawable.teddy),
            contentDescription = "Contact profile picture",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = bookmark.title,
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = bookmark.description,
                style = MaterialTheme.typography.bodyMedium
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
                    Bookmark(title = "Blue mittens", description = "desc", url = null, tags = listOf("mittens", "knitting")),
                    Bookmark(title = "Red mittens", description = "desc", url = null, tags = listOf("mittens", "knitting")),
                    Bookmark(title = "Green mittens", description = "desc", url = null, tags = listOf("mittens", "knitting")),

                    Bookmark(title = "Blue mittens", description = "desc", url = null, tags = listOf("mittens", "crochet")),
                    Bookmark(title = "Red mittens", description = "desc", url = null, tags = listOf("mittens", "crochet")),
                    Bookmark(title = "Green mittens", description = "desc", url = null, tags = listOf("mittens", "crochet")),
                ),
                tagFolders = listOf(
                    TagFolder(tag = "mittens", children = listOf()),
                    TagFolder(tag = "crochet", children = listOf("mittens")),
                    TagFolder(tag = "knitting", children = listOf("mittens")),
                ),
                path = "/knitting",
                onFolderClick = {},
                onFileClick = {}
            )
        }
    }
}