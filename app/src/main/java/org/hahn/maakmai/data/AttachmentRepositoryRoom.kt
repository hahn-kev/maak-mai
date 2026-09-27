package org.hahn.maakmai.data

import org.hahn.maakmai.data.source.local.AttachmentDao
import org.hahn.maakmai.model.Attachment
import org.hahn.maakmai.model.AttachmentInfo
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private const val CHUNK_SIZE = 512 * 1024

@Singleton
class AttachmentRepositoryRoom @Inject constructor(
    private val attachmentDao: AttachmentDao
) : AttachmentRepository {

    override suspend fun create(attachment: Attachment) {
        attachmentDao.insertAttachment(attachment)
    }

    override suspend fun getData(id: UUID): ByteArray? {
        val length = attachmentDao.getDataLength(id) ?: return null
        val out = ByteArrayOutputStream(length)
        var start = 1
        while (start <= length) {
            val chunk = attachmentDao.getDataChunk(id, start, CHUNK_SIZE) ?: return null
            if (chunk.isEmpty()) break
            out.write(chunk)
            start += chunk.size
        }
        return out.toByteArray()
    }

    override suspend fun getAllInfo(): List<AttachmentInfo> {
        return attachmentDao.getAttachmentInfos()
    }

    override suspend fun replaceData(id: UUID, data: ByteArray): Boolean {
        return attachmentDao.updateData(id, data) > 0
    }

    override suspend fun delete(id: UUID): Boolean {
        return attachmentDao.deleteAttachmentById(id) > 0
    }
}
