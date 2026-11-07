package com.example.naviglas.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.naviglas.ui.theme.AccentGreen
import com.example.naviglas.ui.theme.DarkBackground
import com.example.naviglas.ui.theme.DarkSurface
import com.example.naviglas.ui.theme.DarkSurfaceVariant
import com.example.naviglas.ui.theme.TextPrimary
import com.example.naviglas.ui.theme.TextSecondary

@Composable
fun PermissionScreen(
    onRequestPermissions: () -> Unit,
    onOpenOverlayPermissionPage: () -> Unit,
    onOpenAppSettingsPage: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        val isLandscape = maxWidth > maxHeight
        val scrollState = rememberScrollState()

        if (isLandscape) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leva kolona: Naslov i Akciona dugmad
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aktivacija NaviGlas-a",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Za siguran rad u vožnji i čitanje SMS poruka, odobrite dozvole u 3 brza koraka:",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    PermissionActionButtons(
                        onRequestPermissions = onRequestPermissions,
                        onOpenOverlayPermissionPage = onOpenOverlayPermissionPage,
                        onOpenAppSettingsPage = onOpenAppSettingsPage
                    )
                }

                // Desna kolona: Vodič i Spisak Dozvola
                Column(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UnblockGuideCard()
                    PermissionItemsList()
                }
            }
        } else {
            // Portretni režim sa skrolovanjem
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = AccentGreen,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Aktivacija NaviGlas-a",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Za siguran rad u vožnji i čitanje SMS poruka, odobrite dozvole ispod:",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    UnblockGuideCard()
                    PermissionItemsList()
                }

                Spacer(modifier = Modifier.height(16.dp))

                PermissionActionButtons(
                    onRequestPermissions = onRequestPermissions,
                    onOpenOverlayPermissionPage = onOpenOverlayPermissionPage,
                    onOpenAppSettingsPage = onOpenAppSettingsPage
                )
            }
        }
    }
}

@Composable
private fun UnblockGuideCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "💡 Uputstvo za Odblokiranje SMS Dozvole na Android 13/14/15:",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AccentGreen
            )
            Text(
                text = "Ako je SMS ili bilo koja dozvola osenčena ili siva na vašem telefonu (Xiaomi, Samsung, Pixel):\n" +
                        "1. Kliknite na dugme '⚙️ Odblokiraj Sive Dozvole' na dnu.\n" +
                        "2. Kliknite na 3 TAČKICE (⋮) u gornjem desnom uglu telefona.\n" +
                        "3. Izaberite 'Dozvoli ograničena podešavanja' (Allow restricted settings).\n" +
                        "4. Vratite se u aplikaciju i odobrite SMS i ostale dozvole.",
                fontSize = 12.sp,
                color = TextPrimary,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun PermissionActionButtons(
    onRequestPermissions: () -> Unit,
    onOpenOverlayPermissionPage: () -> Unit,
    onOpenAppSettingsPage: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onRequestPermissions,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Sms,
                    contentDescription = null,
                    tint = DarkBackground,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "1. Omogući Dozvole (SMS, Pozivi)",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            }
        }

        OutlinedButton(
            onClick = onOpenOverlayPermissionPage,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Layers,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "2. Dozvoli Prikaz Mehura Preko Mapa",
                    fontSize = 13.sp,
                    color = TextPrimary
                )
            }
        }

        OutlinedButton(
            onClick = onOpenAppSettingsPage,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    tint = AccentGreen,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "⚙️ 3. Odblokiraj Sive Dozvole u Telefona",
                    fontSize = 13.sp,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun PermissionItemsList() {
    PermissionItem(
        icon = Icons.Default.Sms,
        title = "SMS Poruke",
        description = "Za glasovno diktiranje i čitanje poruka naglas"
    )
    PermissionItem(
        icon = Icons.Default.Mic,
        title = "Mikrofon",
        description = "Za prepoznavanje glasovnih komandi u vožnji"
    )
    PermissionItem(
        icon = Icons.Default.Contacts,
        title = "Imenik",
        description = "Za traženje imena kontakata (u padežima)"
    )
    PermissionItem(
        icon = Icons.Default.Call,
        title = "Pozivi",
        description = "Za glasovno pozivanje i odazivanje na poziv"
    )
    PermissionItem(
        icon = Icons.Default.LocationOn,
        title = "Lokacija (GPS)",
        description = "Za slanje SOS poruke i čitanje lokacije"
    )
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AccentGreen,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}
