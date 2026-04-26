package com.cleanerapp.cleaner

import android.content.Context
import android.os.Environment
import androidx.documentfile.provider.DocumentFile
import java.io.File

class AnalyseUseCase(private val context: Context) {

    fun execute(
        scanCache: Boolean = true,
        scanTemp: Boolean = true,
        scanBrowser: Boolean = true,
        scanApk: Boolean = true,
        scanThumbnails: Boolean = true,
        whatsAppDocFile: DocumentFile? = null,
        telegramDocFile: DocumentFile? = null
    ): AnalyseResult {
        val categories = mutableListOf<CategoryAnalysis>()

        if (scanCache) {
            categories.add(scanCache())
        }
        if (scanTemp) {
            categories.add(scanTemp())
        }
        if (scanBrowser) {
            categories.add(scanBrowser())
        }
        if (scanApk) {
            categories.add(scanApk())
        }
        if (scanThumbnails) {
            categories.add(scanThumbnails())
        }
        if (whatsAppDocFile != null) {
            categories.add(scanDocumentFile("WhatsApp", whatsAppDocFile))
        }
        if (telegramDocFile != null) {
            categories.add(scanDocumentFile("Telegram", telegramDocFile))
        }

        return AnalyseResult(categories = categories)
    }

    private fun scanCache(): CategoryAnalysis {
        var size = 0L
        var count = 0
        listOfNotNull(context.cacheDir, *context.externalCacheDirs)
            .forEach { dir ->
                dir.walkTopDown().filter { it.isFile }.forEach {
                    size += it.length()
                    count++
                }
            }
        return CategoryAnalysis("Cache des apps", size, count)
    }

    private fun scanTemp(): CategoryAnalysis {
        var size = 0L
        var count = 0
        val tempExtensions = listOf(".tmp", ".temp", ".cache", ".bak", ".old", ".log")
        val downloads = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        downloads.walkTopDown()
            .filter { it.isFile && tempExtensions.any { ext ->
                it.name.endsWith(ext, ignoreCase = true)
            }}
            .forEach { size += it.length(); count++ }
        return CategoryAnalysis("Fichiers temporaires", size, count)
    }

    private fun scanBrowser(): CategoryAnalysis {
        var size = 0L
        var count = 0
        listOf(
            File(context.cacheDir, "webviewchromium"),
            File(context.cacheDir, "org.chromium.android_webview"),
            File(context.dataDir, "app_webview")
        ).forEach { dir ->
            if (dir.exists()) {
                dir.walkTopDown().filter { it.isFile }.forEach {
                    size += it.length()
                    count++
                }
            }
        }
        return CategoryAnalysis("Données navigateur", size, count)
    }

    private fun scanApk(): CategoryAnalysis {
        var size = 0L
        var count = 0
        val downloads = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )
        downloads.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".apk", ignoreCase = true) }
            .forEach { size += it.length(); count++ }
        return CategoryAnalysis("APK téléchargés", size, count)
    }

    private fun scanThumbnails(): CategoryAnalysis {
        var size = 0L
        var count = 0
        listOf(
            File(Environment.getExternalStorageDirectory(), "DCIM/.thumbnails"),
            File(Environment.getExternalStorageDirectory(), "Pictures/.thumbnails"),
            File(Environment.getExternalStorageDirectory(), ".thumbnails"),
            File(context.cacheDir, "image_manager_disk_cache"),
            File(context.cacheDir, "glide_cache")
        ).forEach { dir ->
            if (dir.exists()) {
                dir.walkTopDown().filter { it.isFile }.forEach {
                    size += it.length()
                    count++
                }
            }
        }
        return CategoryAnalysis("Miniatures médias", size, count)
    }

    private fun scanDocumentFile(name: String, docFile: DocumentFile): CategoryAnalysis {
        var size = 0L
        var count = 0
        fun scan(file: DocumentFile) {
            if (file.isDirectory) {
                file.listFiles().forEach { scan(it) }
            } else {
                size += file.length()
                count++
            }
        }
        scan(docFile)
        return CategoryAnalysis(name, size, count)
    }
}