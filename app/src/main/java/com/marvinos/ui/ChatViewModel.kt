package com.marvinos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marvinos.actions.ActionExecutor
import com.marvinos.ai.DeviceContextBuilder
import com.marvinos.ai.FallbackMatcher
import com.marvinos.ai.GeminiApiClient
import com.marvinos.ai.IntentParser
import com.marvinos.intelligence.DeviceProfiler
import com.marvinos.intelligence.GameCompatChecker
import com.marvinos.model.ActionResult
import com.marvinos.model.ActionType
import com.marvinos.model.Message
import com.marvinos.model.ParsedIntent
import com.marvinos.permissions.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val geminiApiClient: GeminiApiClient,
    private val intentParser: IntentParser,
    private val fallbackMatcher: FallbackMatcher,
    private val actionExecutor: ActionExecutor,
    private val permissionManager: PermissionManager,
    private val deviceContextBuilder: DeviceContextBuilder,
    private val deviceProfiler: DeviceProfiler,
    private val gameCompatChecker: GameCompatChecker
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(
        listOf(Message.assistant("Hi! I'm MarvinOS. What would you like to do?"))
    )
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _pendingIntent = MutableStateFlow<ParsedIntent?>(null)
    val pendingIntent: StateFlow<ParsedIntent?> = _pendingIntent.asStateFlow()

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        addMessage(Message.user(text))
        
        val loadingId = java.util.UUID.randomUUID().toString()
        addMessage(Message(id = loadingId, role = com.marvinos.model.MessageRole.ASSISTANT, content = "", isLoading = true))

        viewModelScope.launch {
            try {
                // Sprint 6: Live telemetry injection
                val promptText = if (text.lowercase().contains("specs") || text.lowercase().contains("play")) {
                    val profile = deviceProfiler.getCurrentProfile()
                    if (text.lowercase().contains("play") || text.lowercase().contains("run")) {
                        deviceContextBuilder.buildGameCompatPrompt(profile, text, gameCompatChecker.getGameDatabaseJson())
                    } else {
                        deviceContextBuilder.buildDeviceInfoPrompt(profile, text)
                    }
                } else {
                    text
                }

                val jsonResponse = try {
                    geminiApiClient.sendMessage(promptText)
                } catch (e: Exception) {
                    null
                }

                val intent = if (jsonResponse != null) {
                    intentParser.parse(jsonResponse, text)
                } else {
                    fallbackMatcher.match(text)
                }

                removeMessage(loadingId)
                
                // For intelligence queries, Gemini generates the prose answer directly via JSON or we can render it.
                // But architecture specifies the response should be plain english from Gemini.
                // Our prompt instructs it to return JSON, so we handle the prose either via another prompt
                // or we rely on the ActionEngine to format it. Let's send it to the Action Engine.
                
                processIntent(intent)

            } catch (e: Exception) {
                removeMessage(loadingId)
                addMessage(Message.assistant("An error occurred: ${e.message}"))
            }
        }
    }

    private suspend fun processIntent(intent: ParsedIntent) {
        if (!intent.isActionable && intent.action == ActionType.UNKNOWN) {
            addMessage(Message.assistant(intent.clarificationQuestion ?: "I didn't quite get that."))
            return
        }

        val requiredPermission = permissionManager.getMissingPermissionFor(intent.action)
        if (requiredPermission != null) {
            val rationale = permissionManager.getRationaleFor(requiredPermission)
            val settingsAction = com.marvinos.permissions.SpecialPermissionHelper().getSettingsActionFor(requiredPermission)
            addMessage(Message.assistant(
                "I need permission to do that.",
                ActionResult.RequiresPermission(requiredPermission, settingsAction, rationale)
            ))
            _pendingIntent.value = intent
            return
        }

        if (ActionExecutor.requiresConfirmation(intent)) {
            _pendingIntent.value = intent
            return
        }

        executeIntent(intent)
    }

    fun confirmPendingIntent() {
        val intent = _pendingIntent.value ?: return
        _pendingIntent.value = null
        viewModelScope.launch {
            executeIntent(intent)
        }
    }

    fun cancelPendingIntent() {
        _pendingIntent.value = null
        addMessage(Message.system("Action cancelled."))
    }

    private suspend fun executeIntent(intent: ParsedIntent) {
        val result = actionExecutor.execute(intent)
        val responseText = when (result) {
            is ActionResult.Success -> result.message
            is ActionResult.NeedsGuidance -> "Here's how to do that:"
            is ActionResult.Failed -> result.reason
            is ActionResult.RequiresPermission -> "Permission is required."
            is ActionResult.Cancelled -> "Cancelled."
        }
        
        addMessage(Message.assistant(responseText, result))
    }

    private fun addMessage(message: Message) {
        _messages.value = _messages.value + message
    }

    private fun removeMessage(id: String) {
        _messages.value = _messages.value.filter { it.id != id }
    }
}
