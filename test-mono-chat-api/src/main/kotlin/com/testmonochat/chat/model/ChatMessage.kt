package com.testmonochat.chat.model

import java.time.LocalDateTime
import java.util.UUID

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String,
    val content: String,
    val timestamp: LocalDateTime = LocalDateTime.now()
)
