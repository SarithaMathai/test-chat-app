package com.testmonochat.api.service

import com.testmonochat.api.model.ChatMessage
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentLinkedDeque

@Service
class ChatService {
    private val messages = ConcurrentLinkedDeque<ChatMessage>()
    private val maxMessages = 1000

    fun addMessage(sender: String, content: String): ChatMessage {
        val message = ChatMessage(sender = sender, content = content)
        messages.addLast(message)
        if (messages.size > maxMessages) {
            messages.pollFirst()
        }
        return message
    }

    fun getMessages(): List<ChatMessage> = messages.toList()

    fun getRecentMessages(limit: Int = 50): List<ChatMessage> =
        messages.toList().takeLast(limit)
}
