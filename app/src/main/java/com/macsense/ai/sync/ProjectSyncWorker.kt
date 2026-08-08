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
 * Background project backup via WorkManager. It is only scheduled when a verified cloud remote
 * has been configured. Room remains the source of truth; a failed upload leaves a project dirty
 * so WorkManager can retry rather than reporting a fictional successful backup.
 */
class ProjectSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val remote = remoteFactory?.invoke()
        if (remote == null) {
            AppLogger.w(TAG, "Cloud sync is not configured; refusing to report a successful backup")
            return Result.failure()
        }
        val engine = ProjectSyncEngine(AppContainer(applicationContext).database.dao(), remote)
        val result = engine.syncDirtyProjects()
        AppLogger.i(TAG, "sync run: uploaded=${result.uploaded} conflicts=${result.skippedConflicts} failed=${result.failed}")
        return if (result.failed == 0) Result.success() else Result.retry()
    }

    companion object {
        private const val TAG = "ProjectSyncWorker"
        const val PROJECT_SYNC_TAG = "project_sync"

        /**
         * Supplies the verified cloud remote when configured at startup. A null factory means
         * this installation is local-only and must never advertise cloud backup as available.
         */
        @Volatile
        var remoteFactory: (() -> SupabaseSyncRemote)? = null

        /** Public capability signal for UI and startup wiring; never infer this from a no-op run. */
        fun isCloudSyncConfigured(): Boolean = remoteFactory != null

        /** Schedules periodic Wi-Fi-only background sync only when a remote is configured. */
        fun scheduleIfConfigured(context: Context): Boolean {
            if (!isCloudSyncConfigured()) {
                AppLogger.i(TAG, "Cloud sync unavailable: no verified remote configuration")
                return false
            }
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.UNMETERED)
                .setRequiresBatteryNotLow(true)
                .build()
            val request = PeriodicWorkRequestBuilder<ProjectSyncWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .addTag(PROJECT_SYNC_TAG)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PROJECT_SYNC_TAG,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            return true
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag(PROJECT_SYNC_TAG)
        }
    }
}
