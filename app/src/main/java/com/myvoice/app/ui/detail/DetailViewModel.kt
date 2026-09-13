package com.myvoice.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.myvoice.app.AppContainer
import com.myvoice.app.data.db.Thought
import com.myvoice.app.data.db.ThoughtRepository
import com.myvoice.app.speech.TtsManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailViewModel(
    private val repo: ThoughtRepository,
    private val tts: TtsManager,
    thoughtId: String
) : ViewModel() {

    val thought: StateFlow<Thought?> = repo.observeById(thoughtId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun toggleFavorite() {
        viewModelScope.launch {
            repo.byId(currentId())?.let { repo.setFavorite(it.id, !it.isFavorite) }
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            tts.stop()
            repo.delete(currentId())
            onDone()
        }
    }

    fun speak(thought: Thought) {
        tts.speak(thought.reply.ifBlank { thought.transcript })
    }

    fun stopSpeaking() {
        tts.stop()
    }

    private fun currentId(): String = thought.value?.id ?: ""
}

fun detailViewModel(container: AppContainer, thoughtId: String): ViewModelProvider.Factory =
    viewModelFactory {
        initializer {
            DetailViewModel(
                repo = container.thoughtRepository,
                tts = container.ttsManager,
                thoughtId = thoughtId
            )
        }
    }
