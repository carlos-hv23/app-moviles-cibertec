package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.BacklogCard
import com.example.data.BacklogRepository
import com.example.data.User
import com.example.data.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ColumnInfo(
    val id: Int,
    val name: String,
    val color: Long
) {
    fun getLocalizedName(lang: String): String {
        return when (id) {
            0 -> if (lang == "es") "Características" else if (lang == "pt") "Recursos Principais" else "Core Features"
            1 -> if (lang == "es") "Colaboración" else if (lang == "pt") "Colaboração" else "Collaboration Tools"
            2 -> if (lang == "es") "Gestión Usuarios" else if (lang == "pt") "Gerenciamento" else "User Management"
            3 -> if (lang == "es") "Métricas" else if (lang == "pt") "Análises" else "Analytics & Insights"
            else -> name
        }
    }
}

class BacklogViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: BacklogRepository
    private val userRepository: UserRepository

    val currentUserState = MutableStateFlow<User?>(null)
    val authError = MutableStateFlow<String?>(null)
    val authLoading = MutableStateFlow(false)

    // Reactive Chat and Collaboration Workspace States
    val registeredUsersState = MutableStateFlow<List<User>>(emptyList())
    val activeRecipientState = MutableStateFlow<User?>(null) // null represents the general board discussion
    val chatMessagesState = MutableStateFlow<List<com.example.data.ChatMessage>>(emptyList())
    private var chatCollectionJob: kotlinx.coroutines.Job? = null

    private val sharedPrefs = application.getSharedPreferences("backlog_prefs", Context.MODE_PRIVATE)
    
    val columns = listOf(
        ColumnInfo(0, "Core Features", 0xFFE91E63),
        ColumnInfo(1, "Collaboration Tools", 0xFFFB8C00),
        ColumnInfo(2, "User Management", 0xFF8E24AA),
        ColumnInfo(3, "Analytics & Insights", 0xFF4CAF50)
    )

    val searchQuery = MutableStateFlow("")

    val currentLanguageState = MutableStateFlow(sharedPrefs.getString("app_language", "es") ?: "es")

    fun setLanguage(langCode: String) {
        currentLanguageState.value = langCode
        sharedPrefs.edit().putString("app_language", langCode).apply()
    }

    val cardsState: StateFlow<List<BacklogCard>>

    init {
        val database = AppDatabase.getDatabase(application)
        repository = BacklogRepository(database.backlogCardDao())
        userRepository = UserRepository(database.userDao(), database.chatMessageDao())

        // Monitor currentUserState to trigger messages sync and user refreshes
        viewModelScope.launch {
            currentUserState.collect { user ->
                if (user != null) {
                    refreshRegisteredUsers()
                    observeChatMessages()
                }
            }
        }

        // Check for active session
        val savedEmail = sharedPrefs.getString("logged_in_email", null)
        if (savedEmail != null) {
            viewModelScope.launch {
                try {
                    val user = userRepository.getUserByEmail(savedEmail)
                    if (user != null) {
                        currentUserState.value = user
                    } else {
                        sharedPrefs.edit().remove("logged_in_email").apply()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Seed data if empty
        viewModelScope.launch {
            try {
                val currentList = repository.allCards.first()
                if (currentList.isEmpty()) {
                    val seedData = listOf(
                        BacklogCard(title = "Dashboard", columnId = 0, priority = "High", description = "Main landing portal displaying visual metrics and core stats overview."),
                        BacklogCard(title = "Calendar Integration", columnId = 0, priority = "High", description = "Bi-directional scheduling synchronizations for calendars and events."),
                        BacklogCard(title = "Task Management", columnId = 0, priority = "High", description = "Workflow controls to assign, delegate, write, and audit backlog tasks."),
                        
                        BacklogCard(title = "Project Management", columnId = 1, priority = "None", description = "Track project boards, milestones, and status metrics."),
                        BacklogCard(title = "Team Search", columnId = 1, priority = "None", description = "Advanced filtering engine for department, skills, and availability."),
                        BacklogCard(title = "Notifications System", columnId = 1, priority = "None", description = "Deliver key events via real-time alerts or emails."),
                        
                        BacklogCard(title = "Student Profiles", columnId = 2, priority = "None", description = "Complete database record of progress and profile characteristics."),
                        
                        BacklogCard(title = "Statistics Dashboard", columnId = 3, priority = "Low", description = "Overview of charts, histograms, data logs, and visual performance charts."),
                        BacklogCard(title = "Performance Analytics", columnId = 3, priority = "Low", description = "Deeper data queries mapping historical trends and backlog velocity.")
                    )
                    repository.insertAll(seedData)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        cardsState = repository.allCards
            .combine(searchQuery) { cards, query ->
                if (query.isBlank()) {
                    cards
                } else {
                    cards.filter { it.title.contains(query, ignoreCase = true) || it.description.contains(query, ignoreCase = true) }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }

    fun addCard(title: String, columnId: Int, priority: String, description: String = "") {
        viewModelScope.launch {
            repository.insert(
                BacklogCard(
                    title = title,
                    columnId = columnId,
                    priority = priority,
                    description = description
                )
            )
        }
    }

    fun updateCard(card: BacklogCard) {
        viewModelScope.launch {
            repository.update(card)
        }
    }

    fun deleteCard(cardId: Int) {
        viewModelScope.launch {
            repository.deleteById(cardId)
        }
    }

    fun moveCard(card: BacklogCard, newColumnId: Int) {
        viewModelScope.launch {
            repository.update(card.copy(columnId = newColumnId))
        }
    }

    fun signUp(email: String, password: String, displayName: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            try {
                if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
                    authError.value = "All fields are required"
                    return@launch
                }
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    authError.value = "Invalid email format"
                    return@launch
                }
                if (password.length < 6) {
                    authError.value = "Password must be at least 6 characters"
                    return@launch
                }
                val targetEmail = email.trim().lowercase()
                val existing = userRepository.getUserByEmail(targetEmail)
                if (existing != null) {
                    authError.value = "Account with this email already exists"
                } else {
                    val newUser = User(
                        email = targetEmail,
                        passwordSecret = password,
                        displayName = displayName.trim()
                    )
                    userRepository.registerUser(newUser)
                    currentUserState.value = newUser
                    sharedPrefs.edit().putString("logged_in_email", newUser.email).apply()
                    onSuccess()
                }
            } catch (e: Exception) {
                authError.value = e.localizedMessage ?: "Sign up failed"
            } finally {
                authLoading.value = false
            }
        }
    }

    fun logIn(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            try {
                if (email.isBlank() || password.isBlank()) {
                    authError.value = "Email and password are required"
                    return@launch
                }
                val targetEmail = email.trim().lowercase()
                val user = userRepository.getUserByEmail(targetEmail)
                if (user == null) {
                    authError.value = "Incorrect email or password"
                } else if (user.passwordSecret != password) {
                    authError.value = "Incorrect email or password"
                } else {
                    currentUserState.value = user
                    sharedPrefs.edit().putString("logged_in_email", user.email).apply()
                    onSuccess()
                }
            } catch (e: Exception) {
                authError.value = e.localizedMessage ?: "Login failed"
            } finally {
                authLoading.value = false
            }
        }
    }

    fun refreshRegisteredUsers() {
        viewModelScope.launch {
            try {
                var list = userRepository.getAllUsers()
                if (list.isEmpty()) {
                    // Seed default workspace personas so there's always someone to chat with
                    val seedUsers = listOf(
                        User("maria.gomez@example.com", "password123", "María Gómez"),
                        User("juan.perez@example.com", "password123", "Juan Pérez"),
                        User("ana.silva@example.com", "password123", "Ana Silva")
                    )
                    seedUsers.forEach { userRepository.registerUser(it) }
                    list = userRepository.getAllUsers()
                }
                registeredUsersState.value = list
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun selectRecipient(recipient: User?) {
        activeRecipientState.value = recipient
        observeChatMessages()
    }

    fun observeChatMessages() {
        chatCollectionJob?.cancel()
        val currentUser = currentUserState.value ?: return
        val recipient = activeRecipientState.value
        
        chatCollectionJob = viewModelScope.launch {
            val flow = if (recipient == null) {
                userRepository.getGeneralMessages()
            } else {
                userRepository.getDirectMessages(currentUser.email, recipient.email)
            }
            flow.collect { list ->
                chatMessagesState.value = list
            }
        }
    }

    fun sendMessage(text: String, fileUrl: String? = null) {
        val currentUser = currentUserState.value ?: return
        viewModelScope.launch {
            try {
                val message = com.example.data.ChatMessage(
                    senderEmail = currentUser.email,
                    senderName = currentUser.displayName,
                    recipientEmail = activeRecipientState.value?.email ?: "all",
                    messageText = text,
                    fileUrl = fileUrl
                )
                userRepository.insertMessage(message)
                refreshRegisteredUsers()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun signUpWithGoogle(userEmail: String?, onSuccess: () -> Unit) {
        viewModelScope.launch {
            authLoading.value = true
            authError.value = null
            try {
                val emailToUse = if (!userEmail.isNullOrBlank()) userEmail.trim().lowercase() else "milercenizario56@gmail.com"
                val displayNameToUse = if (emailToUse.contains("miler")) "Miler Cenizario (Google)" else "Google Backlog Guest"
                
                val existing = userRepository.getUserByEmail(emailToUse)
                val targetUser = if (existing != null) {
                    existing
                } else {
                    val newUser = User(
                        email = emailToUse,
                        passwordSecret = "google123",
                        displayName = displayNameToUse
                    )
                    userRepository.registerUser(newUser)
                    
                    // Seed some initial friendly chat to make the user feel welcome!
                    userRepository.insertMessage(
                        com.example.data.ChatMessage(
                            senderEmail = "maria.gomez@example.com",
                            senderName = "María Gómez",
                            recipientEmail = "all",
                            messageText = "¡Hola a todos! Bienvenidos al espacio de trabajo. Subí los últimos recursos del sprint en un ZIP.",
                            fileUrl = "https://gofile.io/d/sample_zipped_assets"
                        )
                    )
                    userRepository.insertMessage(
                        com.example.data.ChatMessage(
                            senderEmail = "juan.perez@example.com",
                            senderName = "Juan Pérez",
                            recipientEmail = "all",
                            messageText = "Excelente María, acabo de revisarlo. Quedó genial.",
                            fileUrl = null
                        )
                    )
                    userRepository.insertMessage(
                        com.example.data.ChatMessage(
                            senderEmail = "maria.gomez@example.com",
                            senderName = "María Gómez",
                            recipientEmail = newUser.email,
                            messageText = "¡Hola ${newUser.displayName}! Bienvenido a tus mensajes directos.",
                            fileUrl = "https://gofile.io/d/onboarding_guide"
                        )
                    )
                    newUser
                }
                currentUserState.value = targetUser
                sharedPrefs.edit().putString("logged_in_email", targetUser.email).apply()
                refreshRegisteredUsers()
                onSuccess()
            } catch (e: Exception) {
                authError.value = e.localizedMessage ?: "Google Authentication Failed"
            } finally {
                authLoading.value = false
            }
        }
    }

    fun logOut() {
        currentUserState.value = null
        authError.value = null
        sharedPrefs.edit().remove("logged_in_email").apply()
    }
}
