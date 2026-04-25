package com.cleanerapp.cleaner

import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import java.io.File

class ClearBrowserDataUseCase(private val context: Context) {

    data class BrowserResult(
        val clearedBytes: Long,
        val errors: List<String>
    )

    fun execute(): BrowserResult {
        var totalCleared = 0L
        val errors = mutableListOf<String>()

        // Cookies WebView
        try {
            CookieManager.getInstance().apply {
                removeAllCookies(null)
                flush()
            }
        } catch (e: Exception) {
            errors.add("Cookies : ${e.message}")
        }

        // Cache WebView
        try {
            val webView = WebView(context)
            webView.clearCache(true)
            webView.clearHistory()
            webView.clearFormData()
        } catch (e: Exception) {
            errors.add("Cache WebView : ${e.message}")
        }

        // WebStorage (bases de données locales, localStorage)
        try {
            WebStorage.getInstance().deleteAllData()
        } catch (e: Exception) {
            errors.add("WebStorage : ${e.message}")
        }

        // Fichiers cache navigateur sur le stockage interne
        val browserCacheDirs = listOf(
            File(context.cacheDir, "webviewchromium"),
            File(context.cacheDir, "org.chromium.android_webview"),
            File(context.dataDir, "app_webview")
        )

        browserCacheDirs.forEach { dir ->
            if (dir.exists()) {
                dir.walkTopDown()
                    .filter { it.isFile }
                    .forEach { file ->
                        try {
                            totalCleared += file.length()
                            file.delete()
                        } catch (e: Exception) {
                            errors.add("Fichier : ${file.name}")
                        }
                    }
            }
        }

        return BrowserResult(
            clearedBytes = totalCleared,
            errors = errors
        )
    }
}