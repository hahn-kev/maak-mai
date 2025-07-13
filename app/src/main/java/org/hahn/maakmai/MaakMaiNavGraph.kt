package org.hahn.maakmai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.CoroutineScope
import org.hahn.maakmai.MaakMaiArgs.PATH_ARG
import org.hahn.maakmai.browse.BrowseScreen

@Composable
fun MaakMaiNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    startDestination: String = MaakMaiDestinations.BROWSE_ROUTE,
    navActions: MaakMaiNavigationActions = remember(navController) {
        MaakMaiNavigationActions(navController)
    }
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(
            MaakMaiDestinations.BROWSE_ROUTE,
            arguments = listOf(
                navArgument(PATH_ARG) { type = NavType.StringType; defaultValue = "" }
            )
        ) {
            BrowseScreen(
//                path = entry.arguments?.getString(PATH_ARG) ?: "",
//                onFolderClick = { path -> navActions.navigateToBrowse(path) },
//                onFileClick = {}
            )
        }
    }
}