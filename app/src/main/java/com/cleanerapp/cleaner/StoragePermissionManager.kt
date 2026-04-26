package com.cleanerapp.cleaner

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.UriPermission
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile

class StoragePermissionManager(private val context: Context) {

    companion object {
        const val WHATSAPP_PREF_KEY = "whatsapp_uri"
        const val TELEGRAM_PREF_KEY = "telegram_uri"
        const val PREFS_NAME = "cleaner_prefs"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasWhatsAppAccess(): Boolean {
        val uriStr = prefs.getString(WHATSAPP_PREF_KEY, null) ?: return false
        val uri = Uri.parse(uriStr)
        return context.contentResolver.persistedUriPermissions
            .any { it.uri == uri && it.isReadPermission && it.isWritePermission }
    }

    fun hasTelegramAccess(): Boolean {
        val uriStr = prefs.getString(TELEGRAM_PREF_KEY, null) ?: return false
        val uri = Uri.parse(uriStr)
        return context.contentResolver.persistedUriPermissions
            .any { it.uri == uri && it.isReadPermission && it.isWritePermission }
    }

    fun saveWhatsAppUri(uri: Uri) {
        prefs.edit().putString(WHATSAPP_PREF_KEY, uri.toString()).apply()
    }

    fun saveTelegramUri(uri: Uri) {
        prefs.edit().putString(TELEGRAM_PREF_KEY, uri.toString()).apply()
    }

    fun getWhatsAppDocumentFile(): DocumentFile? {
        val uriStr = prefs.getString(WHATSAPP_PREF_KEY, null) ?: return null
        return DocumentFile.fromTreeUri(context, Uri.parse(uriStr))
    }

    fun getTelegramDocumentFile(): DocumentFile? {
        val uriStr = prefs.getString(TELEGRAM_PREF_KEY, null) ?: return null
        return DocumentFile.fromTreeUri(context, Uri.parse(uriStr))
    }

    fun deleteRecursively(documentFile: DocumentFile?): Pair<Long, Int> {
        if (documentFile == null || !documentFile.exists()) return Pair(0L, 0)
        var totalBytes = 0L
        var count = 0
        documentFile.listFiles().forEach { file ->
            if (file.isDirectory) {
                val (bytes, cnt) = deleteRecursively(file)
                totalBytes += bytes
                count += cnt
            } else {
                val size = file.length()
                if (file.delete()) {
                    totalBytes += size
                    count++
                }
            }
        }
        return Pair(totalBytes, count)
    }
}