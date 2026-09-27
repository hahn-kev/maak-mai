package org.hahn.maakmai.data

import org.hahn.maakmai.model.Attachment
import org.hahn.maakmai.model.AttachmentInfo
import java.util.UUID

interface AttachmentRepository {
    suspend fun create(attachment: Attachment)

    /** The stored image bytes, or null if there is no such attachment. */
    suspend fun getData(id: UUID): ByteArray?

    suspend fun getAllInfo(): List<AttachmentInfo>

    suspend fun replaceData(id: UUID, data: ByteArray): Boolean

    suspend fun delete(id: UUID): Boolean
}
