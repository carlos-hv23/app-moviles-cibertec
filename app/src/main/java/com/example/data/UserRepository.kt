package com.example.data

import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val userDao: UserDao,
    private val chatMessageDao: ChatMessageDao
) {
    suspend fun getUserByEmail(email: String): User? = userDao.getUserByEmail(email)
    suspend fun registerUser(user: User) = userDao.insertUser(user)
    suspend fun getAllUsers(): List<User> = userDao.getAllUsers()

    fun getGeneralMessages(): Flow<List<ChatMessage>> = chatMessageDao.getGeneralMessages()

    fun getDirectMessages(user1: String, user2: String): Flow<List<ChatMessage>> =
        chatMessageDao.getDirectMessages(user1, user2)

    suspend fun insertMessage(message: ChatMessage) = chatMessageDao.insertMessage(message)

    suspend fun clearMessages() = chatMessageDao.clearMessages()
}
