package com.vexedit.studio.ui.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.vexedit.studio.data.project.ProjectEntity
import com.vexedit.studio.data.project.ProjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectViewModel(private val repository: ProjectRepository) : ViewModel() {
    val projects = repository.observeProjects().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val trash = repository.observeTrash().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun create(name: String, ratio: String, resolution: String, fps: Int, onCreated: (ProjectEntity) -> Unit = {}) {
        viewModelScope.launch { onCreated(repository.create(name, ratio, resolution, fps)) }
    }

    fun rename(id: String, name: String) = viewModelScope.launch { repository.rename(id, name) }
    fun duplicate(id: String) = viewModelScope.launch { repository.duplicate(id) }
    fun trash(id: String) = viewModelScope.launch { repository.moveToTrash(id) }
    fun restore(id: String) = viewModelScope.launch { repository.restore(id) }
    fun deleteForever(id: String) = viewModelScope.launch { repository.permanentlyDelete(id) }

    class Factory(private val repository: ProjectRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProjectViewModel::class.java)) {
                return ProjectViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
