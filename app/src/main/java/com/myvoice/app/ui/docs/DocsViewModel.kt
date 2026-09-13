package com.myvoice.app.ui.docs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.myvoice.app.AppContainer
import com.myvoice.app.core.friendlyMessage
import com.myvoice.app.data.db.DocEntity
import com.myvoice.app.data.db.DocRepository
import com.myvoice.app.data.prefs.SettingsRepository
import com.myvoice.app.domain.DocGenerator
import com.myvoice.app.domain.DocKind
import com.myvoice.app.domain.KeywordTagger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class CreateDocState(
    val generating: Boolean = false,
    val created: Boolean = false,
    val error: String? = null
)

class DocsViewModel(
    private val docRepo: DocRepository,
    private val generator: DocGenerator,
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    val docs: StateFlow<List<DocEntity>> = docRepo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _create = MutableStateFlow(CreateDocState())
    val create: StateFlow<CreateDocState> = _create.asStateFlow()

    fun delete(doc: DocEntity) {
        viewModelScope.launch { docRepo.delete(doc.id) }
    }

    fun resetCreateState() {
        _create.value = CreateDocState()
    }

    /** Generates a document from text the user types/pastes directly. */
    fun createFromText(text: String, kind: DocKind, extra: String = "") {
        val source = text.trim()
        if (source.isEmpty() || _create.value.generating) return
        viewModelScope.launch {
            _create.update { CreateDocState(generating = true) }
            try {
                val settings = settingsRepo.current()
                val content = generator.generate(kind, source, extra, settings.persona)
                val now = System.currentTimeMillis()
                docRepo.save(
                    DocEntity(
                        id = UUID.randomUUID().toString(),
                        title = KeywordTagger.titleFor(source),
                        kind = kind.id,
                        content = content,
                        source = "TYPED",
                        sourceId = "",
                        createdAt = now,
                        updatedAt = now
                    )
                )
                _create.update { CreateDocState(created = true) }
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                _create.update { CreateDocState(error = friendlyMessage(t)) }
            }
        }
    }
}

fun docsViewModel(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        DocsViewModel(
            docRepo = container.docRepository,
            generator = container.docGenerator,
            settingsRepo = container.settingsRepository
        )
    }
}

class DocDetailViewModel(
    private val docRepo: DocRepository,
    private val tts: com.myvoice.app.speech.TtsManager,
    private val docId: String
) : ViewModel() {

    val doc: StateFlow<DocEntity?> = docRepo.observeById(docId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun speak() {
        doc.value?.let { tts.speak(it.content) }
    }

    fun stopSpeaking() {
        tts.stop()
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            tts.stop()
            docRepo.delete(doc.value?.id ?: docId)
            onDone()
        }
    }
}

fun docDetailViewModel(container: AppContainer, docId: String): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            DocDetailViewModel(
                docRepo = container.docRepository,
                tts = container.ttsManager,
                docId = docId
            )
        }
    }
