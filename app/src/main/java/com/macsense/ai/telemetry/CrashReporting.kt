package com.macsense.ai.telemetry

import com.macsense.ai.BuildConfig

/**
 * Pluggable crash-reporting hook point, gated behind [BuildConfig.CRASH_REPORTING_ENABLED].
 *
 * This intentionally does NOT bundle a concrete vendor SDK (Crashlytics/Sentry) in this
 * commit: wiring a real vendor requires a human decision (which vendor, a `google-
 * services.json` or Sentry DSN, and a CI secret to inject it) that cannot be made safely
 * from code alone. Until that decision is made, [NoOpCrashReporter] keeps the app fully
 * functional and the flag simply controls whether [AppLogger.ERROR] events are also
 * forwarded to whatever [CrashReporter] is installed.
 *
 * To finish wiring a real vendor once a decision is made:
 * 1. Add the vendor SDK dependency + Gradle plugin (e.g. `com.google.gms.google-services`,
 *    already present in the version catalog as `google-services`).
 * 2. Implement [CrashReporter] with that SDK (e.g. `FirebaseCrashlytics.getInstance()...`).
 * 3. Replace the `NoOpCrashReporter()` default passed to [install] in
 *    `MacSenseApplication.onCreate()` with the real implementation, only when
 *    `BuildConfig.CRASH_REPORTING_ENABLED` is true.
 */
interface CrashReporter {
    fun recordException(throwable: Throwable, tag: String, message: String)
    fun log(message: String)
}

/** Default no-op implementation used until a concrete vendor SDK is wired in. */
class NoOpCrashReporter : CrashReporter {
    override fun recordException(throwable: Throwable, tag: String, message: String) {
        // Intentionally does nothing beyond what AppLogger already does; this exists so
        // call sites and tests have a stable, side-effect-free default.
    }

    override fun log(message: String) {
        // No-op.
    }
}

/**
 * Bridges [AppLogger] ERROR/WARN-with-throwable events to a [CrashReporter], when crash
 * reporting is enabled via [BuildConfig.CRASH_REPORTING_ENABLED]. Install once from
 * `MacSenseApplication.onCreate()`.
 */
class CrashReportingLogSink(
    private val reporter: CrashReporter,
    private val enabled: Boolean = BuildConfig.CRASH_REPORTING_ENABLED,
) : AppLogger.Sink {
    override fun log(level: AppLogger.Level, tag: String, message: String, throwable: Throwable?) {
        if (BuildConfig.DEBUG) {
            when (level) {
                AppLogger.Level.DEBUG -> println("D/$tag: $message")
                AppLogger.Level.INFO -> println("I/$tag: $message")
                AppLogger.Level.WARN -> println("W/$tag: $message${throwable?.let { " ($it)" } ?: ""}")
                AppLogger.Level.ERROR -> println("E/$tag: $message${throwable?.let { " ($it)" } ?: ""}")
            }
        }
        if (!enabled) return
        if (throwable != null && (level == AppLogger.Level.ERROR || level == AppLogger.Level.WARN)) {
            reporter.recordException(throwable, tag, message)
        } else if (level == AppLogger.Level.ERROR) {
            reporter.log("[$tag] $message")
        }
    }
}

/**
 * Installs crash reporting into [AppLogger] if [BuildConfig.CRASH_REPORTING_ENABLED] is set,
 * otherwise leaves [AppLogger] on its default Logcat/println behavior. Safe to call
 * unconditionally from `Application.onCreate()`.
 */
object CrashReportingInstaller {
    fun installIfEnabled(reporter: CrashReporter = NoOpCrashReporter()) {
        if (!BuildConfig.CRASH_REPORTING_ENABLED) {
            AppLogger.i("CrashReporting", "Crash reporting disabled via BuildConfig flag.")
            return
        }
        AppLogger.install(CrashReportingLogSink(reporter))
        AppLogger.i("CrashReporting", "Crash reporting sink installed (reporter=${reporter::class.simpleName}).")
    }
}
