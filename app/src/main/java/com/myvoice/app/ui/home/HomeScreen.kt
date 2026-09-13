package com.myvoice.app.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myvoice.app.R
import com.myvoice.app.data.prefs.SttProvider
import com.myvoice.app.ui.components.RichText
import com.myvoice.app.ui.components.formatMillis
import java.util.Locale

@Composable
fun HomeScreen(
    vm: HomeViewModel,
    onOpenSettings: () -> Unit,
    onOpenThought: (String) -> Unit
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val speaking by vm.speaking.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        vm.onPermissionResult(granted)
        if (granted) {
            vm.startVoiceCapture(Locale.getDefault().toLanguageTag())
            scope.launch { snackbar.showSnackbar("🎤 Recording shuru — rokne ke liye mic par dobara tap karein") }
        } else {
            scope.launch { snackbar.showSnackbar("Mic permission chahiye voice ke liye") }
        }
    }

    fun onMicClicked() {
        val granted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            vm.startVoiceCapture(Locale.getDefault().toLanguageTag())
            scope.launch { snackbar.showSnackbar("🎤 Recording shuru — rokne ke liye mic par dobara tap karein") }
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun onOrbClick() {
        when {
            ui.isRecording -> {
                vm.stopVoiceCapture()
                scope.launch { snackbar.showSnackbar("✋ Recording band — text taiyar ho raha hai") }
            }
            speaking -> {
                vm.stopSpeaking()
                scope.launch { snackbar.showSnackbar("🔇 Bolna band kiya") }
            }
            else -> onMicClicked()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = stringResource(R.string.tagline),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Outlined.Settings,
                        contentDescription = "Open settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            MicOrb(
                isRecording = ui.isRecording,
                isBusy = ui.isTranscribing || ui.isThinking || ui.isPolishing,
                isSpeaking = speaking,
                onClick = { onOrbClick() }
            )

            Text(
                text = when {
                    ui.isRecording && ui.sttProvider != SttProvider.DEVICE ->
                        "Recording… ${formatMillis(ui.recordingMs)} — rokne ke liye tap karein"
                    ui.isRecording -> "Sun raha hun… rokne ke liye tap karein"
                    ui.isTranscribing -> "Aapki awaaz text ban rahi hai…"
                    ui.isThinking -> ui.stage ?: "Soch raha hun…"
                    speaking -> "🔊 Bol raha hun — band karne ke liye mic tap karein"
                    else -> "Mic tap karke boliye — ya neeche type karein"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (ui.partialTranscript.isNotBlank()) {
                Text(
                    text = ui.partialTranscript,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = ui.transcript,
                onValueChange = vm::updateTranscript,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8,
                placeholder = { Text(stringResource(R.string.hint_transcript)) }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        vm.clearSession()
                        scope.launch { snackbar.showSnackbar("Session clear ho gaya") }
                    },
                    enabled = !ui.isThinking && !ui.isRecording && !ui.isTranscribing
                ) {
                    Text("Clear")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        vm.sendThought()
                        scope.launch { snackbar.showSnackbar("🧠 Sochna shuru — reply thodi der me aayega") }
                    },
                    enabled = ui.transcript.isNotBlank() && !ui.isThinking && !ui.isPolishing &&
                        !ui.isRecording && !ui.isTranscribing
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.think_button))
                }
            }

            if (ui.reply.isNotBlank()) {
                ReplyCard(
                    reply = ui.reply,
                    usedWeb = ui.usedWeb,
                    speaking = speaking,
                    onSpeak = {
                        vm.speakReply()
                        scope.launch { snackbar.showSnackbar("🔊 Padh raha hun…") }
                    },
                    onStop = vm::stopSpeaking,
                    onCopy = {
                        clipboard.setText(AnnotatedString(ui.reply))
                        scope.launch { snackbar.showSnackbar("✓ Copy ho gaya") }
                    }
                )
            }

            if (ui.savedToHistory && ui.lastSavedId.isNotBlank()) {
                TextButton(onClick = { onOpenThought(ui.lastSavedId) }) {
                    Text("✓ History me save ho gaya — kholein", color = MaterialTheme.colorScheme.primary)
                }
            }

            ui.error?.let { message ->
                ErrorBanner(message = message, onDismiss = vm::dismissError)
            }

            Spacer(modifier = Modifier.padding(bottom = 48.dp))
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun MicOrb(
    isRecording: Boolean,
    isBusy: Boolean,
    isSpeaking: Boolean,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "pulse")
    val scale by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    Box(modifier = Modifier.padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
        if (isRecording) {
            Box(
                modifier = Modifier
                    .size(136.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
            )
        }
        FilledIconButton(
            onClick = onClick,
            enabled = !isBusy,
            modifier = Modifier.size(96.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = when {
                    isRecording -> MaterialTheme.colorScheme.error
                    isSpeaking -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Icon(
                imageVector = when {
                    isRecording -> Icons.Filled.Stop
                    isSpeaking -> Icons.Filled.VolumeOff
                    else -> Icons.Filled.Mic
                },
                contentDescription = when {
                    isRecording -> stringResource(R.string.mic_stop)
                    isSpeaking -> "Stop speaking"
                    else -> stringResource(R.string.mic_start)
                },
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

@Composable
private fun ReplyCard(
    reply: String,
    usedWeb: Boolean,
    speaking: Boolean,
    onSpeak: () -> Unit,
    onStop: () -> Unit,
    onCopy: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (usedWeb) "Myvoice · web-grounded" else "Myvoice",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Row {
                    if (speaking) {
                        TextButton(onClick = onStop) { Text("◼ Stop") }
                    } else {
                        IconButton(onClick = onSpeak) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = "Read aloud")
                        }
                    }
                    IconButton(onClick = onCopy) {
                        Icon(Icons.Filled.ContentCopy, contentDescription = "Copy reply")
                    }
                }
            }
            RichText(text = reply)
        }
    }
}

@Composable
private fun ErrorBanner(message: String, onDismiss: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onDismiss) { Text("Dismiss") }
        }
    }
}
