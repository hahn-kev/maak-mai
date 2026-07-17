package org.hahn.maakmai

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import org.hahn.maakmai.util.ShareTextParser

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
            // Parse the shared extras. The URL is located within the shared text
            // rather than requiring the whole text to be a URL, so "Title + URL"
            // share shapes still capture the link.
            val parsed = ShareTextParser.parse(
                text = intent.getStringExtra(Intent.EXTRA_TEXT),
                subject = intent.getStringExtra(Intent.EXTRA_SUBJECT),
                titleExtra = intent.getStringExtra(Intent.EXTRA_TITLE)
            )

            if (parsed.url != null || parsed.title != null) {
                // Launch the add bookmark screen with the parsed share
                setContent {
                    val navController = rememberNavController()
                    val navActions = remember(navController) {
                        MaakMaiNavigationActions(navController)
                    }

                    val route = navActions.addFromShareRoute(parsed.url, parsed.title, parsed.description)
                    MaakMaiNavGraph(
                        navController = navController,
                        onEditDone = { finish() },
                        startDestination = route
                    )
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
}
