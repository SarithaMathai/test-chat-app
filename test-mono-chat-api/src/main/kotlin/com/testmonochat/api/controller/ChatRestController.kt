package com.testmonochat.api.controller

import com.testmonochat.api.model.ChatMessage
import com.testmonochat.api.service.ChatService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

data class SendMessageRequest(val sender: String, val content: String)

@RestController
@RequestMapping("/api/chat")
class ChatRestController(private val chatService: ChatService) {

    @GetMapping("/messages")
    fun getMessages(@RequestParam(defaultValue = "50") limit: Int): ResponseEntity<List<ChatMessage>> =
        ResponseEntity.ok(chatService.getRecentMessages(limit))

    @PostMapping("/messages")
    fun sendMessage(@RequestBody request: SendMessageRequest): ResponseEntity<ChatMessage> =
        ResponseEntity.ok(chatService.addMessage(request.sender, request.content))
}
