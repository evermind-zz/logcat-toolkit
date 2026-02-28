package com.github.logviewer.settings

import android.util.Log
import com.github.logviewer.LogConfig.Companion.LOGCAT_TOOLKIT_MARKER_FILE
import com.github.logviewer.settings.LogFileDeleteStrategy.Companion.USE_DEFAULT_THRESHOLD
import java.io.File

/**
 * Strategy interface for managing the cleanup of generated log files.
 *
 * This interface allows library consumers to define custom logic for deleting
 * old log exports to prevent storage bloat, which is especially critical on
 * legacy devices with limited disk space.
 */
interface LogFileDeleteStrategy {
    companion object {
        const val USE_DEFAULT_THRESHOLD = 0

    }

    /**
     * Applies the deletion logic to the specified directory.
     *
     * @param logFiles  The log files
     * @param threshold A versatile integer parameter used to define the deletion limit.
     *                  Depending on the implementation, this value can represent:
     *                  - A maximum number of files to keep (e.g., N newest files).
     *                  - A time-based limit in minutes (e.g., older than N minutes).
     *                  - A default fallback value of [USE_DEFAULT_THRESHOLD] if no specific threshold is provided.
     */
    fun apply(logFiles: Array<File>, threshold: Int = USE_DEFAULT_THRESHOLD)
}

class CleanupConfig(
    val strategy: LogFileDeleteStrategy,
    val threshold: Int = USE_DEFAULT_THRESHOLD
) {
    fun applyStrategy(logDir: File) {
        val logFiles = if (File(logDir, LOGCAT_TOOLKIT_MARKER_FILE).exists()) {
            logDir.listFiles { file ->
                file.isFile && file.name != LOGCAT_TOOLKIT_MARKER_FILE
            } ?: emptyArray()
        } else {
            Log.e(
                javaClass.simpleName,
                "Safety abort: marker $LOGCAT_TOOLKIT_MARKER_FILE is missing in ${logDir.path}!"
            )
            emptyArray()
        }

        strategy.apply(logFiles, threshold)
    }
}

open class KeepLastNFilesStrategy : LogFileDeleteStrategy {
    override fun apply(logFiles: Array<File>, threshold: Int) {
        val defaultKeepNoOfFilesThreshold = 5
        val limit =
            if (threshold <= USE_DEFAULT_THRESHOLD) defaultKeepNoOfFilesThreshold else threshold

        logFiles.sortedByDescending { it.lastModified() }
            .drop(limit)
            .forEach { it.delete() }
    }
}

open class DeleteOlderThanNMinutesStrategy : LogFileDeleteStrategy {
    override fun apply(logFiles: Array<File>, threshold: Int) {
        val defaultKeepOlderThanMinutes = 60
        val minutes = if (threshold <= 0) defaultKeepOlderThanMinutes else threshold
        val cutoff = System.currentTimeMillis() - (minutes * 60 * 1000L)

        logFiles.forEach { file ->
            if (file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }
}
