package com.macsense.ai.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.macsense.ai.di.AppContainer
import com.macsense.ai.telemetry.AppLogger
import java.util.concurrent.TimeUnit

/**
 * P8 (issue #43): background sync via WorkManager — unmetered network + charging-friendly
 * idle constraints; Room stays source of truth and the engine never throws, so the worker
 * only retries on unexpected infrastructure errors.
 */
class ProjectSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val remote = remoteFactory?.invoke()
        if (remote == null) {
            AppLogger.i(TAG, "No Supabase credentials configured; skipping sync run")
            return Result.success()
        }
        val engine = ProjectSyncEngine(AppContainer(applicationContext).database.dao(), remote)
        val result = engine.syncDirtyProjects()
        AppLogger.i(TAG, "sync run: uploaded=${result.uploaded} conflicts=${result.skippedConflicts} failed=${result.failed}")
        return Result.success()
    }

    companion object {
        private const val TAG = "ProjectSyncWorker"

        /**
         * Supplies the Supabase remote when credentials are configured (set at app startup
         * from BuildConfig/.env). Null = offline-only build; sync runs become no-ops.
         */
        @Volatile
        var remoteFactory: (() -> SupabaseSyncRemote)? = null
        private const val WORK_NAME = "project_cloud_sync"

        /** Schedules periodic Wi-Fi-only background sync (offline-first: best effort). */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<ProjectSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
