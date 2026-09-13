package com.myvoice.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.myvoice.app.AppContainer
import com.myvoice.app.data.db.Thought
import com.myvoice.app.data.db.ThoughtRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistoryViewModel(private val repo: ThoughtRepository) : ViewModel() {

    private val queryFlow = MutableStateFlow("")
    val query: StateFlow<String> = queryFlow.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val thoughts: StateFlow<List<Thought>> = queryFlow
        .flatMapLatest { q -> repo.search(q) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setQuery(value: String) {
        queryFlow.value = value
    }

    fun toggleFavorite(thought: Thought) {
        viewModelScope.launch { repo.setFavorite(thought.id, !thought.isFavorite) }
    }

    fun delete(thought: Thought) {
        viewModelScope.launch { repo.delete(thought.id) }
    }
}

fun historyViewModel(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
    initializer {
        HistoryViewModel(container.thoughtRepository)
    }
}
