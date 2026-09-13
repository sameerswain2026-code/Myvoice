package com.myvoice.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.myvoice.app.AppContainer
import com.myvoice.app.core.friendlyMessage
import com.myvoice.app.data.db.Thought
import com.myvoice.app.data.db.ThoughtRepository
import com.myvoice.app.data.db.DocRepository
import com.myvoice.app.data.prefs.AppSettings
import com.myvoice.app.data.prefs.SettingsRepository
import com.myvoice.app.data.remote.AppwriteSync
import com.myvoice.app.ui.components.formatTimestamp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepo: SettingsRepository,
    private val appwrite: AppwriteSync,
    private val repo: ThoughtRepository,
    private val docRepo: DocRepository
) : ViewModel() {

    data class UiState(
        val loaded: Boolean = false,
        val draft: AppSettings = AppSettings(),
        val saving: Boolean = false,
        val saved: Boolean = false,
        val syncing: Boolean = false,
        val syncMessage: String? = null,
        val error: String? = null,
        val thoughtCount: Int = 0,
        val docCount: Int = 0
    )

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val s = settingsRepo.current()
            val count = repo.count()
            val docs = docRepo.count()
            _ui.update { it.copy(loaded = true, draft = s, thoughtCount = count, docCount = docs) }
        }
    }

    fun update(transform: (AppSettings) -> AppSettings) {
        _ui.update { it.copy(draft = transform(it.draft), saved = false) }
    }

    fun save() {
        viewModelScope.launch {
            _ui.update { it.copy(saving = true, error = null) }
            try {
                settingsRepo.save(_ui.value.draft)
                _ui.update { it.copy(saving = false, saved = true) }
            } catch (t: Throwable) {
                _ui.update { it.copy(saving = false, error = friendlyMessage(t)) }
            }
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            save()
            _ui.update { it.copy(syncing = true, syncMessage = null, error = null) }
            try {
                val result = appwrite.sync()
                _ui.update { it.copy(syncing = false, syncMessage = result.summary()) }
            } catch (t: Throwable) {
                _ui.update { it.copy(syncing = false, error = friendlyMessage(t)) }
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repo.clearAll()
            docRepo.clearAll()
            _ui.update { it.copy(thoughtCount = 0, docCount = 0) }
        }
    }

    suspend fun exportMarkdown(): String {
        val thoughts = repo.observeAll().first()
        return thoughts.toMarkdown()
    }
}

/** Builds a Markdown export of all thoughts (pure, testable). */
fun List<Thought>.toMarkdown(): String = buildString {
    appendLine("# Myvoice export — ${formatTimestamp(System.currentTimeMillis())}")
    appendLine()
    if (this@toMarkdown.isEmpty()) {
        appendLine("_No thoughts yet._")
        return@buildString
    }
    sortedBy { it.createdAt }.forEach { t ->
        appendLine("## ${t.title}")
        appendLine("_${formatTimestamp(t.createdAt)}${if (t.tags.isNotEmpty()) " · " + t.tags.joinToString(", ") else ""}_")
        appendLine()
        appendLine("**Me:** ${t.transcript}")
        appendLine()
        if (t.reply.isNotBlank()) {
            appendLine("**Myvoice:**")
            appendLine()
            appendLine(t.reply)
        }
        appendLine()
        appendLine("---")
        appendLine()
    }
}

fun settingsViewModel(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        SettingsViewModel(
            settingsRepo = container.settingsRepository,
            appwrite = container.appwriteSync,
            repo = container.thoughtRepository,
            docRepo = container.docRepository
        )
    }
}
