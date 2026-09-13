package com.myvoice.app.ui.detail

import android.content.Intent
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.myvoice.app.domain.DocKind
import com.myvoice.app.ui.components.RichText
import com.myvoice.app.ui.components.SectionCard
import com.myvoice.app.ui.components.SectionTitle
import com.myvoice.app.ui.components.formatTimestamp

@Composable
fun DetailScreen(
    vm: DetailViewModel,
    onBack: () -> Unit
) {
    val thought by vm.thought.collectAsStateWithLifecycle()
    val docCreation by vm.docCreation.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDocDialog by remember { mutableStateOf(false) }

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
                if (current.source == "LIVE") append(" · live chat")
                else if (current.durationMs > 0) append(" · voice note")
                if (current.llmModel.isNotBlank()) append(" · ${current.llmModel}")
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SectionCard(title = "transcript") {
            SectionTitle(if (current.source == "LIVE") "Aapne kya kaha" else "What I said")
            Text(text = current.transcript, style = MaterialTheme.typography.bodyLarge)
        }

        if (current.reply.isNotBlank()) {
            SectionCard(title = "reply") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Myvoice ka jawab")
                    Row {
                        IconButton(onClick = { vm.speak(current) }) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = "Read aloud")
                        }
                        IconButton(onClick = { vm.stopSpeaking() }) {
                            Icon(Icons.Filled.VolumeOff, contentDescription = "Stop speaking")
                        }
                        IconButton(onClick = {
                            clipboard.setText(AnnotatedString(current.reply))
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy reflection")
                        }
                    }
                }
                RichText(text = current.reply)
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

        // Make document
        OutlinedButton(
            onClick = { showDocDialog = true; vm.clearDocStatus() },
            enabled = !docCreation.generating,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Description, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (docCreation.generating) "Document ban raha hai…" else "📄 Document banao (notes, email, report…)")
        }
        if (docCreation.generating) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        docCreation.savedKind?.let {
            Text(
                "✓ '$it' Documents tab me save ho gaya",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
        docCreation.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete this thought?") },
            text = { Text("Ye thought is device se permanently delete ho jayega.") },
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

    if (showDocDialog) {
        DocKindDialog(
            onDismiss = { showDocDialog = false },
            onGenerate = { kind, extra ->
                showDocDialog = false
                vm.generateDocument(kind, extra)
            }
        )
    }
}

@Composable
private fun DocKindDialog(
    onDismiss: () -> Unit,
    onGenerate: (DocKind, String) -> Unit
) {
    var selected by remember { mutableStateOf(DocKind.NOTES) }
    var extra by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Kaisa document banau?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DocKind.entries.forEach { kind ->
                        FilterChip(
                            selected = selected == kind,
                            onClick = { selected = kind },
                            label = { Text("${kind.emoji} ${kind.label}") }
                        )
                    }
                }
                OutlinedTextField(
                    value = extra,
                    onValueChange = { extra = it },
                    label = { Text("Extra instruction (optional)") },
                    placeholder = { Text("jaise: formal tone, 1 page, Hinglish me…") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onGenerate(selected, extra) }) { Text("Generate") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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
