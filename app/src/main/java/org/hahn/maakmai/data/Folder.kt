package org.hahn.maakmai.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey
    val id: UUID,
    val tag: String,
    val parent: UUID?,
    @ColumnInfo(defaultValue = "")
    val tagGroups: List<String> = emptyList(),
    @ColumnInfo(defaultValue = "0xFF9E9E9E") // Default to grey
    val color: String = "0xFF9E9E9E"
)
