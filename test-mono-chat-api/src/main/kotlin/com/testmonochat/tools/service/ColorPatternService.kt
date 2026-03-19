package com.testmonochat.tools.service

import com.fasterxml.jackson.databind.JsonNode
import com.testmonochat.common.service.GraphQLService
import com.testmonochat.tools.model.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * End-to-end color & pattern identification orchestrator.
 *
 * Resolves a product, fabric/combination, or raw attachment through the full
 * pipeline: input → material → color/pattern detection → Nexus matching → recommended name.
 */
@Service
class ColorPatternService(
    private val graphQLService: GraphQLService,
    private val colorPatternAnalyzerService: ColorPatternAnalyzerService,
    private val nexusAttributeService: NexusAttributeService
) {

    private val log = LoggerFactory.getLogger(javaClass)

    // ---------------------------------------------------------------------------
    // Public entry point
    // ---------------------------------------------------------------------------

    /**
     * Process an identification request and return one [IdentifyResultEntry] per analysed material.
     */
    fun identify(request: IdentifyRequest): List<IdentifyResultEntry> {
        return when (request.inputType.lowercase()) {
            "attachment" -> identifyFromAttachment(request.inputId)
            "fabric" -> identifyFromFabric(request.inputId)
            "product" -> identifyFromProduct(request.inputId)
            else -> emptyList()
        }
    }

    // ---------------------------------------------------------------------------
    // Input-type handlers
    // ---------------------------------------------------------------------------

    private fun identifyFromAttachment(attachmentId: String): List<IdentifyResultEntry> {
        val analysis = colorPatternAnalyzerService.analyzeAttachment(attachmentId)
            ?: return listOf(unknownEntry(attachmentId))

        val entry = buildEntryFromAnalysis(attachmentId, analysis)
        return listOf(entry)
    }

    private fun identifyFromFabric(combinationId: String): List<IdentifyResultEntry> {
        val relatedIds = fetchCombinationMaterialIds(combinationId)
        if (relatedIds.isEmpty()) return listOf(unknownEntry(combinationId))

        val materials = fetchMaterials(relatedIds)
        return materials.mapNotNull { processMaterial(it) }
            .ifEmpty { listOf(unknownEntry(combinationId)) }
    }

    private fun identifyFromProduct(productId: String): List<IdentifyResultEntry> {
        val product = fetchProduct(productId) ?: return emptyList()

        val primaryMaterialName = product.productMaterials
            .firstOrNull { it.primary }?.name
            ?: return emptyList()

        val relatedIds = fetchCombinationMaterialIds(primaryMaterialName)
        if (relatedIds.isEmpty()) return listOf(unknownEntry(primaryMaterialName))

        val materials = fetchMaterials(relatedIds)
        return materials.mapNotNull { processMaterial(it) }
            .ifEmpty { listOf(unknownEntry(primaryMaterialName)) }
    }

    // ---------------------------------------------------------------------------
    // Material processing
    // ---------------------------------------------------------------------------

    /**
     * Process a single material record (Color or Artwork) and return a result entry.
     */
    private fun processMaterial(material: Material): IdentifyResultEntry? {
        return when (material.typeName) {
            "Color" -> processColorMaterial(material)
            "Artwork" -> processArtworkMaterial(material)
            else -> null
        }
    }

    private fun processColorMaterial(material: Material): IdentifyResultEntry {
        val red = material.red
        val green = material.green
        val blue = material.blue

        if (red != null && green != null && blue != null) {
            val colorInfo = colorPatternAnalyzerService.identifyColorFromRgb(red, green, blue)
            if (colorInfo != null) {
                return buildResultEntry(
                    materialId = material.id,
                    colorFamily = colorInfo.colorFamily,
                    colorName = colorInfo.colorName,
                    patternRaw = "solid",
                    hexColors = null
                )
            }
        }
        // Fallback when RGB is missing or identification fails
        return buildResultEntry(material.id, null, null, "solid", null)
    }

    private fun processArtworkMaterial(material: Material): IdentifyResultEntry? {
        val attachmentId = material.primaryAttachmentId ?: return null
        val analysis = colorPatternAnalyzerService.analyzeAttachment(attachmentId) ?: return null
        return buildEntryFromAnalysis(material.id, analysis)
    }

    // ---------------------------------------------------------------------------
    // Result builders
    // ---------------------------------------------------------------------------

    /**
     * Build a result entry from an [ImageAnalysisResult], resolving hex colours
     * through the RGB identification pipeline if available.
     */
    private fun buildEntryFromAnalysis(
        materialId: String,
        analysis: ImageAnalysisResult
    ): IdentifyResultEntry {
        val hexColors = analysis.dominantColors
        val patternRaw = analysis.pattern

        var colorFamily: String? = null
        var colorName: String? = null

        if (hexColors.isNotEmpty() && hexColors != listOf("mixed")) {
            val rgb = NexusAttributeService.hexToRgb(hexColors.first())
            if (rgb != null) {
                val colorInfo = colorPatternAnalyzerService.identifyColorFromRgb(rgb.first, rgb.second, rgb.third)
                if (colorInfo != null) {
                    colorFamily = colorInfo.colorFamily
                    colorName = colorInfo.colorName
                }
            }
        }

        return buildResultEntry(
            materialId = materialId,
            colorFamily = colorFamily,
            colorName = colorName,
            patternRaw = patternRaw,
            hexColors = hexColors.takeIf { it != listOf("mixed") }
        )
    }

    /**
     * Build one [IdentifyResultEntry] with Nexus IDs and a recommended display name.
     */
    private fun buildResultEntry(
        materialId: String,
        colorFamily: String?,
        colorName: String?,
        patternRaw: String?,
        hexColors: List<String>?
    ): IdentifyResultEntry {
        val matched = nexusAttributeService.matchSingle(colorFamily, colorName, patternRaw, hexColors)

        val nexusColorId = matched.colorFamily?.id
        val nexusColorName = matched.colorFamily?.name
        val nexusPatternId = matched.pattern?.id
        val nexusPatternName = matched.pattern?.name
        val displayColor = matched.colorName?.name ?: nexusColorName

        val recommendedName = buildRecommendedName(displayColor, nexusPatternName)

        return IdentifyResultEntry(
            materialId = materialId,
            recommendedName = recommendedName,
            nexusColorId = nexusColorId,
            nexusColorName = nexusColorName,
            nexusColorNameDetail = matched.colorName,
            nexusPatternId = nexusPatternId,
            nexusPatternName = nexusPatternName
        )
    }

    // ---------------------------------------------------------------------------
    // Naming rules
    // ---------------------------------------------------------------------------

    /**
     * Generate a recommended name using Nexus naming conventions:
     * - Solid / no pattern  → color only       (e.g. "Navy Blue")
     * - Pattern + white     → pattern only      (e.g. "Floral")
     * - Pattern + non-white → Color + Pattern   (e.g. "Red Plaid")
     */
    companion object {
        fun buildRecommendedName(
            nexusColorName: String?,
            nexusPatternName: String?
        ): String {
            val isSolid = nexusPatternName == null || nexusPatternName.equals("solid", ignoreCase = true)
            val isWhite = nexusColorName != null &&
                nexusColorName.lowercase() in listOf("white", "off-white")

            return when {
                isSolid -> nexusColorName ?: "Unknown"
                isWhite -> nexusPatternName ?: "Unknown"
                else -> listOfNotNull(nexusColorName, nexusPatternName)
                    .joinToString(" ")
                    .ifBlank { "Unknown" }
            }
        }
    }

    // ---------------------------------------------------------------------------
    // GraphQL data fetchers
    // ---------------------------------------------------------------------------

    /**
     * Fetch a product's primary material via GraphQL.
     */
    private fun fetchProduct(productId: String): Product? {
        val query = """
            query {
                findProducts(searchString: "search.id:$productId", page: 1, pageSize: 1) {
                    results {
                        productId
                        productMaterials { name primary }
                    }
                }
            }
        """.trimIndent()

        val data = graphQLService.execute(query)
        val results = data?.at("/data/findProducts/results")
        val first = results?.firstOrNull() ?: return null

        return Product(
            productId = first["productId"]?.asText().orEmpty(),
            productMaterials = first["productMaterials"]?.map { pm ->
                ProductMaterial(
                    name = pm["name"]?.asText().orEmpty(),
                    primary = pm["primary"]?.asBoolean() ?: false
                )
            }.orEmpty()
        )
    }

    /**
     * Fetch related material IDs from a combination.
     */
    private fun fetchCombinationMaterialIds(combinationId: String): List<String> {
        val query = """
            query {
                combination(id: "$combinationId") {
                    id
                    impressionIntentBucketList { relatedMaterialIds }
                }
            }
        """.trimIndent()

        val data = graphQLService.execute(query)
        val buckets = data?.at("/data/combination/impressionIntentBucketList") ?: return emptyList()

        return buckets.flatMap { bucket ->
            bucket["relatedMaterialIds"]?.map { it.asText() }.orEmpty()
        }
    }

    /**
     * Look up materials by IDs, returning type-specific fields.
     */
    private fun fetchMaterials(materialIds: List<String>): List<Material> {
        val orClause = materialIds.joinToString(" OR ")
        val query = """
            query {
                materialsSearch(searchRequest: {query: "id: ($orClause)"}) {
                    content {
                        id
                        description
                        primaryAttachment { id }
                        __typename
                        ... on Artwork { primaryAttachmentId }
                        ... on Color { primaryAttachmentId red green blue }
                    }
                }
            }
        """.trimIndent()

        val data = graphQLService.execute(query)
        val content = data?.at("/data/materialsSearch/content") ?: return emptyList()

        return content.map { node -> parseMaterial(node) }
    }

    private fun parseMaterial(node: JsonNode): Material {
        val attachmentId = node["primaryAttachmentId"]?.asText()
            ?: node["primaryAttachment"]?.get("id")?.asText()

        return Material(
            id = node["id"]?.asText().orEmpty(),
            description = node["description"]?.asText(),
            typeName = node["__typename"]?.asText().orEmpty(),
            primaryAttachmentId = attachmentId,
            red = node["red"]?.asInt(),
            green = node["green"]?.asInt(),
            blue = node["blue"]?.asInt()
        )
    }

    private fun unknownEntry(materialId: String) = IdentifyResultEntry(
        materialId = materialId,
        recommendedName = "Unknown",
        nexusColorId = null,
        nexusColorName = null,
        nexusPatternId = null,
        nexusPatternName = null
    )
}
