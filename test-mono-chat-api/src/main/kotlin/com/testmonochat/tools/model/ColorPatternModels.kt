package com.testmonochat.tools.model

// ---------------------------------------------------------------------------
// Color & Pattern Identification (orchestration endpoint)
// ---------------------------------------------------------------------------

data class IdentifyRequest(
    val inputType: String,
    val inputId: String
)

data class IdentifyResultEntry(
    val materialId: String,
    val recommendedName: String,
    val nexusColorId: String?,
    val nexusColorName: String?,
    val nexusColorNameDetail: ScoredNexusItem? = null,
    val nexusPatternId: String?,
    val nexusPatternName: String?
)

// ---------------------------------------------------------------------------
// Internal GraphQL response shapes
// ---------------------------------------------------------------------------

data class ProductMaterial(
    val name: String,
    val primary: Boolean
)

data class Product(
    val productId: String,
    val productMaterials: List<ProductMaterial> = emptyList()
)

data class Material(
    val id: String,
    val description: String? = null,
    val typeName: String = "",           // __typename: "Color" | "Artwork"
    val primaryAttachmentId: String? = null,
    val red: Int? = null,
    val green: Int? = null,
    val blue: Int? = null
)

data class CombinationBucket(
    val relatedMaterialIds: List<String> = emptyList()
)

data class Combination(
    val id: String,
    val impressionIntentBucketList: List<CombinationBucket> = emptyList()
)
