package com.myvoice.app.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "thoughts")
data class Thought(
    @PrimaryKey val id: String,
    val title: String,
    val transcript: String,
    val reply: String,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long,
    val isFavorite: Boolean = false,
    /** VOICE or TEXT */
    val source: String = "TEXT",
    /** DEVICE / DEEPGRAM / SARVAM, empty for typed thoughts */
    val sttProvider: String = "",
    val llmModel: String = "",
    val durationMs: Long = 0,
    /** Whether this thought has been pushed to Appwrite cloud sync. */
    val synced: Boolean = false
)
