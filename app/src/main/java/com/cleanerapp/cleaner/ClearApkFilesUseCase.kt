package com.cleanerapp.cleaner

import android.content.Context
import android.os.Environment
import java.io.File

class ClearApkFilesUseCase(private val context: Context) {

    data class ApkResult(
        val clearedBytes: Long,
        val filesCount: Int,
        val errors: List<String>
    )

    fun execute(): ApkResult {
        var totalCleared = 0L
        var filesCount = 0
        val errors = mutableListOf<String>()

        // Dossier Downloads principal
        val downloads = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )

        scanForApks(downloads, errors) { bytes ->
            totalCleared += bytes
            filesCount++
        }

        return ApkResult(
            clearedBytes = totalCleared,
            filesCount = filesCount,
            errors = errors
        )
    }

    private fun scanForApks(
        dir: File?,
        errors: MutableList<String>,
        onDeleted: (Long) -> Unit
    ) {
        if (dir == null || !dir.exists()) return
        dir.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".apk", ignoreCase = true) }
            .forEach { file ->
                try {
                    val size = file.length()
                    if (file.delete()) onDeleted(size)
                } catch (e: Exception) {
                    errors.add("Erreur APK : ${file.name}")
                }
            }
    }
}