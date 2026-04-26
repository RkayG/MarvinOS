package com.marvinos.model

/**
 * The result produced by [com.marvinos.actions.ActionExecutor] after
 * processing a [ParsedIntent].
 *
 * The ViewModel pattern-matches on these branches to drive UI state:
 *  - [Success]            → show a green confirmation message in chat
 *  - [NeedsGuidance]      → render a numbered GuidanceCard in the chat
 *  - [RequiresPermission] → trigger the runtime permission flow, then retry
 *  - [Failed]             → show an error message with optional retry
 *  - [Cancelled]          → show a subtle "Action cancelled" message
 */
sealed class ActionResult {

    /**
     * Action completed successfully.
     * @param message Human-readable confirmation shown in the chat bubble.
     */
    data class Success(val message: String) : ActionResult()

    /**
     * Action cannot be performed directly; step-by-step instructions are provided.
     * @param steps Ordered list of human-readable instructions rendered as a card.
     */
    data class NeedsGuidance(val steps: List<String>) : ActionResult()

    /**
     * A required permission has not been granted.
     * @param permissionName  The Android permission string (e.g. "android.permission.CAMERA").
     * @param settingsAction  The Settings intent action to open if the user chose "never ask again".
     * @param rationale       Plain-English explanation shown to the user before the request.
     */
    data class RequiresPermission(
        val permissionName: String,
        val settingsAction: String,
        val rationale: String
    ) : ActionResult()

    /**
     * Action failed with a specific reason.
     * @param reason Human-readable error message.
     * @param isRetryable Whether the user should be offered a retry button.
     */
    data class Failed(
        val reason: String,
        val isRetryable: Boolean = false
    ) : ActionResult()

    /**
     * The user tapped "Cancel" on the confirmation gate.
     * No device state was modified.
     */
    data object Cancelled : ActionResult()
}
