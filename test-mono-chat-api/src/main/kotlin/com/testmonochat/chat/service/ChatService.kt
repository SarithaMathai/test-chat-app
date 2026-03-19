package com.testmonochat.chat.service

import com.testmonochat.chat.model.ChatMessage
import com.testmonochat.common.service.ThinkTankService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentLinkedDeque

@Service
class ChatService(private val thinkTankService: ThinkTankService) {
    private val messages = ConcurrentLinkedDeque<ChatMessage>()
    private val maxMessages = 1000
    private val log = LoggerFactory.getLogger(javaClass)
    private val technicalDifficultyMessage = "There is a technical difficulty. Please try again in a moment."

    fun addMessage(sender: String, content: String): ChatMessage {
        val message = ChatMessage(sender = sender, content = content)
        storeMessage(message)
        return message
    }

    fun addMessageWithAgentReply(sender: String, content: String): Pair<ChatMessage, ChatMessage> {
        val userMessage = ChatMessage(sender = sender, content = content)
        storeMessage(userMessage)
        log.info("Stored user message for sender={}", sender)

        val agentReply = getAgentResponse(content)
        storeMessage(agentReply)
        log.info("Stored agent reply for sender={}", sender)

        return Pair(userMessage, agentReply)
    }

    fun getAgentResponse(userContent: String): ChatMessage {
        return try {
            val payload = mapOf("message" to userContent)
            log.info("Invoking agent with payload keys={}", payload.keys)
            val response = thinkTankService.invokeAgent(payload)
            if (!response.isNullOrBlank()) {
                log.info("Agent invoke returned {} characters", response.length)
                ChatMessage(sender = "Agent", content = response)
            } else {
                log.warn("Agent invoke returned no response body")
                createTechnicalDifficultyReply()
            }
        } catch (e: Exception) {
            log.error("Agent invoke failed: {}", e.message, e)
            createTechnicalDifficultyReply()
        }
    }

    fun getMessages(): List<ChatMessage> = messages.toList()

    fun getRecentMessages(limit: Int = 50): List<ChatMessage> =
        messages.toList().takeLast(limit)

    private fun storeMessage(message: ChatMessage) {
        messages.addLast(message)
        if (messages.size > maxMessages) {
            messages.pollFirst()
        }
    }

    private fun createTechnicalDifficultyReply(): ChatMessage =
        ChatMessage(sender = "Agent", content = technicalDifficultyMessage)
}
