package com.myvoice.app.ui.detail

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myvoice.app.data.db.Thought
import com.myvoice.app.ui.components.SectionCard
import com.myvoice.app.ui.components.SectionTitle
import com.myvoice.app.ui.components.formatTimestamp

@Composable
fun DetailScreen(
    vm: DetailViewModel,
    onBack: () -> Unit
) {
    val thought by vm.thought.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val current = thought ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Row {
                IconButton(onClick = { vm.toggleFavorite() }) {
                    Icon(
                        imageVector = if (current.isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = if (current.isFavorite) "Unfavorite" else "Favorite",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
                IconButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, current.title)
                        putExtra(Intent.EXTRA_TEXT, shareText(current))
                    }
                    context.startActivity(Intent.createChooser(send, "Share thought"))
                }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share")
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        }

        Text(current.title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = buildString {
                append(formatTimestamp(current.createdAt))
                if (current.durationMs > 0) append(" · voice note")
                if (current.llmModel.isNotBlank()) append(" · ${current.llmModel}")
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SectionCard(title = "transcript") {
            SectionTitle("What I said")
            Text(
                text = current.transcript,
                style = MaterialTheme.typography.bodyLarge
            )
        }

        SectionCard(title = "reply") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SectionTitle("Myvoice's reflection")
                Row {
                    IconButton(onClick = { vm.speak(current) }) {
                        Icon(Icons.Filled.VolumeUp, contentDescription = "Read aloud")
                    }
                    IconButton(onClick = { vm.stopSpeaking() }) {
                        Icon(Icons.Filled.VolumeOff, contentDescription = "Stop speaking")
                    }
                    IconButton(onClick = { clipboard.setText(AnnotatedString(current.reply)) }) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy reflection")
                    }
                }
            }
            Text(text = current.reply, style = MaterialTheme.typography.bodyLarge)
            if (current.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = current.tags.joinToString(" · "),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this thought?") },
            text = { Text("This permanently removes the thought from this device.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    vm.delete(onDone = onBack)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

private fun shareText(t: Thought): String = buildString {
    appendLine(t.title)
    appendLine()
    appendLine("— Me:")
    appendLine(t.transcript)
    appendLine()
    appendLine("— Myvoice:")
    appendLine(t.reply)
}
