package org.hahn.maakmai.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.hahn.maakmai.model.Attachment
import org.hahn.maakmai.model.AttachmentInfo
import java.util.UUID

/**
 * Image data is read in chunks: Android can't return a row larger than about 2 MB,
 * and images saved before resizing was added can exceed that.
 */
@Dao
interface AttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: Attachment)

    @Query("SELECT length(data) FROM attachments WHERE id = :id")
    suspend fun getDataLength(id: UUID): Int?

    /** [start] is 1-based, as in SQLite's substr. */
    @Query("SELECT substr(data, :start, :length) FROM attachments WHERE id = :id")
    suspend fun getDataChunk(id: UUID, start: Int, length: Int): ByteArray?

    @Query("SELECT id, length(data) AS size FROM attachments")
    suspend fun getAttachmentInfos(): List<AttachmentInfo>

    @Query("UPDATE attachments SET data = :data WHERE id = :id")
    suspend fun updateData(id: UUID, data: ByteArray): Int

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteAttachmentById(id: UUID): Int
}
