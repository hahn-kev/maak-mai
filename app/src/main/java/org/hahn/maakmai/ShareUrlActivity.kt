package org.hahn.maakmai

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class ShareUrlActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle the intent
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        // Check if the intent has the ACTION_SEND action and the type is text
        if (intent.action == Intent.ACTION_SEND && intent.type?.startsWith("text/") == true) {
            // Extract the shared text
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)
            val title = intent.getStringExtra(Intent.EXTRA_TITLE)

            if (!sharedText.isNullOrEmpty()) {
                // Launch the add bookmark screen with the shared URL
                setContent {
                    val navController = rememberNavController()
                    val navActions = remember(navController) {
                        MaakMaiNavigationActions(navController)
                    }

                    // Create a URL parameter to pass to the add bookmark screen
                    val urlParam = if (Patterns.WEB_URL.matcher(sharedText).matches()) {
                        sharedText
                    } else {
                        null
                    }

                    // Set up the navigation graph with the URL parameter
                    val route = navActions.addFromShareRoute(urlParam, title, subject)
                    MaakMaiNavGraph(
                        navController = navController,
                        onEditDone = { finish() },
                        startDestination = route
                    )
                }
            } else {
                // No text was shared, finish the activity
                finish()
            }
        } else {
            // Not a share intent, finish the activity
            finish()
        }
    }
}
