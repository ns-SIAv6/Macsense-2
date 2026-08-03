package com.macsense.ai.repository

import com.macsense.ai.data.local.MacSenseDao
import com.macsense.ai.data.local.ProjectEntity
import com.macsense.ai.data.repository.MacSenseRepository
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class MacSenseRepositoryTest {
    @Test
    fun testRepository() = runBlocking {
        val dao = object : MacSenseDao {
            val items = mutableListOf<ProjectEntity>()
            override suspend fun insertProject(project: ProjectEntity) { items.add(project) }
            override suspend fun getProjectById(id: String) = items.find { it.id == id }
            override fun getAllProjects() = flowOf(items)
            override suspend fun deleteProject(id: String) { items.removeIf { it.id == id } }
        }
        val repo = MacSenseRepository(dao)
        repo.insertProject(ProjectEntity("1", "test", 0L, 0L, 120.0))
        var count = 0
        repo.getAllProjects().collect { count = it.size }
        assertEquals(1, count)
    }
}
