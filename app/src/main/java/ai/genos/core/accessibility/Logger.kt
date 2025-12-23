package ai.genos.core.accessibility

import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


/**
 * Logger utility for the accessibility service
 * Provides both Android Log integration and file logging capabilities
 */
object Logger {
    
    private const val TAG_PREFIX = "GENOS_"
    private const val FILE_LOG_PREFIX = "[GENOS]"
    private const val MAX_LOG_FILES = 5
    private const val MAX_LOG_FILE_SIZE = 1024 * 1024 // 1MB
    private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS"
    
    private var logFile: File? = null
    private var isFileLoggingEnabled = false
    
    /**
     * Initializes on-device file logging for the accessibility service.
     *
     * Creates a "logs" directory under the application's files directory (if missing), prepares the current
     * log file, rotates existing log files to maintain history, enables file logging, and records an info
     * entry that file logging was initialized.
     *
     * If initialization fails, file logging is disabled and a warning is logged to Android Logcat.
     *
     * @param context Application context used to locate the files directory.
     */
    fun initializeFileLogging(context: android.content.Context) {
        try {
            val logDir = File(context.filesDir, "logs")
            if (!logDir.exists()) {
                logDir.mkdirs()
            }
            
            val currentLogFile = File(logDir, "accessibility_service.log")
            
            // Rotate log files if needed
            rotateLogFiles(logDir)
            
            logFile = currentLogFile
            isFileLoggingEnabled = true
            
            logInfo("Logger", "File logging initialized: ${currentLogFile.absolutePath}")
            
        } catch (e: Exception) {
            Log.w("GENOS_Logger", "Failed to initialize file logging", e)
            isFileLoggingEnabled = false
        }
    }
    
    /**
     * Logs an informational message to Android logcat and, when file logging is enabled, to the rotated log file.
     *
     * @param tag Log tag; `GENOS_` is prefixed to this value for the Android log entry.
     * @param message The message to log.
     * @param throwable Optional throwable whose stack trace will be included in both logcat and file logs when provided.
     */
    fun logInfo(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = TAG_PREFIX + tag
        Log.i(fullTag, message, throwable)
        
        if (isFileLoggingEnabled) {
            writeToFile("INFO", tag, message, throwable)
        }
    }
    
    /**
     * Logs a debug-level message to Android logcat and, when file logging is enabled, to the rotated log file.
     *
     * @param tag The log tag (the `GENOS_` prefix is added automatically).
     * @param message The message to log.
     * @param throwable Optional throwable whose stack trace will be included with the entry.
     */
    fun logDebug(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            val fullTag = TAG_PREFIX + tag
            Log.d(fullTag, message, throwable)
        }
        
        if (isFileLoggingEnabled) {
            writeToFile("DEBUG", tag, message, throwable)
        }
    }
    
    /**
     * Logs a warning message to Android logcat and, if file logging is enabled, to the rotated log file.
     *
     * @param tag The log tag; a fixed prefix is added before this value for Android log entries.
     * @param message The text message to log.
     * @param throwable Optional throwable whose stack trace will be appended to both logcat and the file entry when present.
     */
    fun logWarning(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = TAG_PREFIX + tag
        Log.w(fullTag, message, throwable)
        
        if (isFileLoggingEnabled) {
            writeToFile("WARN", tag, message, throwable)
        }
    }
    
    /**
     * Logs an error message to Android Logcat and, if file logging is enabled, to the rotated log file.
     *
     * If a `throwable` is provided, its stack trace is included in both outputs.
     *
     * @param tag Short identifier for the log source (will be prefixed internally).
     * @param message The error message to record.
     * @param throwable Optional throwable whose stack trace will be logged. 
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = TAG_PREFIX + tag
        Log.e(fullTag, message, throwable)
        
        if (isFileLoggingEnabled) {
            writeToFile("ERROR", tag, message, throwable)
        }
    }
    
    /**
     * Logs a verbose-level entry to Logcat when running a debug build and, if file logging is enabled, appends a VERBOSE entry to the current log file.
     *
     * @param throwable Optional throwable whose stack trace will be included with the log entry.
     */
    fun logVerbose(tag: String, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            val fullTag = TAG_PREFIX + tag
            Log.v(fullTag, message, throwable)
            
            if (isFileLoggingEnabled) {
                writeToFile("VERBOSE", tag, message, throwable)
            }
        }
    }
    
    /**
     * Appends a timestamped, prefixed log entry to the configured log file and rotates files if the current
     * file exceeds the maximum size.
     *
     * The written line uses the file log prefix and includes the provided level, tag, and message.
     * If `throwable` is provided, its stack trace is appended on the following line.
     *
     * IO errors are suppressed to avoid cascading failures; a warning is emitted via Android Log when writing fails.
     *
     * @param level The log level label (e.g., "INFO", "DEBUG", "ERROR").
     * @param tag Short source identifier to include in the entry.
     * @param message The log message to record.
     * @param throwable Optional throwable whose stack trace will be appended to the entry when present.
     */
    private fun writeToFile(level: String, tag: String, message: String, throwable: Throwable?) {
        try {
            val logFile = this.logFile ?: return
            
            // Check file size and rotate if needed
            if (logFile.exists() && logFile.length() > MAX_LOG_FILE_SIZE) {
                rotateLogFiles(File(logFile.parent))
            }
            
            FileWriter(logFile, true).use { writer ->
                val timestamp = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date())
                var logMessage = "$FILE_LOG_PREFIX $timestamp [$level] $tag: $message"
                
                if (throwable != null) {
                    logMessage += "\n${Log.getStackTraceString(throwable)}"
                }
                
                writer.appendln(logMessage)
                writer.flush()
            }
            
        } catch (e: IOException) {
            // Silently fail to avoid infinite loops
            Log.w("GENOS_Logger", "Failed to write to log file", e)
        }
    }
    
    /**
     * Rotate on-disk accessibility service log files to maintain a capped history.
     *
     * Renames files named accessibility_service.log, accessibility_service.1.log, ..., shifting each
     * file up by one index and deleting the oldest file when the count exceeds MAX_LOG_FILES.
     *
     * @param logDir Directory containing the accessibility service log files.
     */
    private fun rotateLogFiles(logDir: File) {
        try {
            for (i in MAX_LOG_FILES downTo 1) {
                val currentFile = File(logDir, "accessibility_service${if (i == 1) "" else ".$i"}.log")
                val nextFile = File(logDir, "accessibility_service.${i + 1}.log")
                
                if (currentFile.exists()) {
                    if (i == MAX_LOG_FILES) {
                        currentFile.delete() // Delete oldest
                    } else {
                        currentFile.renameTo(nextFile)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("GENOS_Logger", "Failed to rotate log files", e)
        }
    }
    
    /**
     * Reads and returns the contents of the current log file.
     *
     * @return The file contents as a `String`, or `null` if no log file is available or it cannot be read.
     */
    fun getLogContents(): String? {
        return try {
            val logFile = this.logFile ?: return null
            if (logFile.exists()) {
                logFile.readText()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("GENOS_Logger", "Failed to read log file", e)
            null
        }
    }
    
    /**
     * Deletes the current log file used by the Logger, if present.
     *
     * Any exception thrown while attempting to delete the file is caught and suppressed.
     */
    fun clearLogs() {
        try {
            val logFile = this.logFile ?: return
            if (logFile.exists()) {
                logFile.delete()
            }
        } catch (e: Exception) {
            Log.w("GENOS_Logger", "Failed to clear logs", e)
        }
    }
}