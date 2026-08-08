package com.macsense.ai.telemetry

import android.content.Context
import com.macsense.ai.BuildConfig
import io.sentry.Sentry
import io.sentry.SentryLevel
import io.sentry.android.core.SentryAndroid
import io.sentry.protocol.User

/**
 * Sentry Android crash reporter implementation for [CrashReporter].
 *
 * Activated only when [BuildConfig.CRASH_REPORTING_ENABLED] is true (release builds).
 * Call [init] once from [MacSenseApplication.onCreate] before any other app code runs.
 *
 * DSN is injected via BuildConfig.SENTRY_DSN — add your real DSN to .env:
 *   SENTRY_DSN=https://your-key@sentry.io/project-id
 *
 * Features enabled:
 * - Automatic session tracking
 * - Full breadcrumb trail for AI call + DAW event paths
 * - Performance tracing at 10% sample rate (configurable per environment)
 * - User scope cleared on sign-out
 * - Debug mode on in DEBUG builds so Sentry logs are visible in Logcat
 */
class SentryCrashReporter(private val context: Context) : CrashReporter {

 fun init() {
 if (BuildConfig.SENTRY_DSN.isBlank()) {
 AppLogger.w("SentryReporter", "SENTRY_DSN is blank — crash reporting disabled even though flag is on")
 return
 }
 SentryAndroid.init(context) { options ->
 options.dsn = BuildConfig.SENTRY_DSN
 options.environment = if (BuildConfig.DEBUG) "debug" else "production"
 options.release = "${BuildConfig.APPLICATION_ID}@${BuildConfig.VERSION_NAME}+${BuildConfig.VERSION_CODE}"
 options.isEnableAutoSessionTracking = true
 // Screenshots can contain sensitive project/lyric data — disabled for privacy
 options.isAttachScreenshot = false
 options.isAttachViewHierarchy = false
 // Capture 100% of errors, 10% of performance traces
 options.sampleRate = 1.0
 options.tracesSampleRate = 0.1
 // Let existing AppLogger output in debug; Sentry adds its own Logcat integration
 options.isDebug = BuildConfig.DEBUG
 // Attach ANR detection
 options.isAnrEnabled = true
 options.anrTimeoutIntervalMillis = 5000L
 // Keep breadcrumb trail large enough to trace a full Ari command lifecycle
 options.maxBreadcrumbs = 200
 }
 AppLogger.i("SentryReporter", "Sentry initialized (env=${if (BuildConfig.DEBUG) "debug" else "production"})")
 }

 override fun recordException(throwable: Throwable, tag: String, message: String) {
 Sentry.withScope { scope ->
 scope.setTag("logger_tag", tag)
 scope.addBreadcrumb(io.sentry.Breadcrumb.info("[$tag] $message"))
 Sentry.captureException(throwable)
 }
 }

 override fun log(message: String) {
 Sentry.addBreadcrumb(message)
 Sentry.captureMessage(message, SentryLevel.ERROR)
 }

 /** Call when a user project is loaded to attach project context to all subsequent events. */
 fun setProjectContext(projectId: String, projectName: String) {
 Sentry.withScope { scope ->
 scope.setTag("project_id", projectId)
 scope.setTag("project_name", projectName)
 }
 }

 /** Attach a pseudonymous user id (never name/email) so session counts are accurate. */
 fun setAnonymousUser(userId: String) {
 Sentry.setUser(User().apply { id = userId })
 }

 /** Clear user context on sign-out / project close. */
 fun clearUser() {
 Sentry.setUser(null)
 }

 companion object {
 /**
 * Convenience wrapper: creates, inits, and returns a [SentryCrashReporter] in one call.
 * Returns null if the DSN is blank so callers can fall back to [NoOpCrashReporter].
 */
 fun createAndInit(context: Context): SentryCrashReporter? {
 if (BuildConfig.SENTRY_DSN.isBlank()) return null
 val reporter = SentryCrashReporter(context)
 reporter.init()
 return reporter
 }
 }
}
