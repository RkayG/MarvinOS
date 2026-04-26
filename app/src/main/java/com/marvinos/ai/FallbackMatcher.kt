package com.marvinos.ai

import com.marvinos.model.ActionType
import com.marvinos.model.ParsedIntent
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline fallback intent matcher using simple regex rules.
 *
 * Runs in parallel with the Gemini API call. Its result is used when:
 *  - The API request times out (> 8s).
 *  - The device has no network connectivity.
 *  - The API returns an error response.
 *
 * This is a pure Kotlin module — zero Android dependencies.
 * Covers all 10 MVP commands with common phrasings.
 */
@Singleton
class FallbackMatcher @Inject constructor() {

    /**
     * Attempts to match [input] against known command patterns.
     *
     * @return A [ParsedIntent] with [confidence] ≤ 0.75 (never as confident as Gemini),
     *         or [ActionType.UNKNOWN] if no pattern matches.
     */
    fun match(input: String): ParsedIntent {
        val text = input.trim().lowercase()

        for (rule in RULES) {
            val match = rule.pattern.find(text) ?: continue
            return rule.buildIntent(match, text)
        }

        return ParsedIntent(
            action = ActionType.UNKNOWN,
            confidence = 0f,
            clarificationQuestion = "I'm in offline mode and couldn't understand that. " +
                    "Try: \"turn off WiFi\", \"flashlight on\", \"check my specs\".",
            rawInput = input
        )
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Rule DSL
    // ──────────────────────────────────────────────────────────────────────────

    private data class Rule(
        val pattern: Regex,
        val buildIntent: (MatchResult, String) -> ParsedIntent
    )

    private val RULES: List<Rule> = listOf(

        // ── WiFi ──────────────────────────────────────────────────────────────
        Rule(Regex("""(turn|switch|toggle|enable|disable|put)?\s*(on|off)?\s*wi[-\s]?fi|(wi[-\s]?fi)\s*(on|off|enable|disable|connect|disconnect)""")) { _, text ->
            val on = isOn(text)
            ParsedIntent(ActionType.TOGGLE_WIFI, target = "wifi", value = on, confidence = 0.75f, rawInput = text)
        },

        // ── Bluetooth ─────────────────────────────────────────────────────────
        Rule(Regex("""(turn|switch|toggle|enable|disable)?\s*(on|off)?\s*bluetooth|(bluetooth)\s*(on|off|enable|disable|connect|disconnect)""")) { _, text ->
            val on = isOn(text)
            ParsedIntent(ActionType.TOGGLE_BLUETOOTH, target = "bluetooth", value = on, confidence = 0.75f, rawInput = text)
        },

        // ── Flashlight / Torch ────────────────────────────────────────────────
        Rule(Regex("""(turn|switch|toggle|enable|disable)?\s*(on|off)?\s*(flashlight|torch|light)|(flashlight|torch)\s*(on|off)""")) { _, text ->
            val on = isOn(text)
            ParsedIntent(ActionType.TOGGLE_FLASHLIGHT, target = "flashlight", value = on, confidence = 0.75f, rawInput = text)
        },

        // ── Brightness ────────────────────────────────────────────────────────
        Rule(Regex("""(set|change|increase|decrease|turn up|turn down|max|min|lower|raise|adjust)?\s*(screen|display)?\s*brightness\s*(to|at)?\s*(\d+%?|max|full|low|min|half|medium|high)?""")) { match, text ->
            val level = match.groupValues.lastOrNull { it.isNotBlank() }?.trim() ?: "medium"
            ParsedIntent(ActionType.SET_BRIGHTNESS, target = level, value = null, confidence = 0.72f, rawInput = text)
        },

        // ── Dark Mode ─────────────────────────────────────────────────────────
        Rule(Regex("""(turn|switch|enable|disable|toggle)?\s*(on|off)?\s*(dark\s?mode|night\s?mode|dark\s?theme)|(dark\s?mode|night\s?mode)\s*(on|off|enable|disable)""")) { _, text ->
            val on = isOn(text)
            ParsedIntent(ActionType.TOGGLE_DARK_MODE, target = "dark_mode", value = on, confidence = 0.75f, rawInput = text)
        },

        // ── Open Settings ─────────────────────────────────────────────────────
        Rule(Regex("""open\s+(settings?|wifi\s+settings?|bluetooth\s+settings?|display\s+settings?|battery\s+settings?|storage\s+settings?|accessibility\s+settings?|app\s+settings?)""")) { match, text ->
            val target = match.groupValues[1].trim().replace("settings", "").trim().ifEmpty { "general" }
            ParsedIntent(ActionType.OPEN_SETTINGS, target = target, value = null, confidence = 0.75f, rawInput = text)
        },

        // ── Optimize ──────────────────────────────────────────────────────────
        Rule(Regex("""(optimize|optimise|boost|clean\s?up|speed\s?up|improve)\s*(my\s+)?(phone|device|performance|battery|ram|memory)?""")) { _, text ->
            ParsedIntent(ActionType.OPTIMIZE, target = "device", value = null, confidence = 0.70f, rawInput = text)
        },

        // ── Free Space / Storage ──────────────────────────────────────────────
        Rule(Regex("""(free|clear|clean|reclaim|check)\s*(up\s+)?(storage|space|disk|memory|cache)|(storage|space)\s*(full|low|check)""")) { _, text ->
            ParsedIntent(ActionType.FREE_SPACE, target = "storage", value = null, confidence = 0.72f, rawInput = text)
        },

        // ── Device Info ───────────────────────────────────────────────────────
        Rule(Regex("""(what('?s| is| are)?|show|tell me|check|display)\s+(my\s+)?(specs?|specifications?|phone\s+info|device\s+info|hardware|ram|storage|cpu|processor|chipset|screen)""")) { _, text ->
            ParsedIntent(ActionType.DEVICE_INFO, target = "hardware", value = null, confidence = 0.72f, rawInput = text)
        },

        // ── Game Compatibility ────────────────────────────────────────────────
        Rule(Regex("""(can|will|does?|could)\s+(my\s+)?(phone|device)?\s*(run|play|handle|support)\s+(.+)""")) { match, text ->
            val game = match.groupValues[5].trim()
            ParsedIntent(ActionType.GAME_COMPAT, target = game, value = null, confidence = 0.68f, rawInput = text)
        }
    )

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Determines whether the text implies an ON/enable or OFF/disable intent.
     * Defaults to ON when ambiguous (e.g. "toggle flashlight" with no direction).
     */
    private fun isOn(text: String): Boolean {
        val offWords = listOf("off", "disable", "disconnect", "deactivate", "turn off", "switch off")
        return offWords.none { text.contains(it) }
    }
}
