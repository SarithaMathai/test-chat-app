package com.testmonochat.ui.controller

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class ChatPageController {

    @GetMapping("/")
    fun chatPage(): String = "chat"
}
