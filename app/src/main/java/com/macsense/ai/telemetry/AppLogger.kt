package com.macsense.ai.telemetry

import com.macsense.ai.BuildConfig

/**
 * Structured logging facade for the app. In debug builds this writes to Logcat via
 * android.util.Log. In release builds it is a hook point where a crash/telemetry SDK
 * (e.g. Crashlytics, Sentry) can be wired in without touching call sites throughout the
 * codebase. Centralizing logging here also guarantees no call site can accidentally leak
 * a raw secret, since every log line flows through one place that can be audited.
 */
object AppLogger {
    interface Sink {
        fun log(level: Level, tag: String, message: String, throwable: Throwable?)
    }

    enum class Level { DEBUG, INFO, WARN, ERROR }

    @Volatile
    private var sink: Sink? = null

    /** Allows the Application class to install a real backend (Crashlytics/Sentry) at startup. */
    fun install(sink: Sink) {
        this.sink = sink
    }

    fun d(tag: String, message: String) = emit(Level.DEBUG, tag, message, null)
    fun i(tag: String, message: String) = emit(Level.INFO, tag, message, null)
    fun w(tag: String, message: String, throwable: Throwable? = null) = emit(Level.WARN, tag, message, throwable)
    fun e(tag: String, message: String, throwable: Throwable? = null) = emit(Level.ERROR, tag, message, throwable)

    private fun emit(level: Level, tag: String, message: String, throwable: Throwable?) {
        val installed = sink
        if (installed != null) {
            installed.log(level, tag, message, throwable)
            return
        }
        if (BuildConfig.DEBUG) {
            when (level) {
                Level.DEBUG -> println("D/$tag: $message")
                Level.INFO -> println("I/$tag: $message")
                Level.WARN -> println("W/$tag: $message${throwable?.let { " ($it)" } ?: ""}")
                Level.ERROR -> println("E/$tag: $message${throwable?.let { " ($it)" } ?: ""}")
            }
        }
    }
}
