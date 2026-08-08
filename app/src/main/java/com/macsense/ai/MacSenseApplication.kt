package com.macsense.ai

import android.app.Application
import com.macsense.ai.di.AppContainer
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
 }
}
