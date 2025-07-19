package org.hahn.maakmai.model

import java.util.UUID

data class Attachment(
    val id: UUID,
    val data: ByteArray,
    val title: String?,
)
