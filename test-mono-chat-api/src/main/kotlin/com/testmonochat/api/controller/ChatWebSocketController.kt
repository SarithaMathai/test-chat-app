package com.testmonochat.api.controller

import com.testmonochat.api.model.ChatMessage
import com.testmonochat.api.service.ChatService
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.stereotype.Controller

data class ChatInput(val sender: String, val content: String)

@Controller
class ChatWebSocketController(private val chatService: ChatService) {

    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    fun send(input: ChatInput): ChatMessage =
        chatService.addMessage(input.sender, input.content)
}
