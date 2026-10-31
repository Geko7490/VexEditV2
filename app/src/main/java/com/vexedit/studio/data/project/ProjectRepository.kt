package com.vexedit.studio.data.project

import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ProjectRepository(private val dao: ProjectDao) {
    fun observeProjects(): Flow<List<ProjectEntity>> = dao.observeActiveProjects()
    fun observeTrash(): Flow<List<ProjectEntity>> = dao.observeTrash()

    suspend fun create(name: String, aspectRatio: String, resolution: String, fps: Int): ProjectEntity {
        val now = System.currentTimeMillis()
        val project = ProjectEntity(
            id = UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "Untitled Project" },
            aspectRatio = aspectRatio,
            resolution = resolution,
            fps = fps,
            createdAt = now,
            updatedAt = now
        )
        dao.upsert(project)
        return project
    }

    suspend fun rename(id: String, name: String) {
        val cleanName = name.trim()
        if (cleanName.isNotEmpty()) dao.rename(id, cleanName, System.currentTimeMillis())
    }

    suspend fun duplicate(id: String) {
        val source = dao.findById(id) ?: return
        val now = System.currentTimeMillis()
        dao.upsert(source.copy(id = UUID.randomUUID().toString(), name = "${source.name} Copy", createdAt = now, updatedAt = now, isTrashed = false))
    }

    suspend fun moveToTrash(id: String) = dao.moveToTrash(id, System.currentTimeMillis())
    suspend fun restore(id: String) = dao.restore(id, System.currentTimeMillis())
    suspend fun permanentlyDelete(id: String) = dao.permanentlyDelete(id)
}
