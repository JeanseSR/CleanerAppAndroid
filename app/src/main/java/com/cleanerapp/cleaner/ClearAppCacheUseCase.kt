package com.votreapp.cleaner

import android.content.Context
import android.os.Build
import java.io.File

class ClearAppCacheUseCase(private val context: Context) {

    data class CacheResult(
        val clearedBytes: Long,
        val errors: List<String>
    )

    fun execute(): CacheResult {
        var totalCleared = 0L
        val errors = mutableListOf<String>()

        // Cache interne de l'app
        totalCleared += clearDir(context.cacheDir, errors)

        // Cache externe si disponible
        context.externalCacheDirs.forEach { dir ->
            if (dir != null) totalCleared += clearDir(dir, errors)
        }

        // Fichiers temporaires internes
        totalCleared += clearDir(context.filesDir, errors)

        return CacheResult(
            clearedBytes = totalCleared,
            errors = errors
        )
    }

    private fun clearDir(dir: File?, errors: MutableList<String>): Long {
        if (dir == null || !dir.exists()) return 0L
        var freed = 0L
        dir.walkTopDown()
            .filter { it.isFile }
            .forEach { file ->
                try {
                    freed += file.length()
                    file.delete()
                } catch (e: Exception) {
                    errors.add("Impossible de supprimer : ${file.name}")
                }
            }
        return freed
    }

    fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1_048_576 -> "%.1f Mo".format(bytes / 1_048_576.0)
            bytes >= 1_024    -> "%.1f Ko".format(bytes / 1_024.0)
            else              -> "$bytes octets"
        }
    }
}