package org.hahn.maakmai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.hahn.maakmai.data.BookmarkRepository
import org.hahn.maakmai.model.Bookmark
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import androidx.core.net.toUri

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

                var description = intent.getStringExtra(Intent.EXTRA_SUBJECT)
                var title = intent.getStringExtra(Intent.EXTRA_TITLE) ?: description
                if (title == description) {
                    description = null
                }
                //determine if shared text is url
                var url: String? = null
                if (Patterns.WEB_URL.matcher(sharedText).matches()) {
                    url = sharedText
                    if (title == null) {
                        title = extractTitleFromUrl(url)
                    }
                } else {
                    description = title
                    title = sharedText
                }

                // Create a new bookmark
                val bookmark = Bookmark(
                    id = UUID.randomUUID(),
                    title = title,
                    description = description ?: "",
                    url = url,
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

    private fun extractLastPathSegment(uri: Uri): String? {
        return try {
            val path = uri.path ?: return null
            path.split("/").last().takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    private fun isAllNumbers(str: String): Boolean {
        return str.all { it.isDigit() || it == '.' || it == ',' }
    }

    private fun cleanupTitle(title: String): String {
        return title
            .replace("[_-]".toRegex(), " ")
            .replace("\\s+".toRegex(), " ")
            .split(" ")
            .joinToString(" ") { word ->
                word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            .trim()
    }

    private fun extractTitleFromUrl(url: String): String {
        try {
            val uri = url.toUri()

            // Try last path segment first
            extractLastPathSegment(uri)?.let { segment ->
                if (!isAllNumbers(segment)) {
                    return cleanupTitle(segment)
                }
            }

            // Try hostname without TLD
            uri.host?.let { host ->
                val parts = host.split(".")
                if (parts.size >= 2) {
                    val domain = parts[parts.size - 2]
                    if (!isAllNumbers(domain)) {
                        return cleanupTitle(domain)
                    }
                }
            }

            // Fallback to full hostname
            return uri.host ?: url
        } catch (e: Exception) {
            return url
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
}
