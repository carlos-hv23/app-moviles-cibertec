package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val senderEmail: String,
    val senderName: String,
    val recipientEmail: String, // "all" for group/general backlog discussion, or the recipient email for DMs
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val fileUrl: String? = null // Optional URL for shared workspace resources (like ZIP files)
) : Serializable
