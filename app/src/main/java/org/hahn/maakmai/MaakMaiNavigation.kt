package org.hahn.maakmai

import androidx.navigation.NavController
import org.hahn.maakmai.MaakMaiArgs.BOOKMARK_ID_ARG
import org.hahn.maakmai.MaakMaiArgs.PATH_ARG
import org.hahn.maakmai.MaakMaiArgs.TITLE_ARG
import org.hahn.maakmai.MaakMaiScreens.ADD_EDIT_BOOKMARK_SCREEN
import org.hahn.maakmai.MaakMaiScreens.BROWSE_SCREEN
import java.util.UUID

private object MaakMaiScreens {
    const val BROWSE_SCREEN = "browse"
    const val ADD_EDIT_BOOKMARK_SCREEN = "addEditBookmark"
}

object MaakMaiArgs {
    const val PATH_ARG = "path";
    const val BOOKMARK_ID_ARG = "id";
    const val TITLE_ARG = "title";
}

object MaakMaiDestinations {
    const val BROWSE_ROUTE = "$BROWSE_SCREEN?$PATH_ARG={$PATH_ARG}"
    const val ADD_EDIT_BOOKMARK_ROUTE = "$ADD_EDIT_BOOKMARK_SCREEN/{$TITLE_ARG}?$BOOKMARK_ID_ARG={$BOOKMARK_ID_ARG}"
}

class MaakMaiNavigationActions(private val navController: NavController) {
    fun navigateToBrowse(path: String = "") {
        navController.navigate("$BROWSE_SCREEN?$PATH_ARG=$path");
    }

    fun navigateToAdd() {
        navController.navigate("$ADD_EDIT_BOOKMARK_SCREEN/Add Bookmark")
    }

    fun navigateToEdit(bookmarkId: UUID) {
        navController.navigate("$ADD_EDIT_BOOKMARK_SCREEN/Edit Bookmark?$BOOKMARK_ID_ARG=$bookmarkId")
    }
}