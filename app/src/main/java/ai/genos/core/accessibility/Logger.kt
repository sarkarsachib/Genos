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
     * Initializes file-based logging for the accessibility service.
     *
     * Creates a "logs" directory under the application's files directory, rotates existing log files
     * to maintain history, sets the active log file, and enables file logging. On successful
     * initialization an informational entry is written; on failure file logging is disabled and a
     * warning is emitted to the Android log.
     *
     * @param context The Android context used to resolve the app's files directory for storing logs.
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
     * Log an informational message to the Android log and, when enabled, to the rotated file log.
     *
     * The Android log tag is prefixed with the Logger tag prefix. When file logging is enabled an
     * INFO entry is appended to the active log file and the throwable's stack trace is included if provided.
     *
     * @param tag Short identifier for the log entry; the Logger's tag prefix is prepended before sending to Android Log.
     * @param message The message text to log.
     * @param throwable Optional throwable whose stack trace will be logged alongside the message.
     */
    fun logInfo(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = TAG_PREFIX + tag
        Log.i(fullTag, message, throwable)
        
        if (isFileLoggingEnabled) {
            writeToFile("INFO", tag, message, throwable)
        }
    }
    
    /**
     * Logs a debug-level message using the GENOS-prefixed tag and optionally records it to the active log file.
     *
     * @param tag The log tag (the function will prepend the internal "GENOS_" prefix).
     * @param message The message to log.
     * @param throwable Optional exception whose stack trace will be included with the log entry.
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
     * Log a warning message to the Android log and, if enabled, append it to the rotated file log.
     *
     * @param tag Log tag (will be prefixed with TAG_PREFIX).
     * @param message The message to record.
     * @param throwable Optional throwable whose stack trace will be included. 
     */
    fun logWarning(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = TAG_PREFIX + tag
        Log.w(fullTag, message, throwable)
        
        if (isFileLoggingEnabled) {
            writeToFile("WARN", tag, message, throwable)
        }
    }
    
    /**
     * Logs an error-level message to Android Log and, if file logging is enabled, appends an ERROR entry to the active log file.
     *
     * @param tag The log tag (the implementation will prefix this tag with the package-specific tag prefix).
     * @param message The log message to record.
     * @param throwable Optional throwable whose stack trace will be included with the log entry.
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = TAG_PREFIX + tag
        Log.e(fullTag, message, throwable)
        
        if (isFileLoggingEnabled) {
            writeToFile("ERROR", tag, message, throwable)
        }
    }
    
    /**
     * Logs a verbose-level message to Android's Log and, when file logging is enabled, appends a timestamped verbose entry to the active log file; only emits output in debug builds.
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
     * Appends a timestamped log entry to the active file log, rotating files if the current file exceeds the size threshold.
     *
     * The entry is written with the configured file prefix and includes the provided level, tag, and message.
     * If `throwable` is provided, its stack trace is appended on the next line.
     * If file I/O fails, a warning is logged and the method returns without throwing.
     *
     * @param level Log level label to include in the entry (e.g., "INFO", "ERROR").
     * @param tag Tag to include with the entry.
     * @param message The log message body.
     * @param throwable Optional throwable whose stack trace will be appended to the entry.
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
     * Rotate existing accessibility_service log files in the given directory to maintain up to MAX_LOG_FILES history.
     *
     * Moves or renames files so that the most recent log stays at `accessibility_service.log` and older files are shifted
     * to `accessibility_service.<n>.log`, deleting the oldest file when the maximum count is exceeded.
     *
     * @param logDir Directory containing the accessibility_service log files to rotate.
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
     * Retrieves the current active log file's contents as a single string.
     *
     * Returns the file contents if an active log file exists and can be read; returns `null` if no log file is configured, the file does not exist, or an error occurs while reading.
     *
     * @return The log file contents, or `null` when unavailable. */
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
     * Deletes the current active log file used for file-based logging, if present.
     *
     * If an error occurs while deleting the file, the exception is caught and not propagated.
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