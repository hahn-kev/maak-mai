package org.hahn.maakmai.model

import java.util.UUID
import java.util.stream.Stream

data class TagFolder(val id: UUID, val tag: String, val children: List<TagFolder>, val rootFolder: Boolean = false) {
    fun findFolder(path: String): TagFolder?{
        return findFolderInternal(path.split("/"))
    }

    private fun findFolderInternal(pathParts: List<String>, startIndex: Int = 0): TagFolder? {
        if (pathParts.size <= startIndex) {
            return this
        }

        val currentPart = pathParts[startIndex]
        val child = children.firstOrNull { f -> f.tag == currentPart }

        return child?.findFolderInternal(pathParts, startIndex + 1)
    }
}
