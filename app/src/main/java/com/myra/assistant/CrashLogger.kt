package com.myra.assistant

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

object CrashLogger {
    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        logFile = File(context.getExternalFilesDir(null), "myra_crash_log.txt")
    }

    fun log(tag: String, message: String, throwable: Throwable? = null) {
        try {
            Log.e(tag, message, throwable)
            val file = logFile ?: return
            val writer = FileWriter(file, true)
            val ts = dateFormat.format(Date())
            writer.append("[${ts}] $tag: $message\n")
            if (throwable != null) {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                writer.append(sw.toString())
                writer.append("\n")
            }
            writer.flush()
            writer.close()
        } catch (_: Exception) {}
    }

    fun getLogPath(): String? = logFile?.absolutePath
}
