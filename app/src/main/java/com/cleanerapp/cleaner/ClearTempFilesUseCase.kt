package com.cleanerapp.cleaner

import android.content.Context
import android.os.Environment
import java.io.File

class ClearTempFilesUseCase(private val context: Context) {

    data class TempResult(
        val clearedBytes: Long,
        val filesCount: Int,
        val errors: List<String>
    )

    fun execute(): TempResult {
        var totalCleared = 0L
        var filesCount = 0
        val errors = mutableListOf<String>()

        // Dossier Downloads
        val downloads = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        scanAndDelete(downloads, errors) { bytes ->
            totalCleared += bytes
            filesCount++
        }

        // Dossier temporaire externe de l'app
        context.externalCacheDirs.filterNotNull().forEach { dir ->
            scanAndDelete(dir, errors) { bytes ->
                totalCleared += bytes
                filesCount++
            }
        }

        return TempResult(
            clearedBytes = totalCleared,
            filesCount = filesCount,
            errors = errors
        )
    }

    private fun scanAndDelete(
        dir: File?,
        errors: MutableList<String>,
        onDeleted: (Long) -> Unit
    ) {
        if (dir == null || !dir.exists()) return
        dir.walkTopDown()
            .filter { it.isFile && isTempFile(it) }
            .forEach { file ->
                try {
                    val size = file.length()
                    if (file.delete()) onDeleted(size)
                } catch (e: Exception) {
                    errors.add("Erreur : ${file.name}")
                }
            }
    }

    private fun isTempFile(file: File): Boolean {
        val tempExtensions = listOf(
            ".tmp", ".temp", ".cache",
            ".bak", ".old", ".log"
        )
        return tempExtensions.any {
            file.name.endsWith(it, ignoreCase = true)
        }
    }
}