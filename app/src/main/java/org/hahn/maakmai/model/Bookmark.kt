package org.hahn.maakmai.model

data class Bookmark(
    val title: String,
    val description: String,
    val url: String?,
    val tags: List<String>,
)
