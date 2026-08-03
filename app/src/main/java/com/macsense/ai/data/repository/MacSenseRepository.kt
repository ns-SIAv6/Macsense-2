package com.macsense.ai.data.repository

import com.macsense.ai.data.local.MacSenseDao
import com.macsense.ai.data.local.ProjectEntity
import kotlinx.coroutines.flow.Flow

class MacSenseRepository(private val dao: MacSenseDao) {
    fun getAllProjects(): Flow<List<ProjectEntity>> = dao.getAllProjects()
    
    suspend fun insertProject(project: ProjectEntity) {
        dao.insertProject(project)
    }
}
