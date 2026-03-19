package com.testmonochat.tools.controller

import com.testmonochat.tools.model.*
import com.testmonochat.tools.service.NexusAttributeService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for Nexus attribute lookups and fuzzy matching.
 *
 * Endpoints:
 * - GET  /api/v1/attributes/count           — health check + item count
 * - GET  /api/v1/attributes/grouped         — all attributes grouped by type
 * - GET  /api/v1/attributes/types           — list of unique attribute types
 * - GET  /api/v1/attributes/by-type/{type}  — attributes for a specific type
 * - POST /api/v1/attributes/match           — fuzzy match color/pattern to Nexus IDs
 */
@RestController
@RequestMapping("/api/v1/attributes")
class NexusAttributeController(private val nexusAttributeService: NexusAttributeService) {

    @GetMapping("/count")
    fun count(): ResponseEntity<AttributesCountResponse> =
        ResponseEntity.ok(
            AttributesCountResponse(totalItems = nexusAttributeService.allItems.size)
        )

    @GetMapping("/grouped")
    fun grouped(
        @RequestParam(name = "type_filter", required = false) typeFilter: String?,
        @RequestParam(required = false) search: String?
    ): ResponseEntity<GroupedAttributesResponse> =
        ResponseEntity.ok(nexusAttributeService.getGrouped(typeFilter, search))

    @GetMapping("/types")
    fun types(): ResponseEntity<AttributeTypesResponse> =
        ResponseEntity.ok(nexusAttributeService.getTypes())

    @GetMapping("/by-type/{typeName}")
    fun byType(@PathVariable typeName: String): ResponseEntity<AttributesByTypeResponse> =
        ResponseEntity.ok(nexusAttributeService.getByType(typeName))

    @PostMapping("/match")
    fun match(@RequestBody request: MatchAttributesRequest): ResponseEntity<Any> {
        if (request.colorFamily == null && request.colorName == null
            && request.pattern == null && request.hexColors.isNullOrEmpty()
        ) {
            return ResponseEntity.badRequest().body(
                mapOf(
                    "error" to "missing_input",
                    "message" to "At least one of colorFamily, colorName, pattern, or hexColors is required."
                )
            )
        }
        return ResponseEntity.ok(nexusAttributeService.matchAttributes(request))
    }
}
