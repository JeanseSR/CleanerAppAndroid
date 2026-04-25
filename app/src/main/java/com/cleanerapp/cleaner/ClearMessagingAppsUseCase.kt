package com.cleanerapp.cleaner

import android.content.Context
import android.os.Environment
import java.io.File

class ClearMessagingAppsUseCase(private val context: Context) {

    data class MessagingResult(
        val clearedBytes: Long,
        val filesCount: Int,
        val errors: List<String>
    )

    fun execute(
        clearWhatsApp: Boolean = true,
        clearTelegram: Boolean = true
    ): MessagingResult {
        var totalCleared = 0L
        var filesCount = 0
        val errors = mutableListOf<String>()

        val root = Environment.getExternalStorageDirectory()

        if (clearWhatsApp) {
            val whatsappDirs = listOf(
                File(root, "WhatsApp/Media/WhatsApp Images/Sent"),
                File(root, "WhatsApp/Media/WhatsApp Video/Sent"),
                File(root, "WhatsApp/Media/WhatsApp Audio/Sent"),
                File(root, "WhatsApp/Media/WhatsApp Documents/Sent"),
                File(root, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/Sent"),
                File(root, "Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Video/Sent")
            )
            whatsappDirs.forEach { dir ->
                scanAndDelete(dir, errors) { bytes ->
                    totalCleared += bytes
                    filesCount++
                }
            }
        }

        if (clearTelegram) {
            val telegramDirs = listOf(
                File(root, "Telegram/Telegram Images"),
                File(root, "Telegram/Telegram Video"),
                File(root, "Telegram/Telegram Audio"),
                File(root, "Telegram/Telegram Documents"),
                File(root, "Android/data/org.telegram.messenger/cache")
            )
            telegramDirs.forEach { dir ->
                scanAndDelete(dir, errors) { bytes ->
                    totalCleared += bytes
                    filesCount++
                }
            }
        }

        return MessagingResult(
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
            .filter { it.isFile }
            .forEach { file ->
                try {
                    val size = file.length()
                    if (file.delete()) onDeleted(size)
                } catch (e: Exception) {
                    errors.add("Erreur : ${file.name}")
                }
            }
    }
}