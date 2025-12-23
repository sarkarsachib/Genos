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
     * Initialize file logging
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
     * Log info level message
     */
    fun logInfo(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = TAG_PREFIX + tag
        Log.i(fullTag, message, throwable)
        
        if (isFileLoggingEnabled) {
            writeToFile("INFO", tag, message, throwable)
        }
    }
    
    /**
     * Log debug level message
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
     * Log warning level message
     */
    fun logWarning(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = TAG_PREFIX + tag
        Log.w(fullTag, message, throwable)
        
        if (isFileLoggingEnabled) {
            writeToFile("WARN", tag, message, throwable)
        }
    }
    
    /**
     * Log error level message
     */
    fun logError(tag: String, message: String, throwable: Throwable? = null) {
        val fullTag = TAG_PREFIX + tag
        Log.e(fullTag, message, throwable)
        
        if (isFileLoggingEnabled) {
            writeToFile("ERROR", tag, message, throwable)
        }
    }
    
    /**
     * Log verbose level message
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
     * Write log message to file
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
     * Rotate log files to maintain history
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
     * Get log file contents
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
     * Clear log files
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