package com.testmonochat.tools.service

import com.testmonochat.common.service.ThinkTankService
import com.testmonochat.tools.model.ImageAnalysisResult
import com.testmonochat.tools.model.RgbColorResult
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.CompletableFuture

/**
 * Coordinates image-based analysis by fetching thumbnails, encoding them,
 * and delegating to the Think Tank LLM for color/pattern detection.
 */
@Service
class ColorPatternAnalyzerService(
    private val attachmentService: AttachmentService,
    private val thinkTankService: ThinkTankService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // ----- internal single-item helpers --------------------------------------

    internal fun analyzeAttachment(attachmentId: String): ImageAnalysisResult? {
        val imageUrl = attachmentService.getBestThumbnailUrl(attachmentId)
        if (imageUrl == null) {
            log.warn("No thumbnail found for attachment {}", attachmentId)
            return null
        }

        val dataUrl = attachmentService.fetchAndEncodeImage(imageUrl)
        if (dataUrl == null) {
            log.warn("Failed to encode image for attachment {}", attachmentId)
            return null
        }

        return try {
            thinkTankService.analyzeImage(attachmentId, dataUrl)
        } catch (e: Exception) {
            log.error("Attachment analysis failed for {}: {}", attachmentId, e.message, e)
            null
        }
    }

    /**
     * Identify a color name and family from RGB values via the LLM.
     */
    internal fun identifyColorFromRgb(red: Int, green: Int, blue: Int): RgbColorResult? {
        return try {
            thinkTankService.identifyColorFromRgb(red, green, blue)
        } catch (e: Exception) {
            log.error("RGB color identification failed: {}", e.message, e)
            null
        }
    }

    // ----- batch operations --------------------------------------------------

    /**
     * Analyze multiple attachments in parallel.
     *
     * Each attachment is processed independently; a failure in one does
     * not affect the others.
     */
    fun analyzeAttachments(attachmentIds: List<String>): Map<String, ImageAnalysisResult?> {
        val futures = attachmentIds.associateWith { id ->
            CompletableFuture.supplyAsync { analyzeAttachment(id) }
        }
        return futures.mapValues { (_, future) -> future.join() }
    }

    /**
     * Identify colors for multiple RGB values in parallel.
     */
    fun identifyColorsFromRgb(rgbValues: List<Triple<Int, Int, Int>>): List<RgbColorResult?> {
        val futures = rgbValues.map { (r, g, b) ->
            CompletableFuture.supplyAsync { identifyColorFromRgb(r, g, b) }
        }
        return futures.map { it.join() }
    }
}
