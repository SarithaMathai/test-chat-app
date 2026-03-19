package com.testmonochat.chat.controller

import com.testmonochat.chat.model.ChatMessage
import com.testmonochat.chat.service.ChatService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class SendMessageRequest(val sender: String, val content: String)

@RestController
@RequestMapping("/api/chat")
class ChatRestController(private val chatService: ChatService) {

    private val log = LoggerFactory.getLogger(javaClass)

    @GetMapping("/messages")
    fun getMessages(@RequestParam(defaultValue = "50") limit: Int): ResponseEntity<List<ChatMessage>> =
        ResponseEntity.ok(chatService.getRecentMessages(limit))

    @PostMapping("/messages")
    fun sendMessage(@RequestBody request: SendMessageRequest): ResponseEntity<List<ChatMessage>> {
        log.info("Received REST chat message from sender={}", request.sender)
        val (userMessage, agentReply) = chatService.addMessageWithAgentReply(request.sender, request.content)
        val result = listOfNotNull(userMessage, agentReply)
        return ResponseEntity.ok(result)
    }
}
