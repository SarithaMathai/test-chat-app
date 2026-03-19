package com.testmonochat.tools.model

// ---------------------------------------------------------------------------
// Image Analysis
// ---------------------------------------------------------------------------

/**
 * LLM response from analyzing a product image.
 */
data class ImageAnalysisResult(
    val dominantColors: List<String> = emptyList(),
    val pattern: String = "unknown",
    val error: String? = null
)

data class ImageAnalysisResponse(
    val attachmentId: String,
    val dominantColors: List<String>,
    val pattern: String,
    val error: String? = null
)

// ---------------------------------------------------------------------------
// RGB Color Identification
// ---------------------------------------------------------------------------

data class RgbColorRequest(
    val red: Int,
    val green: Int,
    val blue: Int
)

data class RgbValues(
    val red: Int,
    val green: Int,
    val blue: Int
)

/**
 * LLM response from identifying a color by RGB values.
 */
data class RgbColorResult(
    val colorName: String = "",
    val colorFamily: String = "",
    val hexCode: String = "",
    val rgb: RgbValues? = null,
    val error: String? = null
)

// ---------------------------------------------------------------------------
// POST /analyze with body
// ---------------------------------------------------------------------------

data class AnalyzePostRequest(
    val imageUrl: String? = null,
    val imageData: String? = null
)

// ---------------------------------------------------------------------------
// Batch operations
// ---------------------------------------------------------------------------

/**
 * Request to analyze multiple attachments in a single call.
 */
data class BatchAnalyzeRequest(
    val attachmentIds: List<String> = emptyList()
)

/**
 * Individual result within a batch analysis response.
 */
data class BatchAnalyzeItemResult(
    val attachmentId: String,
    val dominantColors: List<String>? = null,
    val pattern: String? = null,
    val error: String? = null
)

/**
 * Aggregated response for a batch attachment analysis.
 */
data class BatchAnalyzeResponse(
    val results: List<BatchAnalyzeItemResult>,
    val total: Int,
    val succeeded: Int,
    val failed: Int
)

/**
 * Request to identify multiple colors from RGB values in a single call.
 */
data class BatchRgbRequest(
    val colors: List<RgbColorRequest> = emptyList()
)

/**
 * Individual result within a batch RGB identification response.
 */
data class BatchRgbItemResult(
    val input: RgbColorRequest,
    val colorName: String? = null,
    val colorFamily: String? = null,
    val hexCode: String? = null,
    val error: String? = null
)

/**
 * Aggregated response for a batch RGB color identification.
 */
data class BatchRgbResponse(
    val results: List<BatchRgbItemResult>,
    val total: Int,
    val succeeded: Int,
    val failed: Int
)
