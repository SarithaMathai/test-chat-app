package com.testmonochat.tools.service

import com.testmonochat.tools.constant.NexusData
import com.testmonochat.tools.model.*
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Service for Nexus attribute lookups and fuzzy matching.
 *
 * On startup the static [NexusData.NEXUS_ITEMS] list is pre-indexed by type
 * into fast-lookup maps, so that matching operations avoid repeated filtering.
 */
@Service
class NexusAttributeService {

    private val log = LoggerFactory.getLogger(javaClass)

    /** All items from the static data set. */
    val allItems: List<NexusItem> = NexusData.NEXUS_ITEMS

    /** Items indexed by their `type` field for O(1) category access. */
    val itemsByType: Map<String, List<NexusItem>> = allItems.groupBy { it.type }

    @PostConstruct
    fun init() {
        log.info(
            "Nexus data loaded: {} items across {} types",
            allItems.size, itemsByType.size
        )
    }

    // ---------------------------------------------------------------------------
    // Public query methods
    // ---------------------------------------------------------------------------

    fun getGrouped(typeFilter: String?, search: String?): GroupedAttributesResponse {
        var items = allItems

        if (!typeFilter.isNullOrBlank()) {
            val tf = typeFilter.lowercase()
            items = items.filter { it.type.lowercase() == tf }
        }
        if (!search.isNullOrBlank()) {
            val s = search.lowercase()
            items = items.filter { s in it.name.lowercase() }
        }

        val groups = items.groupBy { it.type }.entries
            .sortedBy { it.key }
            .map { (type, attrs) -> AttributeGroup(type, attrs) }

        return GroupedAttributesResponse(grouped = groups, totalTypes = groups.size)
    }

    fun getTypes(): AttributeTypesResponse {
        val types = allItems.map { it.type }.distinct().sorted()
        return AttributeTypesResponse(types = types, total = types.size)
    }

    fun getByType(typeName: String): AttributesByTypeResponse {
        val items = allItems.filter { it.type.equals(typeName, ignoreCase = true) }
        return AttributesByTypeResponse(type = typeName, attributes = items, total = items.size)
    }

    // ---------------------------------------------------------------------------
    // Fuzzy matching
    // ---------------------------------------------------------------------------

    /**
     * Match incoming color/pattern values to the closest Nexus attribute IDs.
     */
    fun matchAttributes(request: MatchAttributesRequest): MatchAttributesResponse {
        val colorFamilyItems = itemsByType["Color Family"].orEmpty()
        val colorNameItems = itemsByType["Color Name"].orEmpty()
        val colorToneItems = itemsByType["Color Tone"].orEmpty()
        val patternItems = itemsByType["Pattern"].orEmpty()

        var familyMatches = emptyList<ScoredNexusItem>()
        var nameMatches = emptyList<ScoredNexusItem>()
        var toneMatches = emptyList<ScoredNexusItem>()
        var patternMatches = emptyList<ScoredNexusItem>()

        // --- Color Family ---
        if (!request.colorFamily.isNullOrBlank()) {
            familyMatches = bestMatches(request.colorFamily, colorFamilyItems, topN = 3)
        }

        // --- Color Name ---
        if (!request.colorName.isNullOrBlank()) {
            nameMatches = bestMatches(request.colorName, colorNameItems, topN = 3)
            toneMatches = deriveToneFromName(request.colorName, colorToneItems)
        }

        // --- Hex fallback (only when no text color was supplied) ---
        if (!request.hexColors.isNullOrEmpty() && request.colorFamily.isNullOrBlank() && request.colorName.isNullOrBlank()) {
            val hexResult = matchColorFromHex(request.hexColors, colorNameItems, colorFamilyItems, topN = 3)
            familyMatches = hexResult.colorFamily
            nameMatches = hexResult.colorName
        }

        // --- Pattern ---
        if (!request.pattern.isNullOrBlank()) {
            patternMatches = bestMatches(request.pattern, patternItems, topN = 3, minConfidence = 0.25)
        }

        log.info(
            "matchNexusAttributes: family={}, name={}, pattern={}, hex={} -> {} family, {} name, {} pattern matches",
            request.colorFamily, request.colorName, request.pattern, request.hexColors,
            familyMatches.size, nameMatches.size, patternMatches.size
        )

        return MatchAttributesResponse(
            colorMatches = ColorMatches(
                colorFamily = familyMatches,
                colorName = nameMatches,
                colorTone = toneMatches
            ),
            patternMatches = patternMatches
        )
    }

    /**
     * Simplified match used by the color-pattern identification orchestrator.
     * Returns at most 1 result per category.
     */
    fun matchSingle(
        colorFamily: String? = null,
        colorName: String? = null,
        pattern: String? = null,
        hexColors: List<String>? = null
    ): SingleMatchResult {
        val colorFamilyItems = itemsByType["Color Family"].orEmpty()
        val colorNameItems = itemsByType["Color Name"].orEmpty()
        val patternItems = itemsByType["Pattern"].orEmpty()

        var familyMatch: ScoredNexusItem? = null
        var nameMatch: ScoredNexusItem? = null
        var patternMatch: ScoredNexusItem? = null

        if (!colorFamily.isNullOrBlank()) {
            familyMatch = bestMatches(colorFamily, colorFamilyItems, topN = 1).firstOrNull()
        }
        if (!colorName.isNullOrBlank()) {
            nameMatch = bestMatches(colorName, colorNameItems, topN = 1).firstOrNull()
        }
        if (!hexColors.isNullOrEmpty() && colorFamily.isNullOrBlank() && colorName.isNullOrBlank()) {
            val hexResult = matchColorFromHex(hexColors, colorNameItems, colorFamilyItems, topN = 1)
            familyMatch = hexResult.colorFamily.firstOrNull()
            nameMatch = hexResult.colorName.firstOrNull()
        }
        if (!pattern.isNullOrBlank()) {
            patternMatch = bestMatches(pattern, patternItems, topN = 1).firstOrNull()
        }

        return SingleMatchResult(familyMatch, nameMatch, patternMatch)
    }

    data class SingleMatchResult(
        val colorFamily: ScoredNexusItem?,
        val colorName: ScoredNexusItem?,
        val pattern: ScoredNexusItem?
    )

    // ---------------------------------------------------------------------------
    // Similarity helpers
    // ---------------------------------------------------------------------------

    /**
     * Return the top-N [NexusItem]s whose name is most similar to [query].
     */
    fun bestMatches(
        query: String,
        candidates: List<NexusItem>,
        topN: Int = 3,
        minConfidence: Double = 0.3
    ): List<ScoredNexusItem> {
        return candidates
            .map { item ->
                val conf = similarity(query, item.name)
                ScoredNexusItem(item.id, item.name, item.type, conf.roundTo4())
            }
            .filter { it.confidence >= minConfidence }
            .sortedByDescending { it.confidence }
            .take(topN)
    }

    /**
     * Match hex colors to Nexus Color Family and Color Name by mapping hex → hue → family guess.
     */
    fun matchColorFromHex(
        hexColors: List<String>,
        colorNameItems: List<NexusItem>,
        colorFamilyItems: List<NexusItem>,
        topN: Int = 3
    ): HexMatchResult {
        val familyMatches = mutableListOf<ScoredNexusItem>()
        val nameMatches = mutableListOf<ScoredNexusItem>()

        for (hex in hexColors) {
            val rgb = hexToRgb(hex) ?: continue
            val familyGuess = guessColorFamilyFromRgb(rgb)

            colorFamilyItems.forEach { attr ->
                val conf = similarity(familyGuess, attr.name)
                if (conf >= 0.3) familyMatches += ScoredNexusItem(attr.id, attr.name, attr.type, conf.roundTo4())
            }
            colorNameItems.forEach { attr ->
                val conf = similarity(familyGuess, attr.name)
                if (conf >= 0.3) nameMatches += ScoredNexusItem(attr.id, attr.name, attr.type, conf.roundTo4())
            }
        }

        return HexMatchResult(
            colorFamily = dedupAndSort(familyMatches, topN),
            colorName = dedupAndSort(nameMatches, topN)
        )
    }

    data class HexMatchResult(
        val colorFamily: List<ScoredNexusItem>,
        val colorName: List<ScoredNexusItem>
    )

    // ---------------------------------------------------------------------------
    // Internal utilities
    // ---------------------------------------------------------------------------

    /** Derive a tone suggestion from lightness keywords in the color name. */
    private fun deriveToneFromName(colorName: String, toneItems: List<NexusItem>): List<ScoredNexusItem> {
        val lower = colorName.lowercase()
        val toneGuess = when {
            lower.containsAny("light", "pale", "pastel", "soft") -> "Light"
            lower.containsAny("dark", "deep", "navy", "midnight") -> "Dark"
            lower.containsAny("medium", "mid", "classic") -> "Medium"
            else -> return emptyList()
        }
        return bestMatches(toneGuess, toneItems, topN = 2)
    }

    companion object {

        /**
         * Case-insensitive similarity ratio (0.0–1.0) using the longest common subsequence approach,
         * equivalent to Python's `SequenceMatcher.ratio()`.
         */
        fun similarity(a: String, b: String): Double {
            val s1 = a.lowercase()
            val s2 = b.lowercase()
            if (s1 == s2) return 1.0
            if (s1.isEmpty() || s2.isEmpty()) return 0.0

            val matchingLength = longestCommonSubsequenceLength(s1, s2)
            return (2.0 * matchingLength) / (s1.length + s2.length)
        }

        /**
         * Length of the longest common subsequence between two strings.
         * Used to compute the SequenceMatcher-style ratio.
         */
        private fun longestCommonSubsequenceLength(a: String, b: String): Int {
            val m = a.length
            val n = b.length
            val dp = Array(m + 1) { IntArray(n + 1) }
            for (i in 1..m) {
                for (j in 1..n) {
                    dp[i][j] = if (a[i - 1] == b[j - 1]) {
                        dp[i - 1][j - 1] + 1
                    } else {
                        maxOf(dp[i - 1][j], dp[i][j - 1])
                    }
                }
            }
            return dp[m][n]
        }

        /** Convert "#RRGGBB" to an RGB triple, or `null` if malformed. */
        fun hexToRgb(hex: String): Triple<Int, Int, Int>? {
            val clean = hex.removePrefix("#")
            if (clean.length != 6) return null
            return try {
                Triple(
                    clean.substring(0, 2).toInt(16),
                    clean.substring(2, 4).toInt(16),
                    clean.substring(4, 6).toInt(16)
                )
            } catch (_: NumberFormatException) {
                null
            }
        }

        /**
         * Map an RGB colour to a rough color-family name via hue/saturation/lightness.
         */
        fun guessColorFamilyFromRgb(rgb: Triple<Int, Int, Int>): String {
            val (red, green, blue) = rgb
            val r = red / 255.0
            val g = green / 255.0
            val b = blue / 255.0

            val max = maxOf(r, g, b)
            val min = minOf(r, g, b)
            val l = (max + min) / 2.0
            val delta = max - min
            val s = if (delta == 0.0) 0.0 else delta / (1.0 - kotlin.math.abs(2 * l - 1))

            val hue = when {
                delta == 0.0 -> 0.0
                max == r -> 60 * (((g - b) / delta) % 6)
                max == g -> 60 * (((b - r) / delta) + 2)
                else -> 60 * (((r - g) / delta) + 4)
            }.let { if (it < 0) it + 360 else it }

            return when {
                s < 0.1 -> if (l < 0.85) "Gray" else "White"
                l < 0.15 -> "Black"
                hue < 15 || hue >= 345 -> "Red"
                hue < 45 -> "Orange"
                hue < 70 -> "Yellow"
                hue < 160 -> "Green"
                hue < 250 -> "Blue"
                hue < 290 -> "Purple"
                else -> "Pink"
            }
        }

        /** Deduplicate by ID, keep highest confidence, return top N. */
        private fun dedupAndSort(items: List<ScoredNexusItem>, n: Int): List<ScoredNexusItem> {
            return items
                .sortedByDescending { it.confidence }
                .distinctBy { it.id }
                .take(n)
        }

        private fun Double.roundTo4(): Double = "%.4f".format(this).toDouble()

        private fun String.containsAny(vararg words: String): Boolean =
            words.any { this.contains(it) }
    }
}
