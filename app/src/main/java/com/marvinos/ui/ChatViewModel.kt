package com.marvinos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marvinos.actions.ActionExecutor
import com.marvinos.ai.DeviceContextBuilder
import com.marvinos.ai.FallbackMatcher
import com.marvinos.ai.GeminiApiClient
import com.marvinos.ai.IntentParser
import com.marvinos.model.ActionResult
import com.marvinos.model.ActionType
import com.marvinos.model.DeviceProfile
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
    private val deviceContextBuilder: DeviceContextBuilder
) : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(
        listOf(Message.assistant("Hi! I'm MarvinOS. What would you like to do?"))
    )
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private val _pendingIntent = MutableStateFlow<ParsedIntent?>(null)
    val pendingIntent: StateFlow<ParsedIntent?> = _pendingIntent.asStateFlow()

    // Simplified profile for MVP. In Sprint 6 this comes from DeviceProfiler.
    private val mockDeviceProfile = DeviceProfile(
        chipsetName = "Snapdragon 778G",
        chipsetTier = "upper-mid",
        totalRamBytes = 6L * 1024 * 1024 * 1024,
        availRamBytes = 2L * 1024 * 1024 * 1024,
        freeStorageBytes = 42L * 1024 * 1024 * 1024,
        totalStorageBytes = 128L * 1024 * 1024 * 1024,
        gpuGlesVersion = "3.2",
        cpuCores = 8,
        displayRefreshHz = 120f,
        androidVersion = 14f
    )

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        // 1. Add user message
        addMessage(Message.user(text))
        
        // 2. Add loading state
        val loadingId = java.util.UUID.randomUUID().toString()
        addMessage(Message(id = loadingId, role = com.marvinos.model.MessageRole.ASSISTANT, content = "", isLoading = true))

        viewModelScope.launch {
            try {
                // 3. Prepare AI prompt
                val promptText = if (text.lowercase().contains("specs") || text.lowercase().contains("play")) {
                    deviceContextBuilder.buildDeviceInfoPrompt(mockDeviceProfile, text)
                } else {
                    text
                }

                // 4. API Call
                val jsonResponse = try {
                    geminiApiClient.sendMessage(promptText)
                } catch (e: Exception) {
                    null
                }

                // 5. Parse or Fallback
                val intent = if (jsonResponse != null) {
                    intentParser.parse(jsonResponse, text)
                } else {
                    fallbackMatcher.match(text)
                }

                // Remove loading message
                removeMessage(loadingId)

                // 6. Handle Intent
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

        // Check Permissions
        val requiredPermission = permissionManager.getMissingPermissionFor(intent.action)
        if (requiredPermission != null) {
            val rationale = permissionManager.getRationaleFor(requiredPermission)
            val settingsAction = com.marvinos.permissions.SpecialPermissionHelper().getSettingsActionFor(requiredPermission)
            addMessage(Message.assistant(
                "I need permission to do that.",
                ActionResult.RequiresPermission(requiredPermission, settingsAction, rationale)
            ))
            // We store the intent to retry later if permission is granted
            _pendingIntent.value = intent
            return
        }

        // Confirmation Gate
        if (ActionExecutor.requiresConfirmation(intent)) {
            _pendingIntent.value = intent
            return
        }

        // Execute directly if read-only
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

    fun retryPendingIntent() {
        val intent = _pendingIntent.value ?: return
        _pendingIntent.value = null
        viewModelScope.launch {
            processIntent(intent)
        }
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
