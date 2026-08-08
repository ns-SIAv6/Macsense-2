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

 // Fail loud-but-safe: clear diagnostic if required config is missing.
 StartupValidator.runAll()

 // Wire Supabase remote when credentials are configured via .env / CI secrets.
 // If SUPABASE_URL or SUPABASE_ANON_KEY are blank, the factory stays null and
 // sync runs become benign no-ops (offline-first build).
 val supabaseUrl = BuildConfig.SUPABASE_URL
 val supabaseKey = BuildConfig.SUPABASE_ANON_KEY
 if (supabaseUrl.isNotBlank() && supabaseKey.isNotBlank()) {
 ProjectSyncWorker.remoteFactory = {
 SupabasePostgrestRemote(baseUrl = supabaseUrl, apiKey = supabaseKey)
 }
 AppLogger.i("MacSenseApplication", "Supabase sync remote configured.")
 } else {
 AppLogger.i("MacSenseApplication", "SUPABASE_URL/ANON_KEY not set — offline-only mode.")
 }

 // Schedule background sync (Wi-Fi only, battery-friendly, 6-hour interval).
 // Safe to call unconditionally: WorkManager's KEEP policy prevents duplicates.
 ProjectSyncWorker.schedule(this)
 }
}
