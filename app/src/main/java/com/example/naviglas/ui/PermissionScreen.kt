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
@Composable
fun PermissionScreen(onRequestPermissions: () -> Unit, onOpenOverlayPermissionPage: () -> Unit, onOpenAppSettingsPage: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Spacer(Modifier.height(28.dp))
        Text("Dobro došli u NaviGlas", color = TextPrimary, fontSize = 30.sp, fontWeight = FontWeight.Bold)
        Text("Pozovite kontakt ili pripremite poruku svojim glasom.", color = AccentBlue, fontSize = 21.sp)
        Text("Mikrofon je potreban za glasovne komande. Kontakti, telefon i SMS omogućavaju odgovarajuće radnje. Lokacija je opciona, za komandu Gde sam i SOS poruku. Odbijanje dodatne dozvole ne blokira početni ekran.", color = TextSecondary, fontSize = 17.sp)
        Text("Slušanje počinje vašim dodirom. Pozivi i poruke traže jasnu potvrdu pre izvršavanja.", color = TextPrimary, fontSize = 18.sp)
        Button(onClick = onRequestPermissions, modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp)) { Text("Podesi dozvole") }
        OutlinedButton(onClick = onOpenAppSettingsPage, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Otvori dozvole aplikacije") }
        TextButton(onClick = onOpenOverlayPermissionPage, modifier = Modifier.fillMaxWidth()) { Text("Opciono: mehur preko navigacije") }
    }
}

