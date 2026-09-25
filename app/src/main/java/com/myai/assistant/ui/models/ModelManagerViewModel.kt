package com.myai.assistant.ui.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myai.assistant.inference.DownloadProgress
import com.myai.assistant.inference.ModelCatalog
import com.myai.assistant.inference.ModelDownloader
import com.myai.assistant.inference.ModelInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ModelUiState(
    val models: List<ModelInfo> = ModelCatalog.all,
    val downloadedIds: Set<String> = emptySet(),
    val progressById: Map<String, Float> = emptyMap(),
    val activeModelId: String? = null,
    val error: String? = null
)

class ModelManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val downloader = ModelDownloader(application)

    private val _uiState = MutableStateFlow(ModelUiState())
    val uiState: StateFlow<ModelUiState> = _uiState

    init {
        refreshDownloaded()
    }

    private fun refreshDownloaded() {
        val downloaded = ModelCatalog.all.filter { downloader.isDownloaded(it) }.map { it.id }.toSet()
        _uiState.update { it.copy(downloadedIds = downloaded) }
    }

    fun download(model: ModelInfo) {
        viewModelScope.launch {
            downloader.download(model).collect { progress ->
                when (progress) {
                    is DownloadProgress.InProgress -> {
                        val fraction = if (progress.totalBytes > 0) progress.bytesRead / progress.totalBytes.toFloat() else 0f
                        _uiState.update { it.copy(progressById = it.progressById + (model.id to fraction)) }
                    }
                    is DownloadProgress.Done -> {
                        _uiState.update { it.copy(progressById = it.progressById - model.id) }
                        refreshDownloaded()
                    }
                    is DownloadProgress.Failed -> {
                        _uiState.update {
                            it.copy(
                                progressById = it.progressById - model.id,
                                error = "Download failed: ${progress.error.message}"
                            )
                        }
                    }
                }
            }
        }
    }

    fun delete(model: ModelInfo) {
        downloader.delete(model)
        refreshDownloaded()
    }

    fun setActive(model: ModelInfo) {
        _uiState.update { it.copy(activeModelId = model.id) }
        // Actual model load happens lazily on first message in ChatViewModel;
        // wiring the loaded engine instance through app-level state is the
        // next integration step once native inference is validated.
    }
}
