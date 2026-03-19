package com.testmonochat.chat.controller

import com.testmonochat.chat.model.ChatMessage
import com.testmonochat.chat.service.ChatService
import org.slf4j.LoggerFactory
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

data class ChatInput(val sender: String, val content: String)

@Controller
class ChatWebSocketController(
    private val chatService: ChatService,
    private val messagingTemplate: SimpMessagingTemplate
) {

    private val log = LoggerFactory.getLogger(javaClass)

    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    fun send(input: ChatInput): ChatMessage {
        log.info("Received WebSocket chat.send from sender={}", input.sender)
        val (userMessage, agentReply) = chatService.addMessageWithAgentReply(input.sender, input.content)

        log.info("Broadcasting agent reply for sender={}", input.sender)
        messagingTemplate.convertAndSend("/topic/messages", agentReply)

        return userMessage
    }
}
