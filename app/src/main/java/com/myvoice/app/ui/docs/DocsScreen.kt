package com.myvoice.app.ui.docs

import android.content.Intent
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myvoice.app.R
import com.myvoice.app.data.db.DocEntity
import com.myvoice.app.domain.DocKind
import com.myvoice.app.ui.components.EmptyState
import com.myvoice.app.ui.components.RichText
import com.myvoice.app.ui.components.formatTimestamp

@Composable
fun DocsScreen(
    vm: DocsViewModel,
    onOpenDoc: (String) -> Unit
) {
    val docs by vm.docs.collectAsStateWithLifecycle()
    val create by vm.create.collectAsStateWithLifecycle()
    var showNewDialog by remember { mutableStateOf(false) }

    LaunchedEffectBlock(create.created) { vm.resetCreateState() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Documents", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Apne thoughts ya live chats se notes, email, report waghera banao — sab yahan save hoga.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(onClick = { showNewDialog = true; vm.resetCreateState() }) {
            Text("＋ Naya document banao")
        }

        if (create.generating) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("Ban raha hai…", style = MaterialTheme.typography.labelLarge)
        }
        create.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        if (docs.isEmpty()) {
            Spacer(modifier = Modifier.height(24.dp))
            EmptyState(
                icon = Icons.Outlined.Description,
                title = "Koi document nahi",
                body = "Kisi thought ko kholein aur 'Document banao' dabayein — ya upar se naya document shuru karein."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(docs, key = { it.id }) { doc ->
                    DocRow(
                        doc = doc,
                        onClick = { onOpenDoc(doc.id) },
                        onDelete = { vm.delete(doc) }
                    )
                }
            }
        }
    }

    if (showNewDialog) {
        NewDocDialog(
            generating = create.generating,
            onDismiss = { showNewDialog = false },
            onCreate = { text, kind, extra ->
                vm.createFromText(text, kind, extra)
            }
        )
    }
}

@Composable
private fun LaunchedEffectBlock(key: Boolean, onTrue: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(key) {
        if (key) onTrue()
    }
}

@Composable
private fun DocRow(doc: DocEntity, onClick: () -> Unit, onDelete: () -> Unit) {
    val kind = DocKind.byId(doc.kind)
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    maxLines = 2
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                text = "${kind.emoji} ${kind.label} · ${formatTimestamp(doc.createdAt)}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = doc.content.replace(Regex("[#*`>-]"), "").take(120),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun NewDocDialog(
    generating: Boolean,
    onDismiss: () -> Unit,
    onCreate: (String, DocKind, String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var extra by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(DocKind.NOTES) }

    AlertDialog(
        onDismissRequest = { if (!generating) onDismiss() },
        title = { Text("Naya document") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Aapka content / idea / points") },
                    minLines = 4,
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DocKind.entries.forEach { k ->
                        FilterChip(
                            selected = kind == k,
                            onClick = { kind = k },
                            label = { Text("${k.emoji} ${k.label}") }
                        )
                    }
                }
                OutlinedTextField(
                    value = extra,
                    onValueChange = { extra = it },
                    label = { Text("Extra instruction (optional)") },
                    minLines = 1,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(text, kind, extra) },
                enabled = text.isNotBlank() && !generating
            ) { Text("Generate") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !generating) { Text("Cancel") }
        }
    )
}

@Composable
fun DocDetailScreen(
    vm: DocDetailViewModel,
    onBack: () -> Unit
) {
    val doc by vm.doc.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    var showDelete by remember { mutableStateOf(false) }

    val current = doc ?: return
    val kind = DocKind.byId(current.kind)

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
                IconButton(onClick = { vm.speak() }) {
                    Icon(Icons.Filled.VolumeUp, contentDescription = "Read aloud")
                }
                IconButton(onClick = { vm.stopSpeaking() }) {
                    Icon(Icons.Filled.VolumeOff, contentDescription = "Stop speaking")
                }
                IconButton(onClick = {
                    clipboard.setText(AnnotatedString(current.content))
                }) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy")
                }
                IconButton(onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, current.title)
                        putExtra(Intent.EXTRA_TEXT, current.content)
                    }
                    context.startActivity(Intent.createChooser(send, "Share document"))
                }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share")
                }
                IconButton(onClick = { showDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        }

        Text(current.title, style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "${kind.emoji} ${kind.label} · ${formatTimestamp(current.createdAt)}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = androidx.compose.material3.CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                RichText(text = current.content)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete this document?") },
            confirmButton = {
                TextButton(onClick = {
                    showDelete = false
                    vm.delete(onDone = onBack)
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) { Text("Cancel") }
            }
        )
    }
}
