package com.myai.assistant.ui.models

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myai.assistant.MyAiApplication
import com.myai.assistant.inference.DownloadProgress
import com.myai.assistant.inference.EngineLoadState
import com.myai.assistant.inference.ModelCatalog
import com.myai.assistant.inference.ModelDownloader
import com.myai.assistant.inference.ModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ModelUiState(
    val models: List<ModelInfo> = ModelCatalog.all,
    val downloadedIds: Set<String> = emptySet(),
    val progressById: Map<String, Float> = emptyMap(),
    val activeModelId: String? = null,
    val isLoadingModel: Boolean = false,
    val error: String? = null
)

class ModelManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MyAiApplication
    private val downloader = ModelDownloader(application)
    private val engineHolder = app.engineHolder

    private val _uiState = MutableStateFlow(ModelUiState())
    val uiState: StateFlow<ModelUiState> = _uiState

    init {
        refreshDownloaded()

        // Mirror the shared engine's state so "active" survives navigating
        // away from this screen and back (it's held on the Application, not
        // this ViewModel).
        viewModelScope.launch {
            engineHolder.activeModelId.collect { id ->
                _uiState.update { it.copy(activeModelId = id) }
            }
        }
        viewModelScope.launch {
            engineHolder.loadState.collect { state ->
                _uiState.update { it.copy(isLoadingModel = state == EngineLoadState.LOADING) }
            }
        }
        viewModelScope.launch {
            engineHolder.loadError.collect { message ->
                if (message != null) _uiState.update { it.copy(error = message) }
            }
        }
    }

    private fun refreshDownloaded() {
        val downloaded = ModelCatalog.all.filter { downloader.isDownloaded(it) }.map { it.id }.toSet()
        _uiState.update { it.copy(downloadedIds = downloaded) }
    }

    fun download(model: ModelInfo) {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
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
                                error = "Download of ${model.displayName} failed: ${progress.error.message ?: progress.error::class.simpleName}"
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

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }

    fun setActive(model: ModelInfo) {
        _uiState.update { it.copy(error = null) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                engineHolder.load(
                    modelId = model.id,
                    modelPath = downloader.localFileFor(model).absolutePath,
                    contextLength = model.contextLength
                )
            }
        }
    }
}
