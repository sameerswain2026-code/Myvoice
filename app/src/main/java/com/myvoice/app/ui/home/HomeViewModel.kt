package com.myvoice.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.myvoice.app.AppContainer
import com.myvoice.app.audio.AudioRecorder
import com.myvoice.app.core.friendlyMessage
import com.myvoice.app.data.db.Thought
import com.myvoice.app.data.db.ThoughtRepository
import com.myvoice.app.data.prefs.SettingsRepository
import com.myvoice.app.data.prefs.SttProvider
import com.myvoice.app.data.remote.DeepgramClient
import com.myvoice.app.data.remote.SarvamClient
import com.myvoice.app.domain.KeywordTagger
import com.myvoice.app.domain.ThinkingEngine
import com.myvoice.app.domain.ThoughtMetadataGenerator
import com.myvoice.app.domain.TranscriptPolisher
import com.myvoice.app.speech.DeviceSpeechRecognizer
import com.myvoice.app.speech.TtsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

data class HomeUiState(
    val hasMicPermission: Boolean = false,
    val sttProvider: SttProvider = SttProvider.DEVICE,
    val hasGeminiKey: Boolean = false,
    val isRecording: Boolean = false,
    val isTranscribing: Boolean = false,
    val isThinking: Boolean = false,
    val isPolishing: Boolean = false,
    val stage: String? = null,
    val partialTranscript: String = "",
    val transcript: String = "",
    val reply: String = "",
    val usedWeb: Boolean = false,
    val savedToHistory: Boolean = false,
    val lastSavedId: String = "",
    val error: String? = null,
    val autoSpeak: Boolean = false,
    val recordingMs: Long = 0,
    val lastInputWasVoice: Boolean = false
)

class HomeViewModel(
    private val repo: ThoughtRepository,
    private val settingsRepo: SettingsRepository,
    private val engine: ThinkingEngine,
    private val metadataGenerator: ThoughtMetadataGenerator,
    private val polisher: TranscriptPolisher,
    private val recorder: AudioRecorder,
    private val deviceRecognizer: DeviceSpeechRecognizer,
    private val deepgram: DeepgramClient,
    private val sarvam: SarvamClient,
    val tts: TtsManager
) : ViewModel() {

    private val _ui = MutableStateFlow(HomeUiState())
    val ui: StateFlow<HomeUiState> = _ui.asStateFlow()

    /** Whether TTS is currently reading a reply aloud. */
    val speaking: StateFlow<Boolean> = tts.speaking

    private var listenJob: Job? = null
    private var tickJob: Job? = null
    private var lastPartial = ""

    init {
        viewModelScope.launch {
            refreshSettings()
        }
    }

    private suspend fun refreshSettings() {
        val s = settingsRepo.current()
        _ui.update {
            it.copy(
                sttProvider = s.sttProvider,
                hasGeminiKey = s.geminiKey.isNotBlank(),
                autoSpeak = s.autoSpeak
            )
        }
    }

    fun onPermissionResult(granted: Boolean) {
        _ui.update { it.copy(hasMicPermission = granted) }
    }

    fun updateTranscript(value: String) {
        _ui.update { it.copy(transcript = value, savedToHistory = false) }
    }

    fun dismissError() {
        _ui.update { it.copy(error = null) }
    }

    fun startVoiceCapture(languageTag: String) {
        val state = _ui.value
        if (state.isRecording || state.isTranscribing || state.isThinking) return
        tts.stop()
        _ui.update { it.copy(lastInputWasVoice = true, savedToHistory = false) }

        if (state.sttProvider == SttProvider.DEVICE) {
            lastPartial = ""
            listenJob?.cancel()
            listenJob = viewModelScope.launch {
                _ui.update { it.copy(isRecording = true) }
                startTicker()
                try {
                    val text = deviceRecognizer.listen(languageTag) { partial ->
                        lastPartial = partial
                        _ui.update { s -> s.copy(partialTranscript = partial) }
                    }
                    _ui.update { s ->
                        s.copy(
                            isRecording = false,
                            partialTranscript = "",
                            transcript = when {
                                text.isNotBlank() -> text
                                s.transcript.isBlank() -> lastPartial
                                else -> s.transcript
                            },
                            error = if (text.isBlank() && lastPartial.isBlank() && s.transcript.isBlank()) {
                                "Kuch sunayi nahi diya — phir se boliye, ya typing try karein."
                            } else {
                                null
                            }
                        )
                    }
                } catch (t: Throwable) {
                    _ui.update { s ->
                        s.copy(
                            isRecording = false,
                            partialTranscript = "",
                            transcript = s.transcript.ifBlank { lastPartial },
                            error = friendlyMessage(t)
                        )
                    }
                } finally {
                    stopTicker()
                }
            }
        } else {
            try {
                recorder.start()
            } catch (t: Throwable) {
                _ui.update { it.copy(error = friendlyMessage(t)) }
                return
            }
            _ui.update { it.copy(isRecording = true, recordingMs = 0, error = null) }
            startTicker()
        }
    }

    fun stopVoiceCapture() {
        if (!_ui.value.isRecording) return
        if (_ui.value.sttProvider == SttProvider.DEVICE) {
            listenJob?.cancel()
            listenJob = null
            stopTicker()
            _ui.update { s ->
                s.copy(
                    isRecording = false,
                    partialTranscript = "",
                    transcript = s.transcript.ifBlank { lastPartial }
                )
            }
            return
        }
        stopTicker()
        _ui.update { it.copy(isRecording = false, isTranscribing = true) }
        val recording = try {
            recorder.stop()
        } catch (t: Throwable) {
            _ui.update { it.copy(isTranscribing = false, error = friendlyMessage(t)) }
            return
        }
        listenJob = viewModelScope.launch {
            try {
                val s = settingsRepo.current()
                val text = when (s.sttProvider) {
                    SttProvider.DEEPGRAM ->
                        deepgram.transcribe(recording.wav, s.deepgramKey, s.deepgramModel)
                    SttProvider.SARVAM ->
                        sarvam.transcribe(recording.wav, s.sarvamKey, s.sarvamModel, s.sarvamMode)
                    SttProvider.DEVICE -> ""
                }
                _ui.update { current ->
                    current.copy(
                        isTranscribing = false,
                        recordingMs = recording.durationMs,
                        transcript = if (text.isNotBlank()) text else current.transcript,
                        error = if (text.isBlank()) "Awaaz record hui par kuch samajh nahi aaya — phir se try karein." else null
                    )
                }
            } catch (t: Throwable) {
                _ui.update { it.copy(isTranscribing = false, error = friendlyMessage(t)) }
            }
        }
    }

    fun sendThought() {
        val input = _ui.value.transcript.trim()
        if (input.isEmpty() || _ui.value.isThinking || _ui.value.isPolishing) return
        if (!_ui.value.hasGeminiKey) {
            _ui.update {
                it.copy(
                    error = "Gemini API key nahi hai. Settings → 'AI brain' me key daalein — uske bina AI reply nahi banega. (Voice note phir bhi history me save ho sakta hai.)"
                )
            }
            return
        }
        tts.stop()
        listenJob?.cancel()
        _ui.update {
            it.copy(
                isThinking = true,
                isPolishing = true,
                reply = "",
                error = null,
                savedToHistory = false,
                stage = "Aapke shabdon ko saaf kar raha hun…"
            )
        }
        viewModelScope.launch {
            try {
                val s = settingsRepo.current()

                // Step 1: polish the transcript (auto-correction).
                val polished = runCatching { polisher.polish(input) }.getOrDefault(input)
                _ui.update { it.copy(transcript = polished, isPolishing = false, stage = "Soch raha hun…") }

                // Step 2: think (with optional web grounding).
                val context = buildRecentContext(repo.recent(5))
                val result = engine.think(polished, context) { event ->
                    when (event) {
                        is ThinkingEngine.EngineEvent.Stage ->
                            _ui.update { it.copy(stage = event.label) }
                        is ThinkingEngine.EngineEvent.Searching ->
                            _ui.update { it.copy(stage = "Web par dhoond raha hun: ${event.query}") }
                        is ThinkingEngine.EngineEvent.SearchDone ->
                            _ui.update { it.copy(stage = "${event.count} sources padh liye") }
                    }
                }

                // Step 3: save to history.
                _ui.update { it.copy(stage = "History me save kar raha hun…") }
                val meta = metadataGenerator.generate(polished, result.reply)
                val now = System.currentTimeMillis()
                val thought = Thought(
                    id = UUID.randomUUID().toString(),
                    title = meta.title.ifBlank { KeywordTagger.titleFor(polished) },
                    transcript = polished,
                    reply = result.reply,
                    tags = meta.tags,
                    createdAt = now,
                    updatedAt = now,
                    source = if (_ui.value.lastInputWasVoice) "VOICE" else "TEXT",
                    sttProvider = if (_ui.value.lastInputWasVoice) s.sttProvider.name else "",
                    llmModel = s.geminiModel,
                    durationMs = _ui.value.recordingMs
                )
                repo.save(thought)
                _ui.update {
                    it.copy(
                        isThinking = false,
                        stage = null,
                        reply = result.reply,
                        usedWeb = result.usedWeb,
                        savedToHistory = true
                    )
                }
                if (_ui.value.autoSpeak) tts.speak(result.reply)
            } catch (t: Throwable) {
                _ui.update { it.copy(isThinking = false, isPolishing = false, stage = null, error = friendlyMessage(t)) }
            }
        }
    }

    fun speakReply() {
        if (_ui.value.reply.isNotBlank()) tts.speak(_ui.value.reply)
    }

    fun stopSpeaking() {
        tts.stop()
    }

    fun clearSession() {
        listenJob?.cancel()
        listenJob = null
        tts.stop()
        stopTicker()
        lastPartial = ""
        _ui.value = HomeUiState(hasMicPermission = _ui.value.hasMicPermission)
        viewModelScope.launch { refreshSettings() }
    }

    private fun startTicker() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (isActive) {
                delay(250)
                _ui.update { it.copy(recordingMs = it.recordingMs + 250) }
            }
        }
    }

    private fun stopTicker() {
        tickJob?.cancel()
        tickJob = null
    }

    companion object {
        /** Builds the continuity context block from recent thoughts (pure, testable). */
        fun buildRecentContext(thoughts: List<Thought>): String {
            if (thoughts.isEmpty()) return ""
            return thoughts.joinToString("\n") { t ->
                buildString {
                    append("- ")
                    append(t.title)
                    append(": ")
                    append(t.transcript.take(200))
                    if (t.reply.isNotBlank()) {
                        append(" → ")
                        append(t.reply.take(200))
                    }
                }
            }
        }
    }
}

fun homeViewModel(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        HomeViewModel(
            repo = container.thoughtRepository,
            settingsRepo = container.settingsRepository,
            engine = container.thinkingEngine,
            metadataGenerator = container.metadataGenerator,
            polisher = container.transcriptPolisher,
            recorder = container.audioRecorder,
            deviceRecognizer = container.deviceRecognizer,
            deepgram = container.deepgramClient,
            sarvam = container.sarvamClient,
            tts = container.ttsManager
        )
    }
}
