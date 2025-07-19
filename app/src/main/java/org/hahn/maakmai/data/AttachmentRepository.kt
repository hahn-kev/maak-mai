package org.hahn.maakmai.data

import kotlinx.coroutines.flow.Flow
import org.hahn.maakmai.model.Attachment
import java.util.UUID
import kotlin.uuid.Uuid

interface AttachmentRepository {
    suspend fun create(attachment: Attachment)

    suspend fun get(id: UUID): Attachment?

    fun getStream(id: UUID): Flow<Attachment>

    suspend fun update(id: UUID, attachment: Attachment): Boolean

    suspend fun delete(id: UUID): Boolean
}