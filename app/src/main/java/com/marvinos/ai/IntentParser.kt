package com.marvinos.ai

import com.marvinos.model.ActionType
import com.marvinos.model.ParsedIntent
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.float
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Converts the raw JSON string returned by [GeminiApiClient] into a typed [ParsedIntent].
 *
 * This class is a pure Kotlin module — zero Android dependencies — making it
 * fully unit-testable in a standard JVM test runner.
 *
 * Failure strategy: any parse error (malformed JSON, missing fields, unknown action)
 * returns a [ParsedIntent] with [ActionType.UNKNOWN] and confidence 0.0 rather
 * than throwing, so the Action Engine always has something to work with.
 */
@Singleton
class IntentParser @Inject constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parses [rawJson] into a [ParsedIntent].
     *
     * @param rawJson   JSON string from Gemini (may contain leading/trailing whitespace).
     * @param rawInput  Original user text, preserved in the result for logging.
     * @return          A valid [ParsedIntent]; never throws.
     */
    fun parse(rawJson: String, rawInput: String = ""): ParsedIntent {
        val cleaned = rawJson.trim()
            .removePrefix("```json").removePrefix("```")   // strip accidental fences
            .removeSuffix("```").trim()

        return try {
            val obj = json.parseToJsonElement(cleaned) as? JsonObject
                ?: return fallbackIntent(rawInput, reason = "Response was not a JSON object")

            val actionStr = obj["action"]?.jsonPrimitive?.contentOrNull ?: "UNKNOWN"
            val action = try {
                ActionType.valueOf(actionStr.uppercase())
            } catch (e: IllegalArgumentException) {
                ActionType.UNKNOWN
            }

            val target = obj["target"]?.jsonPrimitive?.contentOrNull
            val value = obj["value"]?.jsonPrimitive?.booleanOrNull
            val requiresGuidance = obj["requiresGuidance"]?.jsonPrimitive?.booleanOrNull ?: false
            val confidence = obj["confidence"]?.jsonPrimitive?.floatOrNull ?: 0f
            val clarification = obj["clarificationQuestion"]?.jsonPrimitive?.contentOrNull

            ParsedIntent(
                action = action,
                target = target,
                value = value,
                requiresGuidance = requiresGuidance,
                confidence = confidence,
                clarificationQuestion = clarification,
                rawInput = rawInput
            )
        } catch (e: Exception) {
            fallbackIntent(rawInput, reason = "JSON parse error: ${e.message}")
        }
    }

    private fun fallbackIntent(rawInput: String, reason: String): ParsedIntent {
        return ParsedIntent(
            action = ActionType.UNKNOWN,
            confidence = 0f,
            clarificationQuestion = "Sorry, I couldn't understand that. Could you try rephrasing?",
            rawInput = rawInput
        )
    }
}
