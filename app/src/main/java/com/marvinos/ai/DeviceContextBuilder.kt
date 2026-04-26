package com.marvinos.ai

import com.marvinos.model.DeviceProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Assembles the hardware telemetry block that is prepended to user messages
 * for [ActionType.DEVICE_INFO] and [ActionType.GAME_COMPAT] queries.
 *
 * The Gemini system prompt instructs the model to interpret the raw values
 * into plain English when it sees a DEVICE_CONTEXT block.
 *
 * Example output:
 * ```
 * DEVICE_CONTEXT: {"chipset":"Snapdragon 778G","chipsetTier":"upper-mid",...}
 * USER_QUERY: Can my phone run Genshin Impact?
 * ```
 */
@Singleton
class DeviceContextBuilder @Inject constructor() {

    /**
     * Builds the full message string to send to Gemini for intelligence queries.
     *
     * @param profile   Current device hardware snapshot.
     * @param userQuery The original user question.
     * @param gameDb    Optional game requirements JSON string for GAME_COMPAT queries.
     */
    fun build(
        profile: DeviceProfile,
        userQuery: String,
        gameDb: String? = null
    ): String = buildString {
        append("DEVICE_CONTEXT: ")
        append(profile.toPromptJson())
        append("\n")
        if (gameDb != null) {
            append("GAME_DATABASE: ")
            append(gameDb)
            append("\n")
        }
        append("USER_QUERY: ")
        append(userQuery)
    }

    /**
     * Builds a device info summary prompt — no game DB needed.
     */
    fun buildDeviceInfoPrompt(profile: DeviceProfile, userQuery: String): String =
        build(profile, userQuery, gameDb = null)

    /**
     * Builds a game compatibility prompt with the game DB attached.
     */
    fun buildGameCompatPrompt(
        profile: DeviceProfile,
        userQuery: String,
        gameDb: String
    ): String = build(profile, userQuery, gameDb)
}
