package com.myvoice.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.myvoice.app.AppContainer
import com.myvoice.app.core.friendlyMessage
import com.myvoice.app.data.db.DocEntity
import com.myvoice.app.data.db.DocRepository
import com.myvoice.app.data.db.Thought
import com.myvoice.app.data.db.ThoughtRepository
import com.myvoice.app.data.prefs.SettingsRepository
import com.myvoice.app.domain.DocKind
import com.myvoice.app.domain.DocGenerator
import com.myvoice.app.speech.TtsManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class DocCreationState(
    val generating: Boolean = false,
    val savedKind: String? = null,
    val error: String? = null
)

class DetailViewModel(
    private val thoughtRepo: ThoughtRepository,
    private val docRepo: DocRepository,
    private val docGenerator: DocGenerator,
    private val settingsRepo: SettingsRepository,
    private val tts: TtsManager,
    thoughtId: String
) : ViewModel() {

    val thought: StateFlow<Thought?> = thoughtRepo.observeById(thoughtId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _docCreation = MutableStateFlow(DocCreationState())
    val docCreation: StateFlow<DocCreationState> = _docCreation.asStateFlow()

    private var docJob: Job? = null

    fun toggleFavorite() {
        viewModelScope.launch {
            thoughtRepo.byId(thoughtId())?.let { thoughtRepo.setFavorite(it.id, !it.isFavorite) }
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            tts.stop()
            thoughtRepo.delete(thoughtId())
            onDone()
        }
    }

    fun speak(t: Thought) {
        tts.speak(t.reply.ifBlank { t.transcript })
    }

    fun stopSpeaking() {
        tts.stop()
    }

    fun clearDocStatus() {
        _docCreation.update { DocCreationState() }
    }

    fun generateDocument(kind: DocKind, extra: String = "") {
        val t = thought.value ?: return
        if (_docCreation.value.generating) return
        docJob?.cancel()
        docJob = viewModelScope.launch {
            _docCreation.update { DocCreationState(generating = true) }
            try {
                val settings = settingsRepo.current()
                val source = buildString {
                    appendLine("WHAT THE USER SAID / ASKED:")
                    appendLine(t.transcript)
                    if (t.reply.isNotBlank()) {
                        appendLine()
                        appendLine("MYVOICE'S EARLIER REFLECTIONS:")
                        appendLine(t.reply)
                    }
                }
                val content = docGenerator.generate(kind, source, extra, settings.persona)
                val now = System.currentTimeMillis()
                docRepo.save(
                    DocEntity(
                        id = UUID.randomUUID().toString(),
                        title = t.title.ifBlank { kind.label },
                        kind = kind.id,
                        content = content,
                        source = if (t.source == "LIVE") "CONVERSATION" else "THOUGHT",
                        sourceId = t.id,
                        createdAt = now,
                        updatedAt = now
                    )
                )
                _docCreation.update { DocCreationState(savedKind = kind.label) }
            } catch (t2: Throwable) {
                if (t2 is kotlinx.coroutines.CancellationException) throw t2
                _docCreation.update { DocCreationState(error = friendlyMessage(t2)) }
            }
        }
    }

    private fun thoughtId(): String = thought.value?.id ?: ""
}

fun detailViewModel(container: AppContainer, thoughtId: String): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            DetailViewModel(
                thoughtRepo = container.thoughtRepository,
                docRepo = container.docRepository,
                docGenerator = container.docGenerator,
                settingsRepo = container.settingsRepository,
                tts = container.ttsManager,
                thoughtId = thoughtId
            )
        }
    }
