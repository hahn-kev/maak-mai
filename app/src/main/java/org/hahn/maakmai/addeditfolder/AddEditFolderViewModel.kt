package org.hahn.maakmai.addeditfolder

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.hahn.maakmai.MaakMaiArgs
import org.hahn.maakmai.data.Folder
import org.hahn.maakmai.data.FolderRepository
import org.hahn.maakmai.model.TagFolder
import java.util.UUID
import javax.inject.Inject

data class AddEditFolderUiState(
    val tag: String = "",
    val parentPath: String = "/",
    val isLoading: Boolean = false,
    val isFolderSaved: Boolean = false
)

@HiltViewModel
class AddEditFolderViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val folderRepository: FolderRepository
) : ViewModel() {
    private val folderId: UUID? = savedStateHandle[MaakMaiArgs.FOLDER_ID_ARG]
    private val parentPath: String = savedStateHandle[MaakMaiArgs.PARENT_PATH_ARG] ?: "/"
    private var parentId: UUID? = null

    private val _uiState = MutableStateFlow(AddEditFolderUiState(parentPath = parentPath))
    val uiState = _uiState.asStateFlow()

    init {
        if (folderId != null) {
            loadFolder(folderId)
        } else {
            determineParentId()
        }
    }

    private fun determineParentId() {
        viewModelScope.launch {
            val folders = folderRepository.getAllFolders()
            if (folders.isNotEmpty() && parentPath != "/") {
                parentId = TagFolder(tag = "root", children = folders, id = UUID.randomUUID()).findFolder(parentPath)?.id
            }
        }
    }

    private fun loadFolder(folderId: UUID) {
        _uiState.update {
            it.copy(
                isLoading = true
            )
        }
        viewModelScope.launch {
            val allFolders = folderRepository.getAllFolders()
            val folder = allFolders.find { it.id == folderId }

            if (folder != null) {
                // When editing an existing folder, we need to determine the parent ID
                // from the parent path if it's not the root folder
                if (parentPath != "/") {
                    parentId = TagFolder(tag = "root", children = allFolders, id = UUID.randomUUID()).findFolder(parentPath)?.id
                }

                _uiState.update {
                    it.copy(
                        tag = folder.tag,
                        isLoading = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false
                    )
                }
            }
        }
    }

    fun updateTag(newTag: String) {
        _uiState.update {
            it.copy(tag = newTag)
        }
    }

    fun saveFolder() {
        if (uiState.value.tag.isBlank()) {
            return
        }

        viewModelScope.launch {

            val folder = Folder(
                id = folderId ?: UUID.randomUUID(),
                tag = uiState.value.tag,
                parent = parentId
            )

            val result = if (folderId == null) {
                folderRepository.createFolder(folder)
            } else {
                folderRepository.updateFolder(folder)
            }

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        isFolderSaved = true
                    )
                }
            }
        }
    }
}
