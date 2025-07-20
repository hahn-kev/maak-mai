package org.hahn.maakmai.data

import kotlinx.coroutines.flow.Flow
import org.hahn.maakmai.data.source.local.AttachmentDao
import org.hahn.maakmai.model.Attachment
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AttachmentRepositoryRoom @Inject constructor(
    private val attachmentDao: AttachmentDao
) : AttachmentRepository {

    override suspend fun create(attachment: Attachment) {
        attachmentDao.insertAttachment(attachment)
    }

    override suspend fun get(id: UUID): Attachment? {
        return attachmentDao.getAttachmentById(id)
    }

    override fun getStream(id: UUID): Flow<Attachment> {
        return attachmentDao.getAttachmentStream(id)
    }

    override suspend fun update(id: UUID, attachment: Attachment): Boolean {
        // Ensure the attachment has the correct ID
        val updatedAttachment = attachment.copy(id = id)
        return attachmentDao.updateAttachment(updatedAttachment) > 0
    }

    override suspend fun delete(id: UUID): Boolean {
        return attachmentDao.deleteAttachmentById(id) > 0
    }
}