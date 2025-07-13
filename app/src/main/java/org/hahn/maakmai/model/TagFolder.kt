package org.hahn.maakmai.model

data class TagFolder(val tag: String, val children: List<String>, val rootFolder: Boolean = false)
