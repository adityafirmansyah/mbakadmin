package com.mbakadmin.dagango.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * CrashLogger & UncaughtExceptionHandler
 * Catat setiap fatal exception / uncaught crash ke file lokal internal app:
 * `crash_logs.txt` dan `latest_crash.txt`.
 */
object CrashLogger {
    private const val TAG = "MbakAdminCrashLogger"
    private const val LOG_FILE_NAME = "crash_logs.txt"
    private const val LATEST_FILE_NAME = "latest_crash.txt"

    fun init(context: Context) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                recordCrash(context, thread, throwable)
            } catch (e: Exception) {
                Log.e(TAG, "Gagal menulis log crash: ${e.message}", e)
            } finally {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
    }

    @Synchronized
    fun logHandledException(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            throwable?.printStackTrace(pw)
            val stackTrace = sw.toString()

            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val entry = """
                ==============================
                [HANDLED ERROR] $timestamp
                TAG: $tag
                Message: $message
                ${if (stackTrace.isNotBlank()) "Stacktrace:\n$stackTrace" else ""}
                ==============================
            """.trimIndent() + "\n\n"

            val file = File(context.filesDir, LOG_FILE_NAME)
            file.appendText(entry)
            Log.e(tag, "$message\n$stackTrace")
        } catch (e: Exception) {
            Log.e(TAG, "Gagal log handled exception: ${e.message}")
        }
    }

    private fun recordCrash(context: Context, thread: Thread, throwable: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        throwable.printStackTrace(pw)
        val stackTrace = sw.toString()

        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val crashReport = """
            ==============================
            [FATAL CRASH] $timestamp
            Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})
            Thread: ${thread.name} (ID: ${thread.id})
            Exception: ${throwable.javaClass.name}
            Message: ${throwable.message}
            Stacktrace:
            $stackTrace
            ==============================
        """.trimIndent() + "\n\n"

        // 1. Simpan crash report terbaru
        val latestFile = File(context.filesDir, LATEST_FILE_NAME)
        latestFile.writeText(crashReport)

        // 2. Append ke riwayat crash logs
        val allLogsFile = File(context.filesDir, LOG_FILE_NAME)
        allLogsFile.appendText(crashReport)

        Log.e(TAG, "FATAL CRASH TERCATAT KE FILE:\n$crashReport")
    }

    fun getLatestCrashLog(context: Context): String? {
        val file = File(context.filesDir, LATEST_FILE_NAME)
        return if (file.exists() && file.length() > 0) file.readText() else null
    }

    fun getAllCrashLogs(context: Context): String? {
        val file = File(context.filesDir, LOG_FILE_NAME)
        return if (file.exists() && file.length() > 0) file.readText() else null
    }

    fun clearLogs(context: Context) {
        File(context.filesDir, LATEST_FILE_NAME).delete()
        File(context.filesDir, LOG_FILE_NAME).delete()
    }
}
