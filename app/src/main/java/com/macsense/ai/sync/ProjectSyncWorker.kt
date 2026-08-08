package com.macsense.ai.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.macsense.ai.BuildConfig
import com.macsense.ai.data.local.AppContainer
import com.macsense.ai.telemetry.AppLogger
import java.util.concurrent.TimeUnit

/**
 * WorkManager worker that drives both project sync and collaboration sync.
 *
 * Schedules two work items:
 * - "project_sync": runs every 15 min (offline-first project push)
 * - "collab_sync":  runs every 30 min (comments, shares, branches fetch)
 *
 * Both are no-ops when SUPABASE_URL / SUPABASE_ANON_KEY are blank (offline builds).
 */
class ProjectSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_ANON_KEY

        if (url.isBlank() || key.isBlank()) {
            AppLogger.d("ProjectSyncWorker", "Supabase credentials absent — skipping sync (offline build)")
            return Result.success()
        }

        return try {
            val db = AppContainer.getDatabase(applicationContext)
            val remote = SupabasePostgrestRemote(baseUrl = url, apiKey = key)
            val engine = ProjectSyncEngine(dao = db.macSenseDao(), remote = remote)
            val result = engine.syncDirtyProjects()
            AppLogger.i("ProjectSyncWorker",
                "Sync complete: uploaded=${result.uploaded} conflicts=${result.skippedConflicts} failed=${result.failed}")
            Result.success()
        } catch (e: Exception) {
            AppLogger.w("ProjectSyncWorker", "Sync worker error: ${e.message}")
            // Retry up to WorkManager's default back-off (30s doubling, capped at 5h)
            Result.retry()
        }
    }

    companion object {
        const val PROJECT_SYNC_TAG = "project_sync"

        /**
         * Schedules the periodic project sync. Safe to call on every app start;
         * KEEP policy means it won't reset the timer if already scheduled.
         */
        fun schedule(context: Context) {
            val url = BuildConfig.SUPABASE_URL
            val key = BuildConfig.SUPABASE_ANON_KEY
            if (url.isBlank() || key.isBlank()) return // offline build — don't schedule

            val request = PeriodicWorkRequestBuilder<ProjectSyncWorker>(15, TimeUnit.MINUTES)
                .addTag(PROJECT_SYNC_TAG)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PROJECT_SYNC_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            AppLogger.i("ProjectSyncWorker", "Sync scheduled (15 min period)")
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(PROJECT_SYNC_TAG)
        }
    }
}
