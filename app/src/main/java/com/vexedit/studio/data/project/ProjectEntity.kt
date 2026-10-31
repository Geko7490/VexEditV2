package com.vexedit.studio.data.project

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val aspectRatio: String,
    val resolution: String,
    val fps: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val isTrashed: Boolean = false
)
