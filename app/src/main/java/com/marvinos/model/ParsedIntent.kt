package com.marvinos.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The set of actions MarvinOS can perform.
 * Gemini Flash returns exactly one of these strings in the `action` field.
 */
enum class ActionType {
    TOGGLE_WIFI,
    TOGGLE_BLUETOOTH,
    TOGGLE_FLASHLIGHT,
    SET_BRIGHTNESS,
    TOGGLE_DARK_MODE,
    OPEN_SETTINGS,
    OPTIMIZE,
    FREE_SPACE,
    DEVICE_INFO,
    GAME_COMPAT,
    UNKNOWN
}

/**
 * Structured intent produced by the AI / NLU layer.
 *
 * Every user message flows through [com.marvinos.ai.IntentParser] and
 * resolves into exactly one [ParsedIntent], which then drives the entire
 * Action Engine pipeline.
 *
 * @property action       The resolved action (see [ActionType]).
 * @property target       Optional sub-target, e.g. "storage", "battery", "wifi".
 * @property value        true = ON/enable · false = OFF/disable · null = not applicable.
 * @property requiresGuidance  When true the Action Engine returns step-by-step
 *                             instructions instead of executing directly.
 * @property confidence   0.0–1.0. Below 0.6 the app asks a clarifying question.
 * @property clarificationQuestion  Populated when [confidence] < 0.6.
 * @property rawInput     Original user text preserved for logging / fallback.
 */
@Serializable
data class ParsedIntent(
    @SerialName("action")
    val action: ActionType = ActionType.UNKNOWN,

    @SerialName("target")
    val target: String? = null,

    @SerialName("value")
    val value: Boolean? = null,

    @SerialName("requiresGuidance")
    val requiresGuidance: Boolean = false,

    @SerialName("confidence")
    val confidence: Float = 0f,

    @SerialName("clarificationQuestion")
    val clarificationQuestion: String? = null,

    val rawInput: String = ""
) {
    /** True when the intent confidence is high enough to proceed without clarification. */
    val isActionable: Boolean get() = confidence >= 0.6f && action != ActionType.UNKNOWN

    /** True for read-only queries that skip the confirmation gate. */
    val isReadOnly: Boolean get() = action in setOf(
        ActionType.DEVICE_INFO,
        ActionType.GAME_COMPAT,
        ActionType.FREE_SPACE
    )
}
