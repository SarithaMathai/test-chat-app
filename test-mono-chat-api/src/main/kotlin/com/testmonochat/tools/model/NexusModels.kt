package com.testmonochat.tools.model

/**
 * A single Nexus attribute item (color family, color name, pattern, etc.).
 */
data class NexusItem(
    val id: String,
    val name: String,
    val type: String
)

/**
 * A [NexusItem] enriched with a fuzzy-match confidence score (0.0–1.0).
 */
data class ScoredNexusItem(
    val id: String,
    val name: String,
    val type: String,
    val confidence: Double
)

// ---------------------------------------------------------------------------
// Grouped Attributes
// ---------------------------------------------------------------------------

data class AttributeGroup(
    val type: String,
    val attributes: List<NexusItem>
)

data class GroupedAttributesResponse(
    val grouped: List<AttributeGroup>,
    val totalTypes: Int
)

data class AttributeTypesResponse(
    val types: List<String>,
    val total: Int
)

data class AttributesByTypeResponse(
    val type: String,
    val attributes: List<NexusItem>,
    val total: Int
)

data class AttributesCountResponse(
    val status: String = "ok",
    val totalItems: Int
)

// ---------------------------------------------------------------------------
// Match Request / Response
// ---------------------------------------------------------------------------

data class MatchAttributesRequest(
    val colorFamily: String? = null,
    val colorName: String? = null,
    val pattern: String? = null,
    val hexColors: List<String>? = null
)

data class ColorMatches(
    val colorFamily: List<ScoredNexusItem> = emptyList(),
    val colorName: List<ScoredNexusItem> = emptyList(),
    val colorTone: List<ScoredNexusItem> = emptyList()
)

data class MatchAttributesResponse(
    val colorMatches: ColorMatches,
    val patternMatches: List<ScoredNexusItem>
)
