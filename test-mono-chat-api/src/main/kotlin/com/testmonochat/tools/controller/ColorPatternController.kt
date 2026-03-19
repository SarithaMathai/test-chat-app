package com.testmonochat.tools.controller

import com.testmonochat.tools.model.IdentifyRequest
import com.testmonochat.tools.service.ColorPatternService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for end-to-end color & pattern identification with
 * Nexus-standard recommended naming.
 *
 * POST /api/v1/identify/color-pattern
 *
 * Resolves a product, fabric/combination, or attachment through the full chain:
 * input → material → color/pattern detection → Nexus matching → recommended name.
 */
@RestController
@RequestMapping("/api/v1/identify")
class ColorPatternController(private val colorPatternService: ColorPatternService) {

    companion object {
        private val VALID_INPUT_TYPES = setOf("product", "fabric", "attachment")
    }

    @PostMapping("/color-pattern")
    fun identifyColorAndPattern(@RequestBody request: IdentifyRequest): ResponseEntity<Any> {
        if (request.inputType.isBlank() || request.inputId.isBlank()) {
            return ResponseEntity.badRequest().body(
                mapOf("error" to "missing_input", "message" to "inputType and inputId are required.")
            )
        }

        if (request.inputType.lowercase() !in VALID_INPUT_TYPES) {
            return ResponseEntity.badRequest().body(
                mapOf("error" to "invalid_input_type", "message" to "inputType must be one of: product, fabric, attachment.")
            )
        }

        val results = colorPatternService.identify(request)
        return ResponseEntity.ok(results)
    }
}
