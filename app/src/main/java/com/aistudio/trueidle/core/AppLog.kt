package com.aistudio.trueidle.core

/**
 * Minimal logging seam so the pure `core` / `engine` / `data` modules can report failures without
 * importing `android.util.Log` (see ArchitectureTest). The app wires the real logger once.
 */
object AppLog {
    var logger: ((tag: String, message: String, throwable: Throwable?) -> Unit)? = null

    fun warn(tag: String, message: String, throwable: Throwable? = null) {
        logger?.invoke(tag, message, throwable)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        logger?.invoke(tag, message, throwable)
    }
}
