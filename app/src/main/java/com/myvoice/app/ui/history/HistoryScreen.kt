package com.myvoice.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myvoice.app.R
import com.myvoice.app.data.db.Thought
import com.myvoice.app.ui.components.EmptyState
import com.myvoice.app.ui.components.formatTimestamp

@Composable
fun HistoryScreen(
    vm: HistoryViewModel,
    onOpenThought: (String) -> Unit
) {
    val query by vm.query.collectAsStateWithLifecycle()
    val thoughts by vm.thoughts.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("History", style = MaterialTheme.typography.headlineMedium)

        OutlinedTextField(
            value = query,
            onValueChange = vm::setQuery,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search your thoughts…") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true
        )

        if (thoughts.isEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            EmptyState(
                icon = Icons.Outlined.History,
                title = stringResource(R.string.empty_history_title),
                body = stringResource(R.string.empty_history_body)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(thoughts, key = { it.id }) { thought ->
                    ThoughtRow(
                        thought = thought,
                        onClick = { onOpenThought(thought.id) },
                        onToggleFavorite = { vm.toggleFavorite(thought) },
                        onDelete = { vm.delete(thought) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThoughtRow(
    thought: Thought,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (thought.source == "VOICE") {
                        Icon(
                            Icons.Filled.Mic,
                            contentDescription = "Voice thought",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = thought.title,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1
                    )
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (thought.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = if (thought.isFavorite) "Unfavorite" else "Favorite",
                        tint = if (thought.isFavorite) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = thought.transcript,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
            if (thought.tags.isNotEmpty()) {
                Text(
                    text = thought.tags.joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = buildString {
                    append(
                        when (thought.source) {
                            "LIVE" -> "💬 Live chat"
                            "VOICE" -> "🎤 Voice note"
                            else -> "⌨️ Typed"
                        }
                    )
                    append(" · ")
                    append(formatTimestamp(thought.createdAt))
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
