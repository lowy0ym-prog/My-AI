package com.myai.assistant.ui.sidebar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.myai.assistant.MyAiApplication
import com.myai.assistant.data.db.ConversationEntity
import com.myai.assistant.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConversationListViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as MyAiApplication
    private val repository = ChatRepository(app.database.conversationDao(), app.database.messageDao())

    private val searchQuery = MutableStateFlow("")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val conversations: StateFlow<List<ConversationEntity>> = searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) repository.observeConversations() else repository.searchConversations(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchChange(query: String) {
        searchQuery.value = query
    }

    fun createConversation(onCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repository.createConversation()
            onCreated(id)
        }
    }

    fun rename(id: Long, title: String) {
        viewModelScope.launch { repository.renameConversation(id, title) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.deleteConversation(id) }
    }
}
