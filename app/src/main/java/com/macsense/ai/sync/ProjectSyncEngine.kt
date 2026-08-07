package com.macsense.ai.sync

import com.macsense.ai.data.local.MacSenseDao
import com.macsense.ai.telemetry.AppLogger

/**
 * P8 (issue #43): offline-first project sync engine.
 *
 * Room is the source of truth; Supabase is a durable best-effort backup. The engine only
 * ever touches dirty rows, resolves conflicts last-write-wins on `updatedAt` (logging every
 * conflict for the future merge UI), and leaves rows dirty when the network fails so no
 * change is ever lost — the next run retries.
 */
class ProjectSyncEngine(
    private val dao: MacSenseDao,
    private val remote: SupabaseSyncRemote,
    private val now: () -> Long = { System.currentTimeMillis() },
) {

    data class Result(
        val uploaded: Int,
        val skippedConflicts: Int,
        val failed: Int,
    )

    /** Pushes all dirty projects to the cloud. Never throws; failures stay dirty. */
    suspend fun syncDirtyProjects(): Result {
        val dirty = dao.getDirtyProjects()
        var uploaded = 0
        var conflicts = 0
        var failed = 0
        for (project in dirty) {
            try {
                val cloud = remote.fetchProject(project.id)
                if (cloud != null && cloud.updatedAtMs > project.updatedAt) {
                    // Last-write-wins: cloud copy is newer; keep it, log for future merge UI.
                    AppLogger.w(
                        "ProjectSyncEngine",
                        "sync_conflict project=${project.id} local=${project.updatedAt} cloud=${cloud.updatedAtMs} -> cloud wins"
                    )
                    dao.markProjectSynced(project.id, cloud.id ?: project.id, now())
                    conflicts++
                    continue
                }
                val stored = remote.upsertProject(CloudProject.fromEntity(project))
                dao.markProjectSynced(project.id, stored.id ?: error("cloud id missing"), now())
                uploaded++
            } catch (e: Exception) {
                // Offline-first guarantee: swallow, stay dirty, retry next run.
                AppLogger.w("ProjectSyncEngine", "sync_failed project=${project.id}: ${e.message}")
                failed++
            }
        }
        return Result(uploaded, conflicts, failed)
    }
}
