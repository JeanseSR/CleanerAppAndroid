package com.cleanerapp.cleaner

import android.app.Application
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

    sealed class CleanState {
        object Idle : CleanState()
        object Running : CleanState()
        data class Done(
            val totalBytes: Long,
            val filesCount: Int,
            val errors: List<String>
        ) : CleanState()
        data class Error(val message: String) : CleanState()
    }

    private val _state = MutableStateFlow<CleanState>(CleanState.Idle)
    val state: StateFlow<CleanState> = _state

    fun runCleaner(
        clearCache: Boolean = true,
        clearTemp: Boolean = true,
        clearBrowser: Boolean = true
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = CleanState.Running

            try {
                var totalBytes = 0L
                var filesCount = 0
                val allErrors = mutableListOf<String>()

                if (clearCache) {
                    val result = clearCacheUseCase.execute()
                    totalBytes += result.clearedBytes
                    allErrors += result.errors
                }

                if (clearTemp) {
                    val result = clearTempUseCase.execute()
                    totalBytes += result.clearedBytes
                    filesCount += result.filesCount
                    allErrors += result.errors
                }

                if (clearBrowser) {
                    val result = clearBrowserUseCase.execute()
                    totalBytes += result.clearedBytes
                    allErrors += result.errors
                }

                _state.value = CleanState.Done(
                    totalBytes = totalBytes,
                    filesCount = filesCount,
                    errors = allErrors
                )

            } catch (e: Exception) {
                _state.value = CleanState.Error(
                    message = e.message ?: "Erreur inconnue"
                )
            }
        }
    }

    fun reset() {
        _state.value = CleanState.Idle
    }
}