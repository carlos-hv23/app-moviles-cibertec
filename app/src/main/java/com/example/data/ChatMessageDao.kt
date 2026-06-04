package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Query("SELECT * FROM chat_messages WHERE recipientEmail = 'all' ORDER BY timestamp ASC")
    fun getGeneralMessages(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE (senderEmail = :user1 AND recipientEmail = :user2) OR (senderEmail = :user2 AND recipientEmail = :user1) ORDER BY timestamp ASC")
    fun getDirectMessages(user1: String, user2: String): Flow<List<ChatMessage>>

    @Insert
    suspend fun insertMessage(message: ChatMessage)

    @Query("DELETE FROM chat_messages")
    suspend fun clearMessages()
}
