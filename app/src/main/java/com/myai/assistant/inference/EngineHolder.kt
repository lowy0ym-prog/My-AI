package com.myai.assistant.inference

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class EngineLoadState { IDLE, LOADING, LOADED, ERROR }

/**
 * Owns the single [LlamaEngine] instance for the whole app (held by
 * [com.myai.assistant.MyAiApplication]), so the Models screen and the Chat
 * screen share the same loaded-model state instead of each creating their
 * own engine. Without this, pressing "Use" on a model updated only the
 * Models screen's local state, Chat never saw a loaded model, and
 * navigating back to Models lost the "active" selection entirely.
 */
class EngineHolder {
    val engine = LlamaEngine()

    private val _activeModelId = MutableStateFlow<String?>(null)
    val activeModelId: StateFlow<String?> = _activeModelId

    private val _loadState = MutableStateFlow(EngineLoadState.IDLE)
    val loadState: StateFlow<EngineLoadState> = _loadState

    private val _loadError = MutableStateFlow<String?>(null)
    val loadError: StateFlow<String?> = _loadError

    /** Blocking native load - call this from a background dispatcher. */
    @Synchronized
    fun load(modelId: String, modelPath: String, contextLength: Int) {
        _loadState.value = EngineLoadState.LOADING
        _loadError.value = null
        try {
            engine.load(modelPath, contextLength)
            if (engine.isLoaded) {
                _activeModelId.value = modelId
                _loadState.value = EngineLoadState.LOADED
            } else {
                _loadState.value = EngineLoadState.ERROR
                _loadError.value = "Model failed to load (native engine returned no handle). " +
                    "The file may be corrupt, an unsupported GGUF version, or too large for this device's memory."
            }
        } catch (t: Throwable) {
            _loadState.value = EngineLoadState.ERROR
            _loadError.value = t.message ?: "Unknown error loading model"
        }
    }

    fun unload() {
        engine.unload()
        _activeModelId.value = null
        _loadState.value = EngineLoadState.IDLE
    }
}
