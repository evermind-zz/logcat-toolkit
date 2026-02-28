package com.github.logviewer

import android.content.Context
import com.github.logviewer.ExportLogFileUtils.StorageLocation
import com.github.logviewer.settings.CleanupConfig
import com.github.logviewer.settings.KeepLastNFilesStrategy
import com.github.logviewer.settings.LogFileShare
import com.github.logviewer.settings.LogFileShareDefault
import java.io.BufferedWriter
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

/**
 * Home of the settings that are used within the library.
 *
 * There are two use cases in this library:
 *  - 1) As global [Settings] Singleton for the standard usage (use as fragment or floating window)
 *  - 2) And using inside the [LogcatDumper]
 */
class Settings {
    private val configRef = AtomicReference(LogConfig())

    val config: LogConfig get() = configRef.get()

    /**
     * This method takes the current state and allows to change only fields you want to change.
     */
    fun update(transform: (LogConfig) -> LogConfig) {
        var success = false
        while (!success) {
            val current = configRef.get()
            val next = transform(current)
            success = configRef.compareAndSet(current, next)
        }
    }

    companion object {
        @JvmField
        val Default = Settings()
    }
}

data class LogConfig(
    var logfileFormat: LogFileFormat = object : LogFileFormat {
        override fun writeLogs(
            logFileName: String,
            logs: Array<LogItem>,
            writer: BufferedWriter
        ) {
            for (log in logs) {
                writer.write(log.origin + "\n")
            }
        }
    },

    val logFileName: LogFileName = object : LogFileName {
        override fun getLogFileName(): String {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS'.log'", Locale.getDefault())
            return dateFormat.format(Date())
        }
    },

    val logFileShare: LogFileShare = LogFileShareDefault(),
    /**
     *  chosen default strategy delete all except last 2 logs
     */
    var logCleanupStrategy: CleanupConfig = CleanupConfig(KeepLastNFilesStrategy(), 2),

    val logOpMode: LogcatReader.OperationMode = LogcatReader.OperationMode.CONTINUE,

    val logStorageLocation: StorageLocation = StorageLocation.CACHE_INTERNAL
) {
    companion object {
        /**
         * The folder containing the logs relative to [StorageLocation].
         *
         */
        // it has to be the same folder name as it is defined in 'main/res/xml/logcat_filepaths.xml'
        const val RELATIVE_LOG_DIR = "logcat_toolkit_logs"

        /**
         * create this empty file to ensure that we created that directory
         * and per our own convention are entitled to delete stuff there.
         */
        const val LOGCAT_TOOLKIT_MARKER_FILE = ".logcat_toolkit_root"
    }

    fun getLogFolder(context: Context): File {
        val parentDir = if (logStorageLocation == StorageLocation.CACHE_INTERNAL) {
            context.cacheDir
        } else {
            context.externalCacheDir
        }

        val logDir = File(parentDir, RELATIVE_LOG_DIR)
        ensureSafeLogDirectory(logDir)
        return logDir
    }

    private fun ensureSafeLogDirectory(logDir: File) {
        synchronized(logDir) {
            if (!logDir.exists()) {
                require(logDir.mkdirs()) {
                    "Failed to create directory: ${logDir.path}"
                }
                createMarkerFile(logDir)
            } else { // marker file should exist
                require(logDir.isDirectory) {
                    "${logDir.path} is not a directory"
                }
                checkForMarkerFile(logDir)
            }
        }
    }

    private fun checkForMarkerFile(logDir: File) {
        val marker = File(logDir, LOGCAT_TOOLKIT_MARKER_FILE)
        check(marker.exists()) {
            val errMsg = "unable to find marker file ${LOGCAT_TOOLKIT_MARKER_FILE}"
            Log.e(javaClass.simpleName, errMsg)
            errMsg
        }
    }

    private fun createMarkerFile(logDir: File) {
        val marker = File(logDir, LOGCAT_TOOLKIT_MARKER_FILE)
        require(marker.createNewFile()) { // only true is the right path here as we come from the mkdirs branch
            val errMsg = "unable to create marker file ${LOGCAT_TOOLKIT_MARKER_FILE}"
            Log.e(javaClass.simpleName, errMsg)
            errMsg
        }
    }
}

/**
 * set your implementation if you want to change the output format of the logfile.
 */
interface LogFileFormat {
    /**
     * write logs to output.
     */
    fun writeLogs(logFileName: String, logs: Array<LogItem>, writer: BufferedWriter)
}

/**
 * Set your implementation if you want to override the filename of the logfile.
 *
 * e.g. my_logfile.log
 */
interface LogFileName {
    /**
     * get the name of the to be generated logfile.
     */
    fun getLogFileName(): String
}
