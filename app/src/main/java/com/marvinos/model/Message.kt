package com.marvinos.model

import java.time.Instant

/** Identifies who authored a chat message. */
enum class MessageRole { USER, ASSISTANT, SYSTEM }

/**
 * A single chat message rendered in [com.marvinos.ui.ChatScreen].
 *
 * @property id        Unique identifier (used as Compose key for stable recomposition).
 * @property role      Who sent this message.
 * @property content   Display text shown in the bubble.
 * @property timestamp When the message was created.
 * @property isLoading True while awaiting a response (shows typing indicator).
 * @property result    The resolved [ActionResult], if any, attached to an assistant message.
 *                     The UI uses this to decide whether to render a [GuidanceCard], etc.
 */
data class Message(
    val id: String = java.util.UUID.randomUUID().toString(),
    val role: MessageRole,
    val content: String,
    val timestamp: Instant = Instant.now(),
    val isLoading: Boolean = false,
    val result: ActionResult? = null
) {
    companion object {
        fun user(text: String) = Message(role = MessageRole.USER, content = text)

        fun loading() = Message(
            role = MessageRole.ASSISTANT,
            content = "",
            isLoading = true
        )

        fun assistant(text: String, result: ActionResult? = null) = Message(
            role = MessageRole.ASSISTANT,
            content = text,
            result = result
        )

        fun system(text: String) = Message(role = MessageRole.SYSTEM, content = text)
    }
}
