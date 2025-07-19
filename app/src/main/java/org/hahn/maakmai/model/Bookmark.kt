package org.hahn.maakmai.model

import java.util.UUID

data class Bookmark(
    val id: UUID,
    val title: String,
    val description: String,
    val url: String?,
    val tags: List<String>,
)
