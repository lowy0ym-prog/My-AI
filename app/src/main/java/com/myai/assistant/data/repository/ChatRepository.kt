package com.myai.assistant.data.repository

import com.myai.assistant.data.db.ConversationDao
import com.myai.assistant.data.db.ConversationEntity
import com.myai.assistant.data.db.MessageDao
import com.myai.assistant.data.db.MessageEntity
import com.myai.assistant.data.db.MessageRole
import kotlinx.coroutines.flow.Flow

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) {
    fun observeConversations(): Flow<List<ConversationEntity>> = conversationDao.observeAll()

    fun searchConversations(query: String): Flow<List<ConversationEntity>> =
        conversationDao.search(query)

    fun observeMessages(conversationId: Long): Flow<List<MessageEntity>> =
        messageDao.observeForConversation(conversationId)

    suspend fun createConversation(title: String = "New chat", systemPrompt: String = ""): Long {
        val now = System.currentTimeMillis()
        return conversationDao.insert(
            ConversationEntity(title = title, createdAt = now, updatedAt = now, systemPrompt = systemPrompt)
        )
    }

    suspend fun renameConversation(id: Long, title: String) {
        conversationDao.rename(id, title, System.currentTimeMillis())
    }

    suspend fun deleteConversation(id: Long) {
        conversationDao.delete(id)
    }

    suspend fun addMessage(conversationId: Long, role: MessageRole, content: String, isError: Boolean = false): Long {
        val id = messageDao.insert(
            MessageEntity(
                conversationId = conversationId,
                role = role,
                content = content,
                createdAt = System.currentTimeMillis(),
                isError = isError
            )
        )
        conversationDao.getById(conversationId)?.let {
            conversationDao.update(it.copy(updatedAt = System.currentTimeMillis()))
        }
        return id
    }

    suspend fun updateMessage(message: MessageEntity) = messageDao.update(message)

    suspend fun truncateFrom(conversationId: Long, fromMessageId: Long) =
        messageDao.deleteFrom(conversationId, fromMessageId)
}
