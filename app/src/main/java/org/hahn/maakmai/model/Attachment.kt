package org.hahn.maakmai.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "attachments")
data class Attachment(
    @PrimaryKey
    val id: UUID,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    val data: ByteArray,
    val title: String?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Attachment) return false

        return id == other.id &&
                data.contentEquals(other.data) &&
                title == other.title
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        return result
    }
}
