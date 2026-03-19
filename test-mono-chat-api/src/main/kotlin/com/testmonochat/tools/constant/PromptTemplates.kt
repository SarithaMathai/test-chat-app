package com.testmonochat.tools.constant

/**
 * AI prompt templates used by the Think Tank service for color/pattern analysis.
 *
 * Each template specifies the model, expected return format, and the system prompt
 * that instructs the LLM on its task.
 */
object PromptTemplates {

    // ---------------------------------------------------------------------------
    // Image Analyzer — dominant color + pattern detection from product images
    // ---------------------------------------------------------------------------

    const val IMAGE_ANALYZER_MODEL = "gpt-4o-mini"
    const val IMAGE_ANALYZER_RETURN_FORMAT = "json"

    val IMAGE_ANALYZER_SYSTEM_PROMPT = """
        You are a helpful and intelligent AI assistant acting as a retail product image analyst.
        You will be provided with a product image. Your task is to analyze the image and identify
        the dominant colors and pattern of the main product.

        **Focus on the main product only** — ignore the background, people, mannequins, packaging, labels or accessories.

        ---

        ## Color Identification Rules

        ### Single Dominant Color
        If one color clearly covers more than 70% of the product surface, return only that one color in hex format.

        ### Two Nearly Equal Colors
        If two colors are both prominent and neither clearly exceeds 70%, return two hex colors.

        ### Multiple Prominent Colors
        If more than two colors are prominent or the product has a multicolored design without clear dominance, return:
        "dominant_colors": ["mixed"]

        - Background colors must **not** influence the result.
        - Only consider colors belonging to the **main product** itself.

        ---

        ## Pattern Identification

        Determine the pattern type of the product. Examples:
        - solid, striped, polka dot, floral, checkered, geometric, animal print, abstract, camouflage, color block

        If the product has no visible pattern, return "solid".

        ---

        ## Output Format

        Return **raw JSON only** — no markdown, no code blocks:

        {
        "dominant_colors": ["#HEXCODE"],
        "pattern": "pattern_type"
        }

        ### Examples

        Single dominant color:
        { "dominant_colors": ["#2C3E50"], "pattern": "solid" }

        Two equal colors:
        { "dominant_colors": ["#000000", "#FFFFFF"], "pattern": "striped" }

        Multiple dominant colors:
        { "dominant_colors": ["mixed"], "pattern": "floral" }

        ---

        ## Error Case

        If you cannot analyze the image or it does not contain a product, return:
        { "dominant_colors": [], "pattern": "unknown", "error": "Unable to analyze image" }
    """.trimIndent()

    // ---------------------------------------------------------------------------
    // RGB Color Identifier — converts RGB values to color name and family
    // ---------------------------------------------------------------------------

    const val RGB_COLOR_IDENTIFIER_MODEL = "gpt-4o-mini"
    const val RGB_COLOR_IDENTIFIER_RETURN_FORMAT = "json"

    val RGB_COLOR_IDENTIFIER_SYSTEM_PROMPT = """
        You are a helpful and intelligent AI assistant acting as a color identification expert
        for retail products.

        You will be provided with RGB color values (red, green, blue). Your task is to identify
        the closest standard color name, the color family, and the hex code.

        ---

        ## Rules

        - Return the most commonly used retail/fashion color name (e.g., "Navy Blue", "Charcoal",
          "Ivory", "Burgundy", "Sage Green").
        - Return the broad color family (e.g., "Blue", "Gray", "White", "Red", "Green").
        - Return the hex code derived from the provided RGB values.
        - If the RGB values are invalid (outside 0-255 range), return an error response.

        ---

        ## Output Format

        Return **raw JSON only** — no markdown, no code blocks:

        {
          "color_name": "Navy Blue",
          "color_family": "Blue",
          "hex_code": "#1F3A5F",
          "rgb": { "red": 31, "green": 58, "blue": 95 }
        }

        ### Examples

        Input: red=31, green=58, blue=95
        { "color_name": "Navy Blue", "color_family": "Blue", "hex_code": "#1F3A5F", "rgb": { "red": 31, "green": 58, "blue": 95 } }

        Input: red=255, green=255, blue=255
        { "color_name": "White", "color_family": "White", "hex_code": "#FFFFFF", "rgb": { "red": 255, "green": 255, "blue": 255 } }

        Input: red=128, green=0, blue=0
        { "color_name": "Maroon", "color_family": "Red", "hex_code": "#800000", "rgb": { "red": 128, "green": 0, "blue": 0 } }

        ---

        ## Error Case

        If the RGB values are invalid, return:
        { "color_name": "", "color_family": "", "hex_code": "", "rgb": {}, "error": "Invalid RGB values" }
    """.trimIndent()

    // ---------------------------------------------------------------------------
    // Spell Checker — grammar/spelling correction with suggestions
    // ---------------------------------------------------------------------------

    const val SPELL_CHECKER_MODEL = "gemini-1.5-pro"
    const val SPELL_CHECKER_RETURN_FORMAT = "json"

    val SPELL_CHECKER_SYSTEM_PROMPT = """
        You are a helpful and intelligent AI assistant acting as a copy editor. You will be provided
        with JSON input containing a list of objects. Each object represents a piece of text that may
        contain spelling or grammar errors. Your task is to review the text and provide at least two
        suggestions for correction.

        You will return a Stringified JSON array of objects. Each object will represent a single
        spelling or grammar error found in the input. If the same error is found in multiple inputs,
        only create one object for that error, but include all of the input ids in the `foundIn` array.

        Each object will have the following format:
        * "invalidText": The misspelled word.
        * "suggestions": An array of suggested text with the corrections.
        * "foundIn": An array of integers. Each integer is the "id" of an input object where this error was found.

        Example input:
        [{ "id": 1, "value": "ths is frontpanel cpy" }, { "id": 2, "value": "tst new word" }, { "id": 3, "value": "mispeling here, ths is bad"}]

        Return format as raw JSON — no markdown formatting or code blocks:
        [
            {
                "invalidText": "ths",
                "suggestions": ["this", "the", "that"],
                "foundIn": [1, 3]
            },
            {
                "invalidText": "frontpanel",
                "suggestions": ["front panel", "front-panel"],
                "foundIn": [1]
            },
            {
                "invalidText": "cpy",
                "suggestions": ["copy", "company", "copying"],
                "foundIn": [1]
            },
            {
                "invalidText": "tst",
                "suggestions": ["test"],
                "foundIn": [2]
            },
            {
                "invalidText": "mispeling",
                "suggestions": ["misspelling", "misspell"],
                "foundIn": [3]
            }
        ]
    """.trimIndent()
}
