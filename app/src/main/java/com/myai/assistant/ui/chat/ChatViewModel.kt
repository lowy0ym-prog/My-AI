package com.myai.assistant.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myai.assistant.MyAiApplication
import com.myai.assistant.data.db.MessageEntity
import com.myai.assistant.data.db.MessageRole
import com.myai.assistant.data.repository.ChatRepository
import com.myai.assistant.inference.LlamaEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatUiState(
    val conversationId: Long = -1L,
    val messages: List<MessageEntity> = emptyList(),
    val draft: String = "",
    val isGenerating: Boolean = false,
    val modelLoaded: Boolean = false
)

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as MyAiApplication
    private val repository = ChatRepository(app.database.conversationDao(), app.database.messageDao())
    private val engine = LlamaEngine()

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    private var generationJob: Job? = null

    fun open(conversationId: Long) {
        _uiState.update { it.copy(conversationId = conversationId) }
        viewModelScope.launch {
            repository.observeMessages(conversationId).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }
    }

    fun updateDraft(text: String) {
        _uiState.update { it.copy(draft = text) }
    }

    fun send() {
        val state = _uiState.value
        val text = state.draft.trim()
        if (text.isEmpty() || state.conversationId < 0) return
        _uiState.update { it.copy(draft = "") }

        viewModelScope.launch {
            repository.addMessage(state.conversationId, MessageRole.USER, text)

            if (!engine.isLoaded) {
                repository.addMessage(
                    state.conversationId,
                    MessageRole.ASSISTANT,
                    "No local model is loaded yet. Go to Models to download or import a GGUF model first.",
                    isError = true
                )
                return@launch
            }

            _uiState.update { it.copy(isGenerating = true) }
            val assistantId = repository.addMessage(state.conversationId, MessageRole.ASSISTANT, "")
            val buffer = StringBuilder()

            generationJob = launch {
                engine.generate(text).collect { token ->
                    buffer.append(token)
                    repository.updateMessage(
                        MessageEntity(
                            id = assistantId,
                            conversationId = state.conversationId,
                            role = MessageRole.ASSISTANT,
                            content = buffer.toString(),
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
                _uiState.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        _uiState.update { it.copy(isGenerating = false) }
    }

    fun regenerateFrom(message: MessageEntity) {
        viewModelScope.launch {
            repository.truncateFrom(message.conversationId, message.id)
            send()
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.unload()
    }
}
