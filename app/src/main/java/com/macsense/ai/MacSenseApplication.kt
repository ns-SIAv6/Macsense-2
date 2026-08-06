package com.macsense.ai

import android.app.Application
import com.macsense.ai.di.AppContainer
import com.macsense.ai.telemetry.CrashReportingInstaller
import com.macsense.ai.telemetry.StartupValidator

class MacSenseApplication : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Installs the crash-reporting sink (currently a no-op until a vendor is chosen,
        // see CrashReporting.kt) before any other startup logging happens, so nothing is
        // ever missed once a real vendor is wired in behind CRASH_REPORTING_ENABLED.
        CrashReportingInstaller.installIfEnabled()
        // Fail loud-but-safe: log a clear diagnostic immediately if required config is
        // missing, rather than letting the user discover it via a cryptic network
        // exception the first time they message Ari.
        StartupValidator.runAll()
    }
}
