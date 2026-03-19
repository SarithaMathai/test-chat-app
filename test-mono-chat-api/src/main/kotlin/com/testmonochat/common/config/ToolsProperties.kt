package com.testmonochat.common.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Externalized configuration for external service URLs and credentials.
 *
 * Mapped from `tools.*` properties in application.yml.
 */
@ConfigurationProperties(prefix = "tools")
data class ToolsProperties(
    val graphql: GraphQLProperties = GraphQLProperties(),
    val thinkTank: ThinkTankProperties = ThinkTankProperties(),
    val apiKey: String = "",
    val tenantId: String = ""
) {
    data class GraphQLProperties(val url: String = "http://localhost:8080/graphql")
    data class ThinkTankProperties(
        val modelGarden: ModelGardenProperties = ModelGardenProperties(),
        val agentInvoke: AgentInvokeProperties = AgentInvokeProperties()
    )
    data class ModelGardenProperties(val url: String = "http://localhost:8080/gen_ai_model_requests/v1/chat/completions")
    data class AgentInvokeProperties(val url: String = "http://localhost:8080/agent/invoke")
}
