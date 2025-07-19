package org.hahn.maakmai.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey
    val id: UUID,
    val title: String,
    val description: String,
    val url: String?,
    val tags: List<String>,
)
