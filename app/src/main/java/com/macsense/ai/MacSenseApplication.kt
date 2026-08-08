package com.macsense.ai

import android.app.Application
import com.macsense.ai.BuildConfig
import com.macsense.ai.di.AppContainer
import com.macsense.ai.sync.ProjectSyncWorker
import com.macsense.ai.sync.SupabasePostgrestRemote
import com.macsense.ai.telemetry.AppLogger
import com.macsense.ai.telemetry.CrashReportingInstaller
import com.macsense.ai.telemetry.NoOpCrashReporter
import com.macsense.ai.telemetry.SentryCrashReporter
import com.macsense.ai.telemetry.StartupValidator

class MacSenseApplication : Application() {
    lateinit var container: AppContainer

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)

        // Wire Sentry if enabled; fall back to NoOpCrashReporter so the app never crashes
        // because of missing crash-reporting config.
        val crashReporter = if (BuildConfig.CRASH_REPORTING_ENABLED) {
            SentryCrashReporter.createAndInit(this) ?: run {
                AppLogger.w("MacSenseApplication", "SentryCrashReporter init returned null (blank DSN?). Using NoOp.")
                NoOpCrashReporter()
            }
        } else {
            NoOpCrashReporter()
        }
        CrashReportingInstaller.installIfEnabled(crashReporter)

        // Fail loud-but-safe: log a clear diagnostic immediately if required config is
        // missing, rather than letting the user discover it via a cryptic network
        // exception the first time they message Ari.
        StartupValidator.runAll()

        // Cloud backup is only available when the full Supabase configuration validates:
        // HTTPS project URL, public anon key, and a real authenticated user token. Anything
        // less keeps the installation local-only rather than pretending sync works.
        val supabase = StartupValidator.validateSupabase(
            BuildConfig.SUPABASE_URL,
            BuildConfig.SUPABASE_ANON_KEY,
            BuildConfig.SUPABASE_ACCESS_TOKEN,
        )
        ProjectSyncWorker.remoteFactory = if (supabase.isConfigured) {
            {
                SupabasePostgrestRemote(
                    baseUrl = requireNotNull(supabase.baseUrl),
                    apiKey = requireNotNull(supabase.anonKey),
                    userAccessToken = requireNotNull(supabase.userAccessToken),
                )
            }
        } else {
            null
        }
        if (supabase.isConfigured) {
            AppLogger.i("MacSenseApplication", "Supabase sync remote configured.")
        } else {
            AppLogger.i("MacSenseApplication", "Supabase not fully configured — local-only mode; cloud backup unavailable.")
        }

        // Do not enqueue an empty "backup" job in local-only installations. A real cloud
        // remote must be configured and validated before WorkManager can schedule sync work.
        ProjectSyncWorker.scheduleIfConfigured(this)
    }
}
