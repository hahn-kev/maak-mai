package org.hahn.maakmai.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Material Design Colors for folder color picker
val FolderRed = Color(0xFFF44336)
val FolderPink = Color(0xFFE91E63)
val FolderPurple = Color(0xFF9C27B0)
val FolderDeepPurple = Color(0xFF673AB7)
val FolderIndigo = Color(0xFF3F51B5)
val FolderBlue = Color(0xFF2196F3)
val FolderLightBlue = Color(0xFF03A9F4)
val FolderCyan = Color(0xFF00BCD4)
val FolderTeal = Color(0xFF009688)
val FolderGreen = Color(0xFF4CAF50)
val FolderLightGreen = Color(0xFF8BC34A)
val FolderLime = Color(0xFFCDDC39)
val FolderYellow = Color(0xFFFFEB3B)
val FolderAmber = Color(0xFFFFC107)
val FolderOrange = Color(0xFFFF9800)
val FolderDeepOrange = Color(0xFFFF5722)
val FolderBrown = Color(0xFF795548)
val FolderGrey = Color(0xFF9E9E9E)
val FolderBlueGrey = Color(0xFF607D8B)

// List of all folder colors for the picker
val FolderColors = listOf(
    FolderRed, FolderPink, FolderPurple, FolderDeepPurple, FolderIndigo,
    FolderBlue, FolderLightBlue, FolderCyan, FolderTeal, FolderGreen,
    FolderLightGreen, FolderLime, FolderYellow, FolderAmber, FolderOrange,
    FolderDeepOrange, FolderBrown, FolderGrey, FolderBlueGrey
)

@OptIn(ExperimentalStdlibApi::class)
val DefaultFolderColorStr = FolderGrey.toArgb().toHexString()