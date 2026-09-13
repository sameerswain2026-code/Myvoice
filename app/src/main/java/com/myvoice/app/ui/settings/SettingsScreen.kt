package com.myvoice.app.ui.settings

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
    val snackbar = remember { SnackbarHostState() }
    var showClearDialog by remember { mutableStateOf(false) }
    var exportError by remember { mutableStateOf<String?>(null) }

    val draft = ui.draft

    // Feedback whenever settings are saved or sync finishes.
    LaunchedEffect(ui.saved) {
        if (ui.saved) snackbar.showSnackbar("✓ Settings save ho gayi")
    }
    LaunchedEffect(ui.syncMessage) {
        ui.syncMessage?.let { snackbar.showSnackbar(it) }
    }
    LaunchedEffect(ui.error) {
        ui.error?.let { snackbar.showSnackbar(it) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Ghabraiye mat — sirf Gemini key daalna kaafi hai, wo bhi sirf AI jawab ke liye. " +
                    "Baaki sab optional hai.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ---------------- Setup checklist ----------------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Setup checklist", style = MaterialTheme.typography.titleMedium)
                    ChecklistRow(
                        title = "🎤 Voice input (awaaz → text)",
                        status = when {
                            draft.sttProvider == SttProvider.DEVICE -> "✓ Ready — Device mode (free, offline)"
                            draft.sttProvider == SttProvider.DEEPGRAM && draft.deepgramKey.isNotBlank() ->
                                "✓ Ready — Deepgram"
                            draft.sttProvider == SttProvider.SARVAM && draft.sarvamKey.isNotBlank() ->
                                "✓ Ready — Sarvam"
                            else -> "⚠ Cloud provider chuna hai par key khali hai — neeche 'Voice input' me daalein"
                        }
                    )
                    ChecklistRow(
                        title = "🧠 AI brain (jawab Gemini se aate hain)",
                        status = if (draft.geminiKey.isNotBlank()) {
                            "✓ Ready — ${draft.geminiModel}"
                        } else {
                            "⚠ Zaroori: Gemini key daalein, warna AI jawab nahi banega"
                        }
                    )
                    ChecklistRow(
                        title = "🌐 Web search",
                        status = when {
                            !draft.webSearchEnabled -> "Off (optional — chalu kar sakte hain)"
                            draft.tavilyKey.isNotBlank() -> "✓ Ready — Tavily"
                            else -> "⚠ Chalu hai par Tavily key khali hai — key daalein ya band karein"
                        }
                    )
                    ChecklistRow(
                        title = "☁️ Backup & sync",
                        status = if (draft.appwriteKey.isNotBlank() && draft.appwriteProject.isNotBlank()) {
                            "✓ Ready — Appwrite"
                        } else {
                            "Not set (bilkul optional)"
                        }
                    )
                    Text(
                        "Sirf 🧠 Gemini key zaroori hai. Baaki sab optional hai — chhoti chhoti keys se app aur smart hota jayega.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // ---------------- Voice input ----------------
            SectionCard(title = "voice-input") {
                SectionTitle("🎤 Voice input")
                Helper("Aapki awaaz ko text me badalta hai. Shuru me Device mode hi best hai — free hai, offline chalta hai, key ki zaroorat nahi.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = draft.sttProvider == SttProvider.DEVICE,
                        onClick = { vm.update { it.copy(sttProvider = SttProvider.DEVICE) } },
                        label = { Text("Device (free)") }
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
                when (draft.sttProvider) {
                    SttProvider.DEVICE -> Helper("✓ Kuch aur karne ki zaroorat nahi — bas mic dabao aur bolo.")
                    SttProvider.DEEPGRAM -> {
                        Helper("English ke liye sabse accurate. Key yahan se banayein: console.deepgram.com")
                        KeyField(
                            label = "Deepgram API key",
                            value = draft.deepgramKey,
                            onChange = { v -> vm.update { it.copy(deepgramKey = v) } }
                        )
                        LabeledField(
                            label = "Model (default nova-2 theek hai)",
                            value = draft.deepgramModel,
                            onChange = { v -> vm.update { it.copy(deepgramModel = v) } }
                        )
                    }
                    SttProvider.SARVAM -> {
                        Helper("Hindi, Odia, Tamil, Telugu waghera ke liye best. Key yahan se: dashboard.sarvam.ai")
                        KeyField(
                            label = "Sarvam API subscription key",
                            value = draft.sarvamKey,
                            onChange = { v -> vm.update { it.copy(sarvamKey = v) } }
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = draft.sarvamMode == "transcribe",
                                onClick = { vm.update { it.copy(sarvamMode = "transcribe") } },
                                label = { Text("Jaisa bola waisa likhe") }
                            )
                            FilterChip(
                                selected = draft.sarvamMode == "translate",
                                onClick = { vm.update { it.copy(sarvamMode = "translate") } },
                                label = { Text("English me translate kare") }
                            )
                        }
                    }
                }
            }

            // ---------------- AI brain ----------------
            SectionCard(title = "llm") {
                SectionTitle("🧠 AI brain (Gemini)")
                Helper("Isi se jawab, titles, tags aur documents bante hain. Key free me milti hai: aistudio.google.com/apikey")
                KeyField(
                    label = "Gemini API key (zaroori)",
                    value = draft.geminiKey,
                    onChange = { v -> vm.update { it.copy(geminiKey = v) } }
                )
                LabeledField(
                    label = "Model (default gemini-2.0-flash theek hai)",
                    value = draft.geminiModel,
                    onChange = { v -> vm.update { it.copy(geminiModel = v) } }
                )
                LabeledField(
                    label = "Meri pasand (optional — aapke baare me 2 line)",
                    value = draft.persona,
                    onChange = { v -> vm.update { it.copy(persona = v) } },
                    minLines = 2,
                    placeholder = "jaise: main founder hun, seedha aur chhota jawab pasand hai"
                )
            }

            // ---------------- Voice output ----------------
            SectionCard(title = "voice-out") {
                SectionTitle("🔊 Voice output")
                Helper("Jawab bolke sunane ke liye. Phone ka speaker use hota hai.")
                ToggleRow(
                    title = "Jawab aate hi khud bolna shuru",
                    checked = draft.autoSpeak,
                    onChange = { v -> vm.update { it.copy(autoSpeak = v) } }
                )
            }

            // ---------------- Advanced: web search ----------------
            ExpandableSection(
                title = "🌐 Web search (optional)",
                subtitle = "News, facts, current cheezon ke liye — Tavily key chahiye"
            ) {
                ToggleRow(
                    title = "Jab zaroorat ho web par dhundhe",
                    checked = draft.webSearchEnabled,
                    onChange = { v -> vm.update { it.copy(webSearchEnabled = v) } }
                )
                if (draft.webSearchEnabled) {
                    Helper("Free key yahan se: app.tavily.com")
                    KeyField(
                        label = "Tavily API key",
                        value = draft.tavilyKey,
                        onChange = { v -> vm.update { it.copy(tavilyKey = v) } }
                    )
                }
            }

            // ---------------- Advanced: cloud sync ----------------
            ExpandableSection(
                title = "☁️ Backup & sync (optional, advanced)",
                subtitle = "Apne Appwrite account par thoughts ka backup"
            ) {
                Helper("One-time setup README me likha hai. Confuse ho to skip karein — app bina iske bhi pura chalta hai.")
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
                        Text(if (ui.syncing) "Sync ho raha hai…" else "Save & sync now")
                    }
                }
            }

            // ---------------- Your data ----------------
            SectionCard(title = "data") {
                SectionTitle("🗄️ Aapka data")
                Text(
                    "${ui.thoughtCount} thoughts + ${ui.docCount} documents is device par hain. " +
                        "Keys sirf app ke andar private rehti hain.",
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

            Button(
                onClick = { vm.save() },
                enabled = !ui.saving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (ui.saving) "Saving…" else "✓ Save settings")
            }
            Spacer(modifier = Modifier.height(40.dp))
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Saara data clear karein?") },
            text = { Text("Is device ke sab thoughts aur documents delete ho jayenge. Cloud par synced copies nahi hategi.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    vm.clearAllData()
                    scope.launch { snackbar.showSnackbar("Saara data clear ho gaya") }
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ChecklistRow(title: String, status: String) {
    Column {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            status,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun Helper(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun ExpandableSection(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium)
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }
            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    content()
                }
            }
        }
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
    minLines: Int = 1,
    placeholder: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = { placeholder?.let { Text(it) } },
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
