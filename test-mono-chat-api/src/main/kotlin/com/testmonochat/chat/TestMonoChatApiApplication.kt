package com.testmonochat.chat

import com.testmonochat.common.config.ToolsProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.testmonochat"])
@EnableConfigurationProperties(ToolsProperties::class)
class TestMonoChatApiApplication

fun main(args: Array<String>) {
    runApplication<TestMonoChatApiApplication>(*args)
}
