package com.testmonochat.tools.service

import com.testmonochat.common.service.GraphQLService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.util.Base64

/**
 * Fetches attachment thumbnails via GraphQL and encodes images to base64 data-URLs
 * suitable for the Vision API.
 */
@Service
class AttachmentService(
    private val graphQLService: GraphQLService,
    private val webClient: WebClient
) {

    private val log = LoggerFactory.getLogger(javaClass)

    companion object {
        private const val MIN_THUMBNAIL_WIDTH = 144
    }

    /**
     * Resolve the best thumbnail URL for an attachment via GraphQL.
     *
     * Prefers the smallest thumbnail whose width is at least [MIN_THUMBNAIL_WIDTH] px;
     * falls back to the original download URL when no qualifying thumbnail exists.
     *
     * @return the image URL, or `null` if the attachment cannot be resolved.
     */
    fun getBestThumbnailUrl(attachmentId: String): String? {
        val query = """
            query Attachment(${'$'}id: String!) {
                attachment(id: ${'$'}id) {
                    downloads {
                        downloadUrl
                        thumbnails { downloadUrl width }
                    }
                }
            }
        """.trimIndent()

        val data = graphQLService.execute(query, mapOf("id" to attachmentId))
        val downloads = data?.at("/data/attachment/downloads") ?: return null

        // Collect qualifying thumbnails (width >= MIN_THUMBNAIL_WIDTH)
        val thumbnails = downloads.flatMap { dl ->
            dl["thumbnails"]?.filter { it["width"]?.asInt(0) ?: 0 >= MIN_THUMBNAIL_WIDTH } ?: emptyList()
        }

        // Pick the smallest qualifying thumbnail, or fall back to the original download
        val best = thumbnails.minByOrNull { it["width"]?.asInt(Int.MAX_VALUE) ?: Int.MAX_VALUE }
        if (best != null) return best["downloadUrl"]?.asText()

        // Fallback: first available downloadUrl
        return downloads.firstOrNull()?.get("downloadUrl")?.asText()
    }

    /**
     * Download an image from [imageUrl] and return it as a base64 data-URL string
     * (`data:image/jpeg;base64,…`).
     *
     * @return the data-URL, or `null` on failure.
     */
    fun fetchAndEncodeImage(imageUrl: String): String? {
        return try {
            val bytes = webClient.get()
                .uri(imageUrl)
                .retrieve()
                .bodyToMono(ByteArray::class.java)
                .block()

            if (bytes == null || bytes.isEmpty()) {
                log.warn("Image fetch returned empty body: {}", imageUrl)
                return null
            }

            val encoded = Base64.getEncoder().encodeToString(bytes)
            "data:image/jpeg;base64,$encoded"
        } catch (e: Exception) {
            log.error("Failed to fetch/encode image from {}: {}", imageUrl, e.message, e)
            null
        }
    }
}
