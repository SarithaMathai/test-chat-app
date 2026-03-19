package com.testmonochat.tools.controller

import com.testmonochat.tools.model.BatchAnalyzeItemResult
import com.testmonochat.tools.model.BatchAnalyzeRequest
import com.testmonochat.tools.model.BatchAnalyzeResponse
import com.testmonochat.tools.model.BatchRgbItemResult
import com.testmonochat.tools.model.BatchRgbRequest
import com.testmonochat.tools.model.BatchRgbResponse
import com.testmonochat.tools.model.RgbColorRequest
import com.testmonochat.tools.service.ColorPatternAnalyzerService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for image-based color and pattern analysis.
 *
 * Endpoints:
 * - POST /api/v1/analyze/batch              — analyse multiple attachments
 * - POST /api/v1/analyze/color/rgb/batch    — identify multiple colors from RGB values
 */
@RestController
@RequestMapping("/api/v1/analyze")
class ColorPatternAnalyzerController(
    private val colorPatternAnalyzerService: ColorPatternAnalyzerService
) {

    // ----- POST /api/v1/analyze/batch ----------------------------------------

    @PostMapping("/batch")
    fun analyzeBatch(@RequestBody request: BatchAnalyzeRequest): ResponseEntity<Any> {
        if (request.attachmentIds.isEmpty()) {
            return ResponseEntity.badRequest().body(
                mapOf("error" to "empty_batch", "message" to "attachmentIds must not be empty")
            )
        }

        val resultsMap = colorPatternAnalyzerService.analyzeAttachments(request.attachmentIds)

        val items = request.attachmentIds.map { id ->
            val result = resultsMap[id]
            if (result != null && result.error == null) {
                BatchAnalyzeItemResult(
                    attachmentId = id,
                    dominantColors = result.dominantColors,
                    pattern = result.pattern
                )
            } else {
                BatchAnalyzeItemResult(
                    attachmentId = id,
                    error = result?.error ?: "Analysis failed or no thumbnail found"
                )
            }
        }

        val succeeded = items.count { it.error == null }
        return ResponseEntity.ok(
            BatchAnalyzeResponse(
                results = items,
                total = items.size,
                succeeded = succeeded,
                failed = items.size - succeeded
            )
        )
    }

    // ----- POST /api/v1/analyze/color/rgb/batch ------------------------------

    @PostMapping("/color/rgb/batch")
    fun identifyColorsBatch(@RequestBody request: BatchRgbRequest): ResponseEntity<Any> {
        if (request.colors.isEmpty()) {
            return ResponseEntity.badRequest().body(
                mapOf("error" to "empty_batch", "message" to "colors must not be empty")
            )
        }

        val invalid = request.colors.filter { !isValidRgb(it.red) || !isValidRgb(it.green) || !isValidRgb(it.blue) }
        if (invalid.isNotEmpty()) {
            return ResponseEntity.badRequest().body(
                mapOf("error" to "rgb_out_of_range", "message" to "All RGB values must be between 0 and 255")
            )
        }

        val triples = request.colors.map { Triple(it.red, it.green, it.blue) }
        val results = colorPatternAnalyzerService.identifyColorsFromRgb(triples)

        val items = request.colors.zip(results).map { (input, result) ->
            if (result != null && result.error == null) {
                BatchRgbItemResult(
                    input = input,
                    colorName = result.colorName,
                    colorFamily = result.colorFamily,
                    hexCode = result.hexCode
                )
            } else {
                BatchRgbItemResult(
                    input = input,
                    error = result?.error ?: "Identification failed"
                )
            }
        }

        val succeeded = items.count { it.error == null }
        return ResponseEntity.ok(
            BatchRgbResponse(
                results = items,
                total = items.size,
                succeeded = succeeded,
                failed = items.size - succeeded
            )
        )
    }

    private fun isValidRgb(value: Int): Boolean = value in 0..255
}
