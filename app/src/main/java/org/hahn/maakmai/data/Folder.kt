package org.hahn.maakmai.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "folders")
data class Folder(
    @PrimaryKey
    val id: UUID,
    val tag: String,
    val parent: UUID?
)
