package org.hahn.maakmai.data

import java.util.UUID

data class Folder(val id: UUID, val tag: String, val parent: UUID?)
