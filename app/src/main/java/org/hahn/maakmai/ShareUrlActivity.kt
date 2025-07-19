package org.hahn.maakmai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.hahn.maakmai.data.BookmarkRepository
import org.hahn.maakmai.model.Bookmark
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class ShareUrlActivity : ComponentActivity() {

    @Inject
    lateinit var bookmarkRepository: BookmarkRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle the intent
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        finish()
    }

    private fun handleIntent(intent: Intent) {
        // Check if the intent has the ACTION_SEND action and the type is text
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            // Extract the shared text
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)

            if (!sharedText.isNullOrEmpty()) {
                // Create and save the bookmark
                // Extract a title from the URL
                val title = extractTitleFromUrl(sharedText)
                val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)

                // Create a new bookmark
                val bookmark = Bookmark(
                    id = UUID.randomUUID(),
                    title = title,
                    description = subject ?: "",
                    url = sharedText,
                    tags = listOf()
                )
                saveBookmark(bookmark)
            } else {
                // No text was shared, finish the activity
                finish()
            }
        } else {
            // Not a share intent, finish the activity
            finish()
        }
    }

    private fun saveBookmark(bookmark: Bookmark) {


        // Save the bookmark
        lifecycleScope.launch {
            bookmarkRepository.createBookmark(bookmark)

            // Show a toast notification
            runOnUiThread {
                android.widget.Toast.makeText(
                    this@ShareUrlActivity,
                    "Bookmark saved: ${bookmark.title}",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }

            // Finish the activity after saving
            finish()
        }
    }

    private fun extractTitleFromUrl(url: String): String {
        // Try to extract a meaningful title from the URL
        return try {
            // Remove protocol
            val withoutProtocol = url.replace(Regex("^(https?://|www\\.)"), "")
            // Get domain
            val domain = withoutProtocol.split("/").firstOrNull() ?: url
            // Use domain as title
            domain
        } catch (e: Exception) {
            // If anything goes wrong, just use "Shared URL" as the title
            "Shared URL"
        }
    }
}
