package com.cleanerapp.cleaner

import android.content.Context
import android.os.Environment
import java.io.File

class ClearThumbnailsUseCase(private val context: Context) {

    data class ThumbnailResult(
        val clearedBytes: Long,
        val filesCount: Int,
        val errors: List<String>
    )

    fun execute(): ThumbnailResult {
        var totalCleared = 0L
        var filesCount = 0
        val errors = mutableListOf<String>()

        val thumbnailDirs = listOf(
            // Thumbnails classiques
            File(Environment.getExternalStorageDirectory(), "DCIM/.thumbnails"),
            File(Environment.getExternalStorageDirectory(), "Pictures/.thumbnails"),
            File(Environment.getExternalStorageDirectory(), ".thumbnails"),
            // Cache médias Android
            File(context.cacheDir, "image_manager_disk_cache"),
            File(context.cacheDir, "glide_cache")
        )

        thumbnailDirs.forEach { dir ->
            if (dir.exists()) {
                dir.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        try {
                            val size = file.length()
                            if (file.delete()) {
                                totalCleared += size
                                filesCount++
                            }
                        } catch (e: Exception) {
                            errors.add("Erreur thumbnail : ${file.name}")
                        }
                    }
            }
        }

        return ThumbnailResult(
            clearedBytes = totalCleared,
            filesCount = filesCount,
            errors = errors
        )
    }
}