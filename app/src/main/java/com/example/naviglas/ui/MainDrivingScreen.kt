package com.example.naviglas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naviglas.engine.AssistantState
import com.example.naviglas.ui.theme.*

@Composable
fun MainDrivingScreen(
    currentState: AssistantState, recognizedText: String, spokenFeedback: String,
    isServiceRunning: Boolean, selectedNavAppName: String,
    onMicClick: () -> Unit, onCancelClick: () -> Unit,
    confirmationAvailable: Boolean = false, onConfirmClick: () -> Unit = {},
    onQuickEtaClick: () -> Unit, onSosClick: () -> Unit,
    onOpenNavAppClick: () -> Unit, onSettingsClick: () -> Unit
) {
    val active = currentState != AssistantState.IDLE
    val listening = currentState == AssistantState.LISTENING || currentState == AssistantState.WAITING_FOR_CONFIRMATION
    val title = when (currentState) {
        AssistantState.IDLE -> if (isServiceRunning) "Spreman za komandu" else "Povezivanje sa asistentom…"
        AssistantState.LISTENING -> "Slušam vas"
        AssistantState.PROCESSING -> "Obrađujem komandu"
        AssistantState.SPEAKING -> "Asistent govori"
        AssistantState.WAITING_FOR_CONFIRMATION -> "Čekam vaš odgovor"
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(DarkBackground).padding(20.dp)) {
        val landscape = maxWidth > maxHeight
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NAVIGLAS", color = AccentBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                    Text("Vaš glas. Vaš put.", color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(onClick = onSettingsClick, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Default.Settings, "Podešavanja", tint = TextPrimary)
                }
            }
            if (confirmationAvailable) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onConfirmClick, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("Potvrdi radnju") }
                    OutlinedButton(onClick = onCancelClick, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text("Otkaži") }
                }
            }
            if (landscape) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        MicControl(listening, active, isServiceRunning, onMicClick, onCancelClick)
                    }
                    Column(Modifier.weight(1.4f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ConversationCard(title, recognizedText, spokenFeedback)
                        Actions(selectedNavAppName, isServiceRunning && !active, onOpenNavAppClick, onQuickEtaClick, onSosClick)
                    }
                }
            } else {
                ConversationCard(title, recognizedText, spokenFeedback)
                Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    MicControl(listening, active, isServiceRunning, onMicClick, onCancelClick)
                }
                Actions(selectedNavAppName, isServiceRunning && !active, onOpenNavAppClick, onQuickEtaClick, onSosClick)
            }
            Text("Poziv i poruka se izvršavaju tek posle potvrde. Recite „otkaži” da prekinete radnju.", color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}
@Composable private fun ConversationCard(title: String, recognized: String, feedback: String) {
    Card(colors = CardDefaults.cardColors(containerColor = DarkSurface), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp).semantics { liveRegion = LiveRegionMode.Polite }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, color = AccentBlue, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Text(feedback.ifBlank { "Dodirnite mikrofon. Na primer: Pozovi Marka, ili Pošalji poruku Ani da stižem." }, color = TextPrimary, fontSize = 18.sp, lineHeight = 26.sp)
            if (recognized.isNotBlank()) {
                HorizontalDivider(color = TextSecondary.copy(alpha = 0.2f))
                Text("Čuo sam: „$recognized”", color = TextSecondary, fontSize = 15.sp)
            }
        }
    }
}
@Composable private fun MicControl(listening: Boolean, active: Boolean, ready: Boolean, onMic: () -> Unit, onCancel: () -> Unit) {
    Button(
        onClick = if (active) onCancel else onMic, enabled = ready,
        modifier = Modifier.size(156.dp), shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = if (active) DarkSurfaceVariant else AccentBlue)
    ) {
        Icon(if (active) Icons.Default.Stop else Icons.Default.Mic, if (active) "Zaustavi i otkaži" else "Pokreni slušanje", modifier = Modifier.size(58.dp), tint = if (active) TextPrimary else DarkBackground)
    }
    Spacer(Modifier.height(14.dp))
    Text(if (active) "Dodirnite za prekid" else "Dodirnite i govorite", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
    if (listening) Text("Mikrofon je uključen", color = AccentBlue, fontSize = 14.sp)
}
@Composable private fun Actions(nav: String, enabled: Boolean, openNav: () -> Unit, quick: () -> Unit, sos: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FilledTonalButton(onClick = openNav, modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp), shape = RoundedCornerShape(18.dp)) {
            Icon(Icons.Default.Map, null); Spacer(Modifier.width(10.dp)); Text("Otvori $nav", fontSize = 17.sp)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = quick, enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 58.dp), shape = RoundedCornerShape(18.dp)) {
                Text("Brzi odgovor", fontSize = 16.sp)
            }
            OutlinedButton(onClick = sos, enabled = enabled, modifier = Modifier.weight(1f).heightIn(min = 58.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = WarningRed)) {
                Icon(Icons.Default.Shield, null, Modifier.size(20.dp)); Spacer(Modifier.width(6.dp)); Text("SOS", fontSize = 16.sp)
            }
        }
    }
}

