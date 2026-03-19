package com.testmonochat.common.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.testmonochat.common.config.ToolsProperties
import com.testmonochat.tools.constant.PromptTemplates
import com.testmonochat.tools.model.ImageAnalysisResult
import com.testmonochat.tools.model.RgbColorResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

/**
 * Client for the Think Tank platform.
 *
 * Think Tank exposes multiple capabilities:
 * - **Model Garden** (`/gen_ai_model_requests/v1/chat/completions`) — LLM chat completions
 * - **Agent Invoke** (`/agent/invoke`) — autonomous agent execution
 *
 * This service currently supports two Model Garden operations:
 * - **image-analyzer**: sends a base64 image for dominant-color + pattern detection
 * - **rgb-color-identifier**: converts RGB values to a named color and family
 */
@Service
class ThinkTankService(
    private val webClient: WebClient,
    private val properties: ToolsProperties,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // ----- Model Garden capability -------------------------------------------

    /**
     * Analyze a product image for dominant colors and pattern.
     *
     * @param attachmentId identifier for logging/tracking
     * @param dataUrl      base64-encoded image as a `data:image/...;base64,...` string
     */
    fun analyzeImage(attachmentId: String, dataUrl: String): ImageAnalysisResult? {
        val messages = listOf(
            mapOf("role" to "system", "content" to PromptTemplates.IMAGE_ANALYZER_SYSTEM_PROMPT),
            mapOf(
                "role" to "user",
                "content" to listOf(
                    mapOf("type" to "image_url", "image_url" to mapOf("url" to dataUrl))
                )
            )
        )
        val json = callModelGarden(PromptTemplates.IMAGE_ANALYZER_MODEL, messages) ?: return null
        return parseJson(json, ImageAnalysisResult::class.java)
    }

    /**
     * Identify a color name and family from RGB values.
     */
    fun identifyColorFromRgb(red: Int, green: Int, blue: Int): RgbColorResult? {
        val userContent = "red=$red, green=$green, blue=$blue"
        val messages = listOf(
            mapOf("role" to "system", "content" to PromptTemplates.RGB_COLOR_IDENTIFIER_SYSTEM_PROMPT),
            mapOf("role" to "user", "content" to userContent)
        )
        val json = callModelGarden(PromptTemplates.RGB_COLOR_IDENTIFIER_MODEL, messages) ?: return null
        return parseJson(json, RgbColorResult::class.java)
    }

    // ----- Agent Invoke capability --------------------------------------------

    /**
     * Invoke an agent with the given payload and return the raw response body.
     */
    fun invokeAgent(payload: Map<String, Any>): String? {
        return callAgentInvoke(payload)
    }

    // ----- internal helpers --------------------------------------------------

    private fun callAgentInvoke(payload: Map<String, Any>): String? {
        return try {
            log.info("Calling Think Tank agent-invoke at {}", properties.thinkTank.agentInvoke.url)
            val response = webClient.post()
                .uri(properties.thinkTank.agentInvoke.url)
                .header("Content-Type", "application/json")
                .header("X-API-KEY", properties.apiKey)
                .header("tenant-id", properties.tenantId)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String::class.java)
                .block()

            if (response.isNullOrBlank()) {
                log.warn("Think Tank Agent Invoke returned empty response")
                return null
            }

            log.info("Think Tank Agent Invoke returned {} characters", response.length)
            response
        } catch (e: Exception) {
            log.error("Think Tank Agent Invoke call failed: {}", e.message, e)
            null
        }
    }

    private fun callModelGarden(model: String, messages: List<Map<String, Any>>): String? {
        val body = mapOf(
            "model" to model,
            "messages" to messages,
            "temperature" to 1,
            "max_tokens" to 2048
        )

        return try {
            val response = webClient.post()
                .uri(properties.thinkTank.modelGarden.url)
                .header("Content-Type", "application/json")
                .header("X-API-KEY", properties.apiKey)
                .header("tenant-id", properties.tenantId)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String::class.java)
                .block()

            if (response.isNullOrBlank()) {
                log.warn("Think Tank Model Garden returned empty response for model={}", model)
                return null
            }

            // Extract the assistant message content from the chat-completions response
            val tree = objectMapper.readTree(response)
            tree.at("/choices/0/message/content")?.asText()
        } catch (e: Exception) {
            log.error("Think Tank Model Garden call failed (model={}): {}", model, e.message, e)
            null
        }
    }

    private fun <T> parseJson(raw: String, clazz: Class<T>): T? {
        return try {
            objectMapper.readValue(raw, clazz)
        } catch (e: Exception) {
            log.error("Failed to parse LLM JSON response: {}", e.message)
            null
        }
    }
}
