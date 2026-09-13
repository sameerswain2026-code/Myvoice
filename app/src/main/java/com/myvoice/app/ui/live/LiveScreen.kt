package com.myvoice.app.ui.live

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.myvoice.app.live.LiveEngine
import com.myvoice.app.live.LiveMessage
import kotlinx.coroutines.launch

/**
 * Real-time, hands-free voice conversation (advanced-voice-style):
 * listen → think → speak → repeat, running in a foreground service.
 */
@Composable
fun LiveScreen(engine: LiveEngine) {
    val state by engine.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var showEndDialog by remember { mutableStateOf(false) }
    var savedToast by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val micGranted = result[Manifest.permission.RECORD_AUDIO] == true
        if (micGranted) {
            engine.start()
        } else {
            savedToast = "Mic permission ke bina live baat nahi ho sakti"
        }
    }

    fun onStartClicked() {
        val micGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (!micGranted) {
            val perms = buildList {
                add(Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    add(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            permissionLauncher.launch(perms.toTypedArray())
        } else {
            engine.start()
        }
    }

    fun onEndClicked(save: Boolean) {
        scope.launch {
            if (save) {
                val id = engine.endAndSave()
                savedToast = if (id != null) {
                    "✓ Conversation History me save ho gayi"
                } else {
                    "Koi baat hui hi nahi — kuch save nahi hua"
                }
            } else {
                engine.stopNow()
            }
        }
    }

    // Auto-scroll to the newest message.
    LaunchedEffect(state.messages.size, state.partial) {
        val count = state.messages.size
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Live", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "Bolkar baat karo — jaise advanced voice assistant",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (engine.isActive) {
                TextButton(onClick = { showEndDialog = true }) { Text("End") }
            }
        }

        if (state.phase == LiveEngine.Phase.IDLE) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "Mic dabao aur seedha baat karo.\n" +
                        "Main sunta hun, sochta hun, aur bolke jawab deta hun —\n" +
                        "jab tak aap End na dabao.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.size(20.dp))
                LiveOrb(phase = state.phase, muted = false, onClick = { onStartClicked() })
                Spacer(modifier = Modifier.size(12.dp))
                Text("Live shuru karein", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.messages) { message ->
                    MessageBubble(message)
                }
                if (state.partial.isNotBlank()) {
                    item { MessageBubble(LiveMessage(fromUser = true, text = state.partial + "…")) }
                }
            }

            Text(
                text = state.status,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            state.error?.let {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { engine.setMuted(!state.muted) }) {
                    Icon(
                        imageVector = if (state.muted) Icons.Filled.MicOff else Icons.Filled.Mic,
                        contentDescription = if (state.muted) "Unmute mic" else "Mute mic",
                        tint = if (state.muted) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                LiveOrb(
                    phase = state.phase,
                    muted = state.muted,
                    onClick = {
                        when (state.phase) {
                            LiveEngine.Phase.SPEAKING -> engine.interruptSpeaking()
                            else -> showEndDialog = true
                        }
                    }
                )
                Spacer(modifier = Modifier.width(16.dp))
                IconButton(onClick = { showEndDialog = true }) {
                    Icon(
                        Icons.Filled.Stop,
                        contentDescription = "End conversation",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            Text(
                text = when (state.phase) {
                    LiveEngine.Phase.SPEAKING -> "Mic tap karke beech me rok sakte hain (interrupt)"
                    else -> "End dabane par conversation save hogi"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            )
        }

        savedToast?.let {
            Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
        }
    }

    if (showEndDialog) {
        AlertDialog(
            onDismissRequest = { showEndDialog = false },
            title = { Text("Conversation khatam karein?") },
            text = { Text("Puri baat-cheet History me save ho jayegi — baad me isse document bhi bana sakte hain.") },
            confirmButton = {
                TextButton(onClick = {
                    showEndDialog = false
                    onEndClicked(save = true)
                }) { Text("Save & End") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEndDialog = false
                    onEndClicked(save = false)
                }) { Text("Discard") }
            }
        )
    }
}

@Composable
private fun LiveOrb(phase: LiveEngine.Phase, muted: Boolean, onClick: () -> Unit) {
    val busy = phase == LiveEngine.Phase.THINKING || phase == LiveEngine.Phase.STARTING
    FilledIconButton(
        onClick = onClick,
        enabled = !busy,
        modifier = Modifier.size(84.dp),
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = when {
                phase == LiveEngine.Phase.IDLE -> MaterialTheme.colorScheme.primary
                phase == LiveEngine.Phase.SPEAKING -> MaterialTheme.colorScheme.tertiary
                phase == LiveEngine.Phase.THINKING -> MaterialTheme.colorScheme.secondary
                muted -> MaterialTheme.colorScheme.outline
                else -> MaterialTheme.colorScheme.primary
            },
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Icon(
            imageVector = if (phase == LiveEngine.Phase.IDLE) Icons.Filled.GraphicEq else Icons.Filled.Mic,
            contentDescription = null,
            modifier = Modifier.size(38.dp)
        )
    }
}

@Composable
private fun MessageBubble(message: LiveMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromUser) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.fromUser) 16.dp else 4.dp,
                        bottomEnd = if (message.fromUser) 4.dp else 16.dp
                    )
                )
                .background(
                    if (message.fromUser) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                style = MaterialTheme.typography.bodyLarge,
                color = if (message.fromUser) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}
