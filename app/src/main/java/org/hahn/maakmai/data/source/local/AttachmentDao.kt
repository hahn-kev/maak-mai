package org.hahn.maakmai.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import org.hahn.maakmai.model.Attachment
import java.util.UUID

@Dao
interface AttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: Attachment)

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getAttachmentById(id: UUID): Attachment?

    @Query("SELECT * FROM attachments WHERE id = :id")
    fun getAttachmentStream(id: UUID): Flow<Attachment>

    @Update
    suspend fun updateAttachment(attachment: Attachment): Int

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteAttachmentById(id: UUID): Int
}