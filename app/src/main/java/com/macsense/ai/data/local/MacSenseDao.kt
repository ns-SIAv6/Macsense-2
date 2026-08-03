package com.macsense.ai.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MacSenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Query("SELECT * FROM projects ORDER BY updated_at DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>
    
    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProject(id: String)
}
