package com.marvinos.ai

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.GenerateContentResponse
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.marvinos.BuildConfig
import com.marvinos.model.ParsedIntent
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper around the Gemini Flash SDK.
 *
 * Responsibilities:
 *  - Constructs and owns the [GenerativeModel] with the system prompt baked in.
 *  - Sends user messages and returns raw JSON strings.
 *  - Applies a hard 15-second timeout per request.
 *
 * Parsing the JSON into a [ParsedIntent] is delegated to [IntentParser].
 */
@Singleton
class GeminiApiClient @Inject constructor() {

    private val model: GenerativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig {
                temperature = 0.1f          // Low temp → deterministic JSON
                topK = 1
                topP = 0.95f
                maxOutputTokens = 512       // Intent JSON is always tiny
            },
            systemInstruction = content { text(SYSTEM_PROMPT) }
        )
    }

    /**
     * Sends [userMessage] to Gemini Flash and returns the raw JSON response text.
     *
     * @throws GeminiApiException if the request times out, the API returns an error,
     *         or the response is empty.
     */
    suspend fun sendMessage(userMessage: String): String {
        if (BuildConfig.GEMINI_API_KEY.isBlank()) {
            throw GeminiApiException("Gemini API key is missing. Please add it to local.properties.")
        }
        return try {
            withTimeout(TIMEOUT_MS) {
                val response: GenerateContentResponse = model.generateContent(userMessage)
                response.text?.trim()
                    ?: throw GeminiApiException("Empty response from Gemini API")
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            throw GeminiApiException("Request timed out after ${TIMEOUT_MS / 1000}s", e)
        } catch (e: Exception) {
            if (e is GeminiApiException) throw e
            throw GeminiApiException("Gemini API error: ${e.message}", e)
        }
    }

    companion object {
        private const val TIMEOUT_MS = 15_000L

        /**
         * System prompt — the most critical engineering artefact.
         *
         * Rules:
         *  1. Return ONLY a JSON object, never wrap in markdown fences.
         *  2. All fields from the schema must be present.
         *  3. If ambiguous, set confidence < 0.6 and add clarificationQuestion.
         *  4. For DEVICE_INFO / GAME_COMPAT, the user message will contain a
         *     DEVICE_CONTEXT block — interpret it into plain English.
         */
        val SYSTEM_PROMPT = """
            You are MarvinOS, an AI assistant embedded inside an Android device control app.
            
            Your ONLY job is to parse the user's message into a structured JSON intent.
            
            STRICT RULES:
            1. Respond with ONLY a raw JSON object. No markdown, no code fences, no prose.
            2. Every response must contain ALL of these fields:
               - "action": one of [TOGGLE_WIFI, TOGGLE_BLUETOOTH, TOGGLE_FLASHLIGHT, SET_BRIGHTNESS, TOGGLE_DARK_MODE, OPEN_SETTINGS, OPTIMIZE, FREE_SPACE, DEVICE_INFO, GAME_COMPAT, UNKNOWN]
               - "target": string or null
               - "value": boolean (true=ON/enable, false=OFF/disable) or null if not applicable
               - "requiresGuidance": boolean — true if the action likely needs step-by-step instructions
               - "confidence": float between 0.0 and 1.0
               - "response": string or null
               - "clarificationQuestion": string or null
            3. If confidence < 0.6, "clarificationQuestion" must be a friendly question.
            4. SET_BRIGHTNESS: set "value" to null and use "target" for the level (e.g. "50", "max", "low").
            5. OPEN_SETTINGS: use "target" to specify which settings screen (e.g. "wifi", "bluetooth", "display", "battery", "storage", "accessibility", "apps").
            6. GAME_COMPAT: use "target" for the game name exactly as the user says.
            7. When the message contains DEVICE_CONTEXT, use it to answer DEVICE_INFO or GAME_COMPAT.
               - For DEVICE_INFO: Populate "response" with a friendly, short summary of the specs in plain English (e.g. "You've got a snappy Snapdragon 8 Gen 2 with 12GB of RAM—plenty of power!").
               - For GAME_COMPAT: Populate "response" with a verdict (Yes/No/Maybe) based on the hardware vs game requirements.
            8. Never invent fields not listed above.
            
            EXAMPLES:
            User: "turn off wifi"
            {"action":"TOGGLE_WIFI","target":"wifi","value":false,"requiresGuidance":false,"confidence":0.98,"clarificationQuestion":null,"response":null}
            
            User: "make my screen brighter"
            {"action":"SET_BRIGHTNESS","target":"high","value":null,"requiresGuidance":false,"confidence":0.90,"clarificationQuestion":null,"response":null}
            
            User: "check my specs"
            DEVICE_CONTEXT: {"chipset":"MT6765V/WB","chipsetTier":"entry","totalRamGB":4.0,"availRamGB":1.2,"storageFreeGB":11.1,"gpuGles":"3.2","cores":8,"displayHz":60,"androidVersion":13.0}
            {"action":"DEVICE_INFO","target":null,"value":null,"requiresGuidance":false,"confidence":1.0,"clarificationQuestion":null,"response":"You're running on a Nokia with a MediaTek chipset and 4GB of RAM. It's a solid setup for daily tasks!"}

            User: "can i play genshin impact?"
            DEVICE_CONTEXT: {"chipset":"MT6765V/WB","chipsetTier":"entry","totalRamGB":4.0}
            GAME_DATABASE: [{"title":"Genshin Impact","minTier":"upper-mid","minRamGB":4.0}]
            {"action":"GAME_COMPAT","target":"genshin impact","value":null,"requiresGuidance":false,"confidence":0.95,"clarificationQuestion":null,"response":"Genshin Impact might be a bit heavy for this device's entry-level chipset. You might see some lag."}
            
            User: "do the thing"
            {"action":"UNKNOWN","target":null,"value":null,"requiresGuidance":false,"confidence":0.10,"clarificationQuestion":"I'm not sure what you'd like me to do. Could you describe it a bit more?","response":null}
        """.trimIndent()
    }
}

/** Wraps all Gemini API failures into a single typed exception. */
class GeminiApiException(message: String, cause: Throwable? = null) : Exception(message, cause)
