package com.mbakadmin.dagango.util

import android.content.Context
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.RandomAccessFile
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * CrashLogger & UncaughtExceptionHandler (hardened).
 * - One-attempt-per-process guard (no infinite crash-restart loop if crash happens in Application.onCreate)
 * - Size-capped rotating log file with tail reads (no unbounded growth, no main-thread full-file reads)
 * - PII redaction: digit runs >= 8 (phone/account numbers) masked; message + stacktrace capped;
 *   only a one-line summary goes to logcat (never the full report)
 */
object CrashLogger {
    private const val TAG = "MbakAdminCrash"
    private const val LOG_FILE_NAME = "crash_logs.txt"
    private const val LATEST_FILE_NAME = "latest_crash.txt"

    private const val MAX_LOG_BYTES = 512_000L      // rotate when exceeded
    private const val KEEP_TAIL_BYTES = 256_000L    // keep after rotation
    private const val READ_TAIL_BYTES = 64_000L     // max bytes shown in viewer
    private const val MAX_MESSAGE_CHARS = 300
    private const val MAX_STACK_LINES = 40

    private val crashHandlingInProgress = AtomicBoolean(false)
    private val fileLock = Any()

    private val phoneLikeRegex = Regex("""\d{8,}""")

    fun init(context: Context) {
        val appContext = context.applicationContext
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            // One attempt per process: if a second crash arrives while the first
            // is still being written (or a crash loops inside onCreate), hand straight
            // to the OS default handler instead of looping.
            if (crashHandlingInProgress.compareAndSet(false, true)) {
                try {
                    recordCrash(appContext, thread, throwable)
                } catch (_: Exception) {
                    // Never let logging itself crash the process
                }
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    @Synchronized
    fun logHandledException(context: Context, tag: String, message: String, throwable: Throwable? = null) {
        try {
            val entry = buildString {
                appendLine("==============================")
                appendLine("[HANDLED ERROR] ${timestamp()}")
                appendLine("TAG: ${redactText(tag)}")
                appendLine("Message: ${redactText(truncate(message, MAX_MESSAGE_CHARS))}")
                if (throwable != null) {
                    appendLine("Exception: ${throwable.javaClass.name}")
                    appendLine(capLines(stackOf(throwable), MAX_STACK_LINES))
                }
                appendLine("==============================")
            } + "\n"

            appendRotating(context, entry)
            // One-line summary only — full detail stays on disk, never logcat
            Log.e(tag, "Handled error recorded: ${redactText(truncate(message, 120))}")
        } catch (_: Exception) {
            // Swallow: diagnostics must not break the app
        }
    }

    private fun recordCrash(context: Context, thread: Thread, throwable: Throwable) {
        val entry = buildString {
            appendLine("==============================")
            appendLine("[FATAL CRASH] ${timestamp()}")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, API ${Build.VERSION.SDK_INT})")
            appendLine("Thread: ${thread.name} (ID: ${thread.id})")
            appendLine("Exception: ${throwable.javaClass.name}")
            appendLine("Message: ${redactText(truncate(throwable.message ?: "-", MAX_MESSAGE_CHARS))}")
            appendLine(capLines(stackOf(throwable), MAX_STACK_LINES))
            appendLine("==============================")
        } + "\n"

        // 1. Latest crash (overwrite)
        File(context.filesDir, LATEST_FILE_NAME).writeText(entry)
        // 2. Rotating history (append)
        appendRotating(context, entry)

        Log.e(TAG, "Fatal crash recorded to disk: ${redactText(truncate(throwable.toString(), 120))}")
    }

    private fun appendRotating(context: Context, entry: String) {
        synchronized(fileLock) {
            val file = File(context.filesDir, LOG_FILE_NAME)
            if (file.exists() && file.length() > MAX_LOG_BYTES) {
                val tail = readTail(file, KEEP_TAIL_BYTES)
                file.writeText(tail)
            }
            file.appendText(entry)
        }
    }

    /** Cheap existence check for UI composition (stat only, no bulk read). */
    fun hasLogs(context: Context): Boolean {
        val file = File(context.filesDir, LOG_FILE_NAME)
        return file.exists() && file.length() > 0
    }

    /** Returns at most the last READ_TAIL_BYTES of the crash log. */
    fun getAllCrashLogs(context: Context): String? {
        val file = File(context.filesDir, LOG_FILE_NAME)
        if (!file.exists() || file.length() == 0L) return null
        return readTail(file, READ_TAIL_BYTES)
    }

    @Synchronized
    fun clearLogs(context: Context) {
        synchronized(fileLock) {
            File(context.filesDir, LATEST_FILE_NAME).delete()
            File(context.filesDir, LOG_FILE_NAME).delete()
        }
    }

    // === PII & SIZE HYGIENE ===

    private fun redactText(input: String): String =
        phoneLikeRegex.replace(input) { match ->
            val v = match.value
            v.take(2) + "*".repeat(v.length - 3) + v.last()
        }

    private fun truncate(input: String, max: Int): String =
        if (input.length <= max) input else input.take(max) + "…"

    private fun capLines(input: String, maxLines: Int): String {
        val lines = input.lineSequence().toList()
        return if (lines.size <= maxLines) input
        else lines.take(maxLines).joinToString("\n") +
            "\n\t... (${lines.size - maxLines} more frames capped)"
    }

    private fun stackOf(t: Throwable): String {
        val sw = StringWriter()
        t.printStackTrace(PrintWriter(sw))
        return redactText(sw.toString())
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

    private fun readTail(file: File, maxBytes: Long): String =
        try {
            RandomAccessFile(file, "r").use { raf ->
                val fileLen = raf.length()
                val start = if (fileLen > maxBytes) fileLen - maxBytes else 0L
                raf.seek(start)
                val bytes = ByteArray((fileLen - start).toInt())
                raf.readFully(bytes)
                String(bytes, Charsets.UTF_8)
            }
        } catch (_: Exception) {
            ""
        }
}
