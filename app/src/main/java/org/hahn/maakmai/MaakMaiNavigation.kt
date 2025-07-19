package org.hahn.maakmai

import androidx.navigation.NavController
import org.hahn.maakmai.MaakMaiArgs.PATH_ARG
import org.hahn.maakmai.MaakMaiScreens.BROWSE_SCREEN

private object MaakMaiScreens {
    const val BROWSE_SCREEN = "browse"
}

object MaakMaiArgs {
    const val PATH_ARG = "path";
}

object MaakMaiDestinations {
    const val BROWSE_ROUTE = "$BROWSE_SCREEN/{$PATH_ARG}";
}

class MaakMaiNavigationActions(private val navController: NavController) {
    fun navigateToBrowse(path: String = "") {
        navController.navigate("$BROWSE_SCREEN/$path");
    }

}