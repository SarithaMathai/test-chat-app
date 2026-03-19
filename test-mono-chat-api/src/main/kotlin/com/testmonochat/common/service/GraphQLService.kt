package com.testmonochat.common.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.testmonochat.common.config.ToolsProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

/**
 * Thin wrapper around the external GraphQL API.
 *
 * Every public method sends a query string and returns the parsed JSON tree,
 * letting callers navigate the response structure as needed.
 */
@Service
class GraphQLService(
    private val webClient: WebClient,
    private val properties: ToolsProperties,
    private val objectMapper: ObjectMapper
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Execute a GraphQL query and return the root [JsonNode] of the response.
     *
     * @return the full response body, or `null` on error.
     */
    fun execute(query: String, variables: Map<String, Any?> = emptyMap()): JsonNode? {
        val body = buildMap<String, Any?> {
            put("query", query)
            if (variables.isNotEmpty()) put("variables", variables)
        }

        return try {
            val response = webClient.post()
                .uri(properties.graphql.url)
                .header("Content-Type", "application/json")
                .header("X-API-KEY", properties.apiKey)
                .header("tenant-id", properties.tenantId)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String::class.java)
                .block()

            response?.let { objectMapper.readTree(it) }
        } catch (e: Exception) {
            log.error("GraphQL request failed: {}", e.message, e)
            null
        }
    }
}
