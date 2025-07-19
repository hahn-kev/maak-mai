package org.hahn.maakmai.model

import java.util.UUID
import java.util.stream.Stream

data class TagFolder(val id: UUID, val tag: String, val children: List<TagFolder>, val rootFolder: Boolean = false, val tagGroups: List<String> = emptyList()) {
    fun findFolder(path: String): TagFolder? {
        if (path == "/") {
            return this
        }
        return findFolderInternal(path.split("/").filter { f -> f.isNotEmpty() })
    }

    private fun findFolderInternal(pathParts: List<String>, startIndex: Int = 0): TagFolder? {
        if (pathParts.size <= startIndex) {
            return this
        }

        val currentPart = pathParts[startIndex]
        val child = children.firstOrNull { f -> f.tag == currentPart }

        return child?.findFolderInternal(pathParts, startIndex + 1)
    }

    fun findFolders(path: String): List<TagFolder> {
        if (path == "/") {
            return emptyList()
        }
        return findFoldersInternal(path.split("/").filter { f -> f.isNotEmpty() })?.asReversed()?.filter { it.tag != "/" } ?: emptyList()
    }
    private fun findFoldersInternal(pathParts: List<String>, startIndex: Int = 0): List<TagFolder>? {
        if (pathParts.size <= startIndex) {
            return listOf(this)
        }

        val currentPart = pathParts[startIndex]
        val child = children.firstOrNull { f -> f.tag == currentPart }

        return child?.findFoldersInternal(pathParts, startIndex + 1)?.let { it + this }
    }
}
