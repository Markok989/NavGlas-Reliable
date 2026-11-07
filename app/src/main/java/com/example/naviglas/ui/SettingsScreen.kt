package com.example.naviglas.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naviglas.ui.theme.*

data class NavAppItem(val packageName: String, val name: String)

@Composable
fun SettingsScreen(
    speechRate: Float,
    pitch: Float,
    overlayAlpha: Float,
    overlaySize: Float,
    wakeWordEnabled: Boolean,
    sosNumber: String,
    quickEtaMessage: String,
    isBluetoothAutoStartEnabled: Boolean,
    selectedNavAppPackage: String,
    availableNavApps: List<NavAppItem>,
    autoOpenNavAppOnStart: Boolean,
    allInstalledApps: List<NavAppItem>,
    triggerAppPackages: Set<String>,
    onSpeechRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onOverlayAlphaChange: (Float) -> Unit,
    onOverlaySizeChange: (Float) -> Unit,
    onWakeWordToggle: (Boolean) -> Unit,
    onSosNumberChange: (String) -> Unit,
    onQuickEtaChange: (String) -> Unit,
    onBluetoothToggle: (Boolean) -> Unit,
    onNavAppSelect: (String) -> Unit,
    onAutoOpenNavToggle: (Boolean) -> Unit,
    onTriggerAppToggle: (String) -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onPlayTestVoice: (String) -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenSystemTtsSettings: () -> Unit,
    onBackClick: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Podešavanja", color = TextPrimary, fontSize = 26.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            TextButton(onClick = onBackClick) { Text("Sačuvaj") }
        }
        Text("Glas asistenta", color = AccentBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Brzina govora", color = TextPrimary)
        Slider(value = speechRate, onValueChange = onSpeechRateChange, valueRange = 0.5f..1.5f)
        Text("Visina glasa", color = TextPrimary)
        Slider(value = pitch, onValueChange = onPitchChange, valueRange = 0.5f..1.5f)
        Button(onClick = { onPlayTestVoice("NaviGlas je spreman. Poziv i poruka čekaju vašu potvrdu.") }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Poslušaj primer") }
        OutlinedButton(onClick = onOpenSystemTtsSettings, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Sistemska podešavanja glasa") }
        HorizontalDivider()
        Text("Poruke i pomoć", color = AccentBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        OutlinedTextField(value = sosNumber, onValueChange = onSosNumberChange, label = { Text("SOS primalac: broj ili puno ime") }, supportingText = { Text("SOS traži potvrdu. Nije zamena za poziv hitnim službama.") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(value = quickEtaMessage, onValueChange = onQuickEtaChange, label = { Text("Tekst brzog odgovora") }, supportingText = { Text("Primalac je poslednji pošiljalac SMS-a. Pre slanja sledi potvrda.") }, modifier = Modifier.fillMaxWidth(), minLines = 2)
        HorizontalDivider()
        Text("Navigacija", color = AccentBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        availableNavApps.forEach { app ->
            OutlinedButton(onClick = { onNavAppSelect(app.packageName) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text((if (app.packageName == selectedNavAppPackage) "✓ " else "") + app.name)
            }
        }
        Text("Prozirnost mehura preko mape", color = TextPrimary)
        Slider(value = overlayAlpha, onValueChange = onOverlayAlphaChange, valueRange = 0.3f..1f)
        OutlinedButton(onClick = onOpenAppSettings, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Dozvole aplikacije") }
        OutlinedButton(onClick = onOpenOverlaySettings, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Dozvoli mehur preko mape") }
        Text("Slušanje počinje dodirom mikrofona ili mehura. Aktivacija ključnom rečju i automatsko pokretanje preko Bluetooth-a nisu uključeni u ovu verziju.", color = TextSecondary)
        HorizontalDivider()
        Text("Android Auto", color = AccentBlue, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Dok je režim vožnje uključen, dolazne SMS poruke mogu da se pojave u Android Autu. Čitanje i glasovni odgovor vodi asistent u automobilu. Dostupnost srpskog zavisi od tog asistenta.", color = TextSecondary)
        Text("Pre polaska dozvolite prijem i slanje SMS-a i obaveštenja. Pozive prikazuje sistem automobila. NaviGlas još nema sopstvenu aktivaciju ključnom rečju preko mikrofona auta.", color = TextSecondary)
        HorizontalDivider()
        OutlinedButton(onClick = {
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("NaviGlas dijagnostika", com.example.naviglas.engine.VoiceDiagnostics.report(context)))
            android.widget.Toast.makeText(context, "Dijagnostika je kopirana. Možete je nalepiti u razgovor.", android.widget.Toast.LENGTH_LONG).show()
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Kopiraj dijagnostiku") }
        Text("Sadrži model telefona, servis za govor i tehničke greške. Ne sadrži poruke, kontakte ni vaše glasovne komande.", color = TextSecondary)
        Spacer(Modifier.height(16.dp))
    }
}
