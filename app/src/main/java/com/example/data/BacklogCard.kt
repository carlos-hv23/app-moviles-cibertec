package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backlog_cards")
data class BacklogCard(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val columnId: Int, // 0: Core Features, 1: Collaboration Tools, 2: User Management, 3: Analytics & Insights
    val priority: String, // "High", "Low", "None"
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
