package com.myvoice.app.live

import android.content.Context
import com.myvoice.app.core.friendlyMessage
import com.myvoice.app.data.db.Thought
import com.myvoice.app.data.db.ThoughtRepository
import com.myvoice.app.data.prefs.SettingsRepository
import com.myvoice.app.data.remote.GeminiClient
import com.myvoice.app.domain.KeywordTagger
import com.myvoice.app.domain.ThoughtMetadataGenerator
import com.myvoice.app.speech.DeviceSpeechRecognizer
import com.myvoice.app.speech.TtsManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.UUID

data class LiveMessage(
    val fromUser: Boolean,
    val text: String,
    val at: Long = System.currentTimeMillis()
)

/**
 * Runs a hands-free, real-time voice conversation loop:
 * listen → think → speak → listen again (like an advanced voice assistant).
 *
 * Owned by [com.myvoice.app.AppContainer]; [LiveService] keeps the process
 * alive in the background while a conversation is running.
 */
class LiveEngine(
    private val context: Context,
    private val repo: ThoughtRepository,
    private val settingsRepo: SettingsRepository,
    private val gemini: GeminiClient,
    private val metadataGenerator: ThoughtMetadataGenerator,
    private val recognizer: DeviceSpeechRecognizer,
    private val tts: TtsManager
) {

    enum class Phase { IDLE, STARTING, LISTENING, THINKING, SPEAKING }

    data class State(
        val phase: Phase = Phase.IDLE,
        val partial: String = "",
        val messages: List<LiveMessage> = emptyList(),
        val muted: Boolean = false,
        val status: String = "",
        val error: String? = null
    )

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var loopJob: Job? = null
    private var sessionStartedAt = 0L

    val isActive: Boolean get() = _state.value.phase != Phase.IDLE

    /** Starts a new live conversation (mic permission must already be granted). */
    fun start() {
        if (isActive) return
        sessionStartedAt = System.currentTimeMillis()
        _state.value = State(phase = Phase.STARTING, status = "Shuru kar raha hun…")
        LiveService.start(context)
        loopJob = scope.launch { runLoop() }
    }

    /** Mutes the mic (and silences the assistant) without ending the session. */
    fun setMuted(muted: Boolean) {
        _state.update { it.copy(muted = muted, status = if (muted) "Mic muted" else "Mic on") }
        if (muted) {
            tts.stop()
            restartLoop()
        }
    }

    /** Barge-in: silence the assistant mid-sentence; the loop resumes listening. */
    fun interruptSpeaking() {
        tts.stop()
    }

    /** Hard stop without saving. */
    fun stopNow() {
        loopJob?.cancel()
        loopJob = null
        tts.stop()
        LiveService.stop(context)
        _state.value = State()
    }

    /** Stops the session and saves it to history. Returns the thought id or null. */
    suspend fun endAndSave(): String? {
        val messages = _state.value.messages
        val duration = if (sessionStartedAt > 0) System.currentTimeMillis() - sessionStartedAt else 0L
        stopNow()
        if (messages.none { it.fromUser }) return null
        val userText = messages.filter { it.fromUser }.joinToString("\n\n") { it.text }
        val assistantText = messages.filterNot { it.fromUser }.joinToString("\n\n") { it.text }
        val firstUserMessage = messages.first { it.fromUser }.text
        val meta = try {
            metadataGenerator.generate(firstUserMessage, assistantText)
        } catch (_: Exception) {
            null
        }
        val now = System.currentTimeMillis()
        val model = try { settingsRepo.current().geminiModel } catch (_: Exception) { "" }
        val thought = Thought(
            id = UUID.randomUUID().toString(),
            title = meta?.title?.takeIf { it.isNotBlank() } ?: KeywordTagger.titleFor(firstUserMessage),
            transcript = userText,
            reply = assistantText,
            tags = meta?.tags.orEmpty(),
            createdAt = now,
            updatedAt = now,
            source = "LIVE",
            sttProvider = "DEVICE",
            llmModel = model,
            durationMs = duration
        )
        repo.save(thought)
        return thought.id
    }

    private fun restartLoop() {
        loopJob?.cancel()
        if (!isActive) return
        loopJob = scope.launch { runLoop() }
    }

    private suspend fun runLoop() {
        while (currentCoroutineContext().isActive && _state.value.phase != Phase.IDLE) {
            if (_state.value.muted) {
                delay(250)
                continue
            }
            _state.update {
                it.copy(
                    phase = Phase.LISTENING,
                    status = if (it.partial.isBlank()) "Sun raha hun… boliye" else it.status,
                    error = null
                )
            }
            val heard: String? = try {
                recognizer.listen(Locale.getDefault().toLanguageTag()) { partial ->
                    _state.update { it.copy(partial = partial, status = "Sun raha hun…") }
                }
            } catch (t: Throwable) {
                if (t is CancellationException) throw t
                _state.update { it.copy(error = friendlyMessage(t)) }
                delay(800)
                null
            }
            if (_state.value.phase == Phase.IDLE) return
            _state.update { it.copy(partial = "") }
            if (heard.isNullOrBlank()) {
                delay(350)
                continue
            }
            _state.update {
                it.copy(
                    messages = it.messages + LiveMessage(fromUser = true, text = heard),
                    phase = Phase.THINKING,
                    status = "Soch raha hun…",
                    error = null
                )
            }
            val reply = think(heard)
            if (_state.value.phase == Phase.IDLE) return
            if (reply == null) {
                delay(300)
                continue
            }
            _state.update {
                it.copy(
                    messages = it.messages + LiveMessage(fromUser = false, text = reply),
                    phase = Phase.SPEAKING,
                    status = "Jawab de raha hun… (tap to interrupt)"
                )
            }
            try {
                tts.speakAwait(reply)
            } catch (_: Throwable) {
            }
            if (_state.value.phase == Phase.IDLE) return
            delay(400)
        }
    }

    private suspend fun think(userText: String): String? {
        val s = settingsRepo.current()
        if (!s.hasGemini) {
            _state.update {
                it.copy(error = "Gemini API key nahi hai. Settings → 'AI brain' me key daalein — tabhi live jawab aayega.")
            }
            return null
        }
        return try {
            gemini.generate(
                system = liveSystemPrompt(s.persona),
                user = buildLiveContext(_state.value.messages),
                temperature = 0.8
            )
        } catch (t: Throwable) {
            if (t is CancellationException) throw t
            _state.update { it.copy(error = friendlyMessage(t)) }
            null
        }
    }

    companion object {
        /** Builds the rolling conversation context (pure, testable). */
        fun buildLiveContext(messages: List<LiveMessage>): String {
            val recent = messages.takeLast(12)
            if (recent.isEmpty()) return ""
            return "LIVE CONVERSATION SO FAR (latest last):\n" +
                recent.joinToString("\n") { m ->
                    if (m.fromUser) "User: ${m.text}" else "Myvoice: ${m.text}"
                } +
                "\n\nRespond naturally to the latest user message."
        }

        /** The live conversation persona (pure, testable). */
        fun liveSystemPrompt(persona: String): String = buildString {
            append(
                "You are Myvoice, the user's live voice conversation partner — like a sharp, warm friend. " +
                    "Keep replies SHORT (2–6 sentences, under ~80 words) because they are spoken aloud. " +
                    "Plain flowing speech only: no markdown, no bullets, no emoji. " +
                    "Be direct and thoughtful; ask a follow-up question only when it truly helps. " +
                    "Match the user's language (English, Hindi, Hinglish, Odia…)."
            )
            if (persona.isNotBlank()) {
                append(" Personalization: ").append(persona.trim())
            }
        }
    }
}
