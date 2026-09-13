package com.myvoice.app.ui.settings

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myvoice.app.core.friendlyMessage
import com.myvoice.app.data.prefs.AppSettings
import com.myvoice.app.data.prefs.SttProvider
import com.myvoice.app.ui.components.SectionCard
import com.myvoice.app.ui.components.SectionTitle
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(vm: SettingsViewModel) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }

    val draft = ui.draft

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        // ---------------- Speech-to-text ----------------
        SectionCard(title = "stt") {
            SectionTitle("Speech-to-text")
            Text(
                "Device mode is free and offline. Deepgram is great for English; " +
                    "Sarvam is best for Indian languages (Hindi, Odia, Tamil, …).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = draft.sttProvider == SttProvider.DEVICE,
                    onClick = { vm.update { it.copy(sttProvider = SttProvider.DEVICE) } },
                    label = { Text("Device") }
                )
                FilterChip(
                    selected = draft.sttProvider == SttProvider.DEEPGRAM,
                    onClick = { vm.update { it.copy(sttProvider = SttProvider.DEEPGRAM) } },
                    label = { Text("Deepgram") }
                )
                FilterChip(
                    selected = draft.sttProvider == SttProvider.SARVAM,
                    onClick = { vm.update { it.copy(sttProvider = SttProvider.SARVAM) } },
                    label = { Text("Sarvam") }
                )
            }
            if (draft.sttProvider == SttProvider.DEEPGRAM) {
                KeyField(
                    label = "Deepgram API key",
                    value = draft.deepgramKey,
                    onChange = { v -> vm.update { it.copy(deepgramKey = v) } }
                )
                LabeledField(
                    label = "Deepgram model",
                    value = draft.deepgramModel,
                    onChange = { v -> vm.update { it.copy(deepgramModel = v) } }
                )
            }
            if (draft.sttProvider == SttProvider.SARVAM) {
                KeyField(
                    label = "Sarvam API subscription key",
                    value = draft.sarvamKey,
                    onChange = { v -> vm.update { it.copy(sarvamKey = v) } }
                )
                LabeledField(
                    label = "Sarvam model (saaras:v3 / saarika:v2.5)",
                    value = draft.sarvamModel,
                    onChange = { v -> vm.update { it.copy(sarvamModel = v) } }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = draft.sarvamMode == "transcribe",
                        onClick = { vm.update { it.copy(sarvamMode = "transcribe") } },
                        label = { Text("Transcribe") }
                    )
                    FilterChip(
                        selected = draft.sarvamMode == "translate",
                        onClick = { vm.update { it.copy(sarvamMode = "translate") } },
                        label = { Text("Translate → English") }
                    )
                }
            }
        }

        // ---------------- Thinking model ----------------
        SectionCard(title = "llm") {
            SectionTitle("Thinking model (Gemini)")
            KeyField(
                label = "Gemini API key",
                value = draft.geminiKey,
                onChange = { v -> vm.update { it.copy(geminiKey = v) } }
            )
            LabeledField(
                label = "Gemini model",
                value = draft.geminiModel,
                onChange = { v -> vm.update { it.copy(geminiModel = v) } }
            )
            LabeledField(
                label = "Personalize my assistant (optional)",
                value = draft.persona,
                onChange = { v -> vm.update { it.copy(persona = v) } },
                minLines = 3
            )
            ToggleRow(
                title = "Let Myvoice search the web when it helps",
                checked = draft.webSearchEnabled,
                onChange = { v -> vm.update { it.copy(webSearchEnabled = v) } }
            )
            if (draft.webSearchEnabled) {
                KeyField(
                    label = "Tavily API key",
                    value = draft.tavilyKey,
                    onChange = { v -> vm.update { it.copy(tavilyKey = v) } }
                )
            }
        }

        // ---------------- Voice output ----------------
        SectionCard(title = "voice") {
            SectionTitle("Voice output")
            ToggleRow(
                title = "Read replies aloud automatically",
                checked = draft.autoSpeak,
                onChange = { v -> vm.update { it.copy(autoSpeak = v) } }
            )
        }

        // ---------------- Cloud sync ----------------
        SectionCard(title = "sync") {
            SectionTitle("Cloud sync (Appwrite)")
            Text(
                "Optionally mirror your thoughts to your own Appwrite database. " +
                    "See the README for the one-time collection setup.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LabeledField(
                label = "Endpoint",
                value = draft.appwriteEndpoint,
                onChange = { v -> vm.update { it.copy(appwriteEndpoint = v) } }
            )
            LabeledField(
                label = "Project ID",
                value = draft.appwriteProject,
                onChange = { v -> vm.update { it.copy(appwriteProject = v) } }
            )
            LabeledField(
                label = "Database ID",
                value = draft.appwriteDatabase,
                onChange = { v -> vm.update { it.copy(appwriteDatabase = v) } }
            )
            LabeledField(
                label = "Collection ID",
                value = draft.appwriteCollection,
                onChange = { v -> vm.update { it.copy(appwriteCollection = v) } }
            )
            KeyField(
                label = "Appwrite API key",
                value = draft.appwriteKey,
                onChange = { v -> vm.update { it.copy(appwriteKey = v) } }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(onClick = { vm.syncNow() }, enabled = !ui.syncing) {
                    Text(if (ui.syncing) "Syncing…" else "Save & sync now")
                }
            }
            ui.syncMessage?.let {
                Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }

        // ---------------- Your data ----------------
        SectionCard(title = "data") {
            SectionTitle("Your data")
            Text(
                "${ui.thoughtCount} thought${if (ui.thoughtCount == 1) "" else "s"} stored on this device. " +
                    "API keys are stored only in the app's private storage.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = {
                    scope.launch {
                        try {
                            val markdown = vm.exportMarkdown()
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Myvoice export")
                                putExtra(Intent.EXTRA_TEXT, markdown)
                            }
                            context.startActivity(Intent.createChooser(send, "Export thoughts"))
                        } catch (t: Throwable) {
                            exportError = friendlyMessage(t)
                        }
                    }
                }) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text("Export all")
                }
                OutlinedButton(onClick = { showClearDialog = true }) {
                    Text("Clear all data")
                }
            }
            exportError?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
            }
        }

        // ---------------- Save ----------------
        Button(
            onClick = { vm.save() },
            enabled = !ui.saving,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (ui.saving) "Saving…" else "Save settings")
        }
        if (ui.saved) {
            Text(
                "✓ Saved",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
        ui.error?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (ui.syncing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear all thoughts?") },
            text = { Text("Every thought stored on this device will be deleted. Cloud copies already synced to Appwrite are not removed.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    vm.clearAllData()
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun KeyField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation()
    )
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    minLines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        minLines = minLines
    )
}

@Composable
private fun ToggleRow(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
