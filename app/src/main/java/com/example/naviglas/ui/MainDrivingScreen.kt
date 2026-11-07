package com.example.naviglas.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naviglas.engine.AssistantState
import com.example.naviglas.ui.theme.AccentGreen
import com.example.naviglas.ui.theme.DarkBackground
import com.example.naviglas.ui.theme.DarkSurface
import com.example.naviglas.ui.theme.DarkSurfaceVariant
import com.example.naviglas.ui.theme.TextPrimary
import com.example.naviglas.ui.theme.TextSecondary
import com.example.naviglas.ui.theme.WarningRed

@Composable
fun MainDrivingScreen(
    currentState: AssistantState,
    recognizedText: String,
    spokenFeedback: String,
    isServiceRunning: Boolean,
    selectedNavAppName: String,
    onMicClick: () -> Unit,
    onQuickEtaClick: () -> Unit,
    onSosClick: () -> Unit,
    onOpenNavAppClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    val isListening = currentState == AssistantState.LISTENING || currentState == AssistantState.WAITING_FOR_CONFIRMATION

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        val isLandscape = maxWidth > maxHeight
        val isCompact = maxHeight < 550.dp || maxWidth < 340.dp
        val scrollState = rememberScrollState()

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leva kolona: Naslov vožnje + Mikrofon dugme
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    DrivingHeader(
                        isServiceRunning = isServiceRunning,
                        onSettingsClick = onSettingsClick
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    MicrophoneButton(
                        isListening = isListening,
                        pulseScale = pulseScale,
                        size = if (isCompact) 110.dp else 140.dp,
                        onMicClick = onMicClick
                    )
                }

                // Desna kolona: Statusna kartica + Brze akcije
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    StatusCard(
                        currentState = currentState,
                        recognizedText = recognizedText,
                        spokenFeedback = spokenFeedback,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    QuickActionsRow(
                        selectedNavAppName = selectedNavAppName,
                        onOpenNavAppClick = onOpenNavAppClick,
                        onQuickEtaClick = onQuickEtaClick,
                        onSosClick = onSosClick
                    )
                }
            }
        } else {
            // Portretni režim sa podrškom za skrolovanje na malim ekranima
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                DrivingHeader(
                    isServiceRunning = isServiceRunning,
                    onSettingsClick = onSettingsClick
                )
                Spacer(modifier = Modifier.height(12.dp))
                StatusCard(
                    currentState = currentState,
                    recognizedText = recognizedText,
                    spokenFeedback = spokenFeedback,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                MicrophoneButton(
                    isListening = isListening,
                    pulseScale = pulseScale,
                    size = if (isCompact) 130.dp else 170.dp,
                    onMicClick = onMicClick
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Dugme na volanu • Dodir mehura • Pritisak ekrana",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                QuickActionsRow(
                    selectedNavAppName = selectedNavAppName,
                    onOpenNavAppClick = onOpenNavAppClick,
                    onQuickEtaClick = onQuickEtaClick,
                    onSosClick = onSosClick
                )
            }
        }
    }
}

@Composable
private fun DrivingHeader(
    isServiceRunning: Boolean,
    onSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Navigation,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(26.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "NaviGlas Vožnja",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isServiceRunning) "Režim vožnje aktivan" else "Spremno",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.BluetoothConnected,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onSettingsClick) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Podešavanja",
                        tint = AccentGreen,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    currentState: AssistantState,
    recognizedText: String,
    spokenFeedback: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val statusTitle = when (currentState) {
                AssistantState.LISTENING -> "Slušam vaše komande..."
                AssistantState.PROCESSING -> "Obrađujem..."
                AssistantState.SPEAKING -> "Asistent govori..."
                AssistantState.WAITING_FOR_CONFIRMATION -> "Čekam vašu potvrdu (Da / Otkaži)..."
                AssistantState.IDLE -> "Pritisnite ili izgovorite komandu"
            }

            Text(
                text = statusTitle,
                fontSize = 15.sp,
                color = AccentGreen,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (recognizedText.isNotBlank()) "\"$recognizedText\"" else spokenFeedback.ifBlank { "Npr. 'Pozovi Tijanu' ili 'Pošalji poruku Verici da stižem'" },
                fontSize = 16.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun MicrophoneButton(
    isListening: Boolean,
    pulseScale: Float,
    size: Dp,
    onMicClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(size)
            .scale(if (isListening) pulseScale else 1.0f)
            .background(
                color = if (isListening) AccentGreen else DarkSurface,
                shape = CircleShape
            )
            .border(
                width = 4.dp,
                color = AccentGreen,
                shape = CircleShape
            )
            .clickable { onMicClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Pokreni mikrofon",
            tint = if (isListening) DarkBackground else AccentGreen,
            modifier = Modifier.size(size * 0.45f)
        )
    }
}

@Composable
private fun QuickActionsRow(
    selectedNavAppName: String,
    onOpenNavAppClick: () -> Unit,
    onQuickEtaClick: () -> Unit,
    onSosClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Dugme za otvaranje Navigacije (Google Maps / Waze) sa mehurićem
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .clickable { onOpenNavAppClick() },
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = AccentGreen)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Map,
                    contentDescription = null,
                    tint = DarkBackground,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "🗺️ Otvori $selectedNavAppName Mape u Vožnji",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clickable { onQuickEtaClick() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⏱ Stižem za 10 min",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
                    .clickable { onSosClick() },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = WarningRed.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = WarningRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ŠALJI SOS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningRed
                    )
                }
            }
        }
    }
}
