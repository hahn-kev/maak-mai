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

val DefaultColor = Color(0xff9986B1)

// List of all folder colors for the picker
val FolderColors = listOf(
    Color(0xffe9be11),
    Color(0xff9D4379),
    Color(0xff3475a4),
    Color(0xff834c7a),
    DefaultColor,
    Color(0xff2b7356),
    Color(0xffEDD669),
    Color(0xffbf789e),
    Color(0xff819b55),

)

@OptIn(ExperimentalStdlibApi::class)
val DefaultFolderColorStr = DefaultColor.toArgb().toHexString()