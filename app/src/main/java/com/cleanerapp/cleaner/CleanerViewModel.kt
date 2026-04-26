package com.cleanerapp.cleaner

import android.app.Application
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.votreapp.cleaner.ClearAppCacheUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class CleanerViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext

    private val clearCacheUseCase = ClearAppCacheUseCase(context)
    private val clearTempUseCase = ClearTempFilesUseCase(context)
    private val clearBrowserUseCase = ClearBrowserDataUseCase(context)
    private val clearApkUseCase = ClearApkFilesUseCase(context)
    private val clearThumbnailsUseCase = ClearThumbnailsUseCase(context)
    private val analyseUseCase = AnalyseUseCase(context)
    val permManager = StoragePermissionManager(context)

    sealed class CleanState {
        object Idle : CleanState()
        object Analysing : CleanState()
        data class AnalysisDone(
            val result: AnalyseResult
        ) : CleanState()
        object Running : CleanState()
        data class Done(
            val totalBytes: Long,
            val filesCount: Int,
            val errors: List<String>,
            val details: List<CategoryAnalysis>
        ) : CleanState()
        data class Error(val message: String) : CleanState()
    }

    private val _state = MutableStateFlow<CleanState>(CleanState.Idle)
    val state: StateFlow<CleanState> = _state

    // Paramètres mémorisés pour le nettoyage après analyse
    private var lastParams: CleanParams? = null

    data class CleanParams(
        val clearCache: Boolean,
        val clearTemp: Boolean,
        val clearBrowser: Boolean,
        val clearApk: Boolean,
        val clearThumbnails: Boolean,
        val clearWhatsApp: Boolean,
        val clearTelegram: Boolean,
        val whatsAppDocFile: DocumentFile?,
        val telegramDocFile: DocumentFile?
    )

    fun runAnalysis(params: CleanParams) {
        lastParams = params
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = CleanState.Analysing
            try {
                val result = analyseUseCase.execute(
                    scanCache = params.clearCache,
                    scanTemp = params.clearTemp,
                    scanBrowser = params.clearBrowser,
                    scanApk = params.clearApk,
                    scanThumbnails = params.clearThumbnails,
                    whatsAppDocFile = if (params.clearWhatsApp) params.whatsAppDocFile else null,
                    telegramDocFile = if (params.clearTelegram) params.telegramDocFile else null
                )
                _state.value = CleanState.AnalysisDone(result)
            } catch (e: Exception) {
                _state.value = CleanState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun runCleaner() {
        val params = lastParams ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = CleanState.Running
            try {
                var totalBytes = 0L
                var filesCount = 0
                val allErrors = mutableListOf<String>()
                val details = mutableListOf<CategoryAnalysis>()

                if (params.clearCache) {
                    val result = clearCacheUseCase.execute()
                    totalBytes += result.clearedBytes
                    allErrors += result.errors
                    details.add(CategoryAnalysis("Cache des apps", result.clearedBytes, 0))
                }

                if (params.clearTemp) {
                    val result = clearTempUseCase.execute()
                    totalBytes += result.clearedBytes
                    filesCount += result.filesCount
                    allErrors += result.errors
                    details.add(CategoryAnalysis("Fichiers temporaires", result.clearedBytes, result.filesCount))
                }

                if (params.clearBrowser) {
                    val result = clearBrowserUseCase.execute()
                    totalBytes += result.clearedBytes
                    allErrors += result.errors
                    details.add(CategoryAnalysis("Données navigateur", result.clearedBytes, 0))
                }

                if (params.clearApk) {
                    val result = clearApkUseCase.execute()
                    totalBytes += result.clearedBytes
                    filesCount += result.filesCount
                    allErrors += result.errors
                    details.add(CategoryAnalysis("APK téléchargés", result.clearedBytes, result.filesCount))
                }

                if (params.clearThumbnails) {
                    val result = clearThumbnailsUseCase.execute()
                    totalBytes += result.clearedBytes
                    filesCount += result.filesCount
                    allErrors += result.errors
                    details.add(CategoryAnalysis("Miniatures médias", result.clearedBytes, result.filesCount))
                }

                if (params.clearWhatsApp && params.whatsAppDocFile != null) {
                    val (bytes, count) = permManager.deleteRecursively(params.whatsAppDocFile)
                    totalBytes += bytes
                    filesCount += count
                    details.add(CategoryAnalysis("WhatsApp", bytes, count))
                }

                if (params.clearTelegram && params.telegramDocFile != null) {
                    val (bytes, count) = permManager.deleteRecursively(params.telegramDocFile)
                    totalBytes += bytes
                    filesCount += count
                    details.add(CategoryAnalysis("Telegram", bytes, count))
                }

                _state.value = CleanState.Done(
                    totalBytes = totalBytes,
                    filesCount = filesCount,
                    errors = allErrors,
                    details = details
                )

            } catch (e: Exception) {
                _state.value = CleanState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    fun reset() {
        lastParams = null
        _state.value = CleanState.Idle
    }
}