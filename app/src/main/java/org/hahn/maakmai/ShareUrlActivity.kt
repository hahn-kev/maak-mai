package org.hahn.maakmai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.hahn.maakmai.data.BookmarkRepository
import org.hahn.maakmai.util.SharedBookmarkFactory
import org.hahn.maakmai.util.ShareTextParser
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class ShareUrlActivity : ComponentActivity() {

    @Inject
    lateinit var bookmarkRepository: BookmarkRepository

    /** Id of the bookmark auto-captured for the current share, if any. */
    private var capturedBookmarkId: UUID? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val restoredId = savedInstanceState?.getString(KEY_CAPTURED_ID)?.let(UUID::fromString)
        if (restoredId != null) {
            // Recreated (e.g. rotation) — reuse the bookmark already captured for this
            // share instead of capturing it again.
            capturedBookmarkId = restoredId
            renderEditor(restoredId)
        } else {
            handleIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capturedBookmarkId?.let { outState.putString(KEY_CAPTURED_ID, it.toString()) }
    }

    private fun handleIntent(intent: Intent) {
        // Check if the intent has the ACTION_SEND action and the type is text
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            // Parse the shared extras. The URL is located within the shared text
            // rather than requiring the whole text to be a URL, so "Title + URL"
            // share shapes still capture the link.
            val parsed = ShareTextParser.parse(
                text = intent.getStringExtra(Intent.EXTRA_TEXT),
                subject = intent.getStringExtra(Intent.EXTRA_SUBJECT),
                titleExtra = intent.getStringExtra(Intent.EXTRA_TITLE)
            )

            if (parsed.url != null || parsed.title != null) {
                // Persist the bookmark immediately so the shared link is never lost,
                // even if the user backs out, closes, or swipes the app away without
                // tapping Save. The Add screen then edits this same record by id.
                lifecycleScope.launch {
                    val bookmark = SharedBookmarkFactory.fromShare(parsed)
                    bookmarkRepository.createBookmark(bookmark)
                    capturedBookmarkId = bookmark.id
                    renderEditor(bookmark.id)
                }
            } else {
                // Nothing usable was shared, finish the activity
                finish()
            }
        } else {
            // Not a share intent, finish the activity
            finish()
        }
    }

    private fun renderEditor(bookmarkId: UUID) {
        setContent {
            val navController = rememberNavController()
            val navActions = remember(navController) {
                MaakMaiNavigationActions(navController)
            }

            MaakMaiNavGraph(
                navController = navController,
                onEditDone = { finish() },
                startDestination = navActions.shareCaptureRoute(bookmarkId)
            )
        }
    }

    private companion object {
        const val KEY_CAPTURED_ID = "captured_bookmark_id"
    }
}
