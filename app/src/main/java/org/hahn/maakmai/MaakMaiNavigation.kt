package org.hahn.maakmai

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import org.hahn.maakmai.MaakMaiArgs.BOOKMARK_ID_ARG
import org.hahn.maakmai.MaakMaiArgs.FOLDER_ID_ARG
import org.hahn.maakmai.MaakMaiArgs.PARENT_PATH_ARG
import org.hahn.maakmai.MaakMaiArgs.PATH_ARG
import org.hahn.maakmai.MaakMaiArgs.TITLE_ARG
import org.hahn.maakmai.MaakMaiScreens.ADD_EDIT_BOOKMARK_SCREEN
import org.hahn.maakmai.MaakMaiScreens.ADD_EDIT_FOLDER_SCREEN
import org.hahn.maakmai.MaakMaiScreens.BROWSE_SCREEN
import java.util.UUID

private object MaakMaiScreens {
    const val BROWSE_SCREEN = "browse"
    const val ADD_EDIT_BOOKMARK_SCREEN = "addEditBookmark"
    const val ADD_EDIT_FOLDER_SCREEN = "addEditFolder"
}

object MaakMaiArgs {
    const val PATH_ARG = "path";
    const val BOOKMARK_ID_ARG = "id";
    const val TITLE_ARG = "title";
    const val FOLDER_ID_ARG = "folderId";
    const val PARENT_PATH_ARG = "parentPath";
}

object MaakMaiDestinations {
    const val BROWSE_ROUTE = "$BROWSE_SCREEN?$PATH_ARG={$PATH_ARG}"
    const val ADD_EDIT_BOOKMARK_ROUTE = "$ADD_EDIT_BOOKMARK_SCREEN/{$TITLE_ARG}?$BOOKMARK_ID_ARG={$BOOKMARK_ID_ARG}&$PATH_ARG={$PATH_ARG}"
    const val ADD_EDIT_FOLDER_ROUTE = "$ADD_EDIT_FOLDER_SCREEN/{$TITLE_ARG}?$FOLDER_ID_ARG={$FOLDER_ID_ARG}&$PARENT_PATH_ARG={$PARENT_PATH_ARG}"
}

class MaakMaiNavigationActions(private val navController: NavController) {
    private fun browseRoute(path: String): String {
        return "$BROWSE_SCREEN?$PATH_ARG=$path"
    }
    fun navigateToBrowse(path: String = "", currentRoute: String? = null) {
        navController.navigate(browseRoute(path), currentRoute?.let {
            NavOptions.Builder().setPopUpTo(it, true).build()
        } ?: NavOptions.Builder().build());
    }

    fun backToBrowseParent(path: String) {
        navController.popBackStack(browseRoute(path), true)
    }

    fun navigateToAdd(atPath: String? = null) {
        navController.navigate("$ADD_EDIT_BOOKMARK_SCREEN/Add Bookmark${atPath.let { "?$PATH_ARG=$atPath" }}")
    }

    fun navigateToEdit(bookmarkId: UUID) {
        navController.navigate("$ADD_EDIT_BOOKMARK_SCREEN/Edit Bookmark?$BOOKMARK_ID_ARG=$bookmarkId")
    }

    fun navigateToAddFolder(parentPath: String, parentId: UUID?) {
        navController.navigate("$ADD_EDIT_FOLDER_SCREEN/Add Folder?$PARENT_PATH_ARG=$parentPath")
    }

    fun navigateToEditFolder(folderId: UUID, parentPath: String, parentId: UUID?) {
        navController.navigate("$ADD_EDIT_FOLDER_SCREEN/Edit Folder?$FOLDER_ID_ARG=$folderId&$PARENT_PATH_ARG=$parentPath")
    }
}
