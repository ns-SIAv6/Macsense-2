package com.macsense.ai.sync

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectSyncWorkerContractTest {

    @After
    fun resetRemoteFactory() {
        ProjectSyncWorker.remoteFactory = null
    }

    @Test
    fun `cloud sync is explicitly unavailable without a configured remote`() {
        ProjectSyncWorker.remoteFactory = null

        assertFalse(ProjectSyncWorker.isCloudSyncConfigured())
    }

    @Test
    fun `cloud sync is available only with a configured remote`() {
        ProjectSyncWorker.remoteFactory = { object : SupabaseSyncRemote {
            override suspend fun upsertProject(project: CloudProject): CloudProject = project
            override suspend fun fetchProject(localId: String): CloudProject? = null
        } }

        assertTrue(ProjectSyncWorker.isCloudSyncConfigured())
    }
}