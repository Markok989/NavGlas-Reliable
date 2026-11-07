package com.example.naviglas.engine

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Geocoder
import android.location.LocationManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.telecom.TelecomManager
import android.telephony.SmsManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ActionExecutor {

    private fun getSmsManager(context: Context): SmsManager {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getBestLastKnownLocation(lm: LocationManager): android.location.Location? {
        val providers = lm.getProviders(true)
        var bestLocation: android.location.Location? = null
        for (provider in providers) {
            val l = try { lm.getLastKnownLocation(provider) } catch (e: Exception) { null } ?: continue
            if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                bestLocation = l
            }
        }
        return bestLocation
    }

    /**
     * Automatski vraća prikaz na aktivnu navigacionu aplikaciju (Google Maps, OsmAnd, Waze itd.) nakon završetka akcije.
     */
    fun returnToActiveNavApp(context: Context) {
        val prefs = context.getSharedPreferences("NaviGlasPrefs", Context.MODE_PRIVATE)
        val lastNav = prefs.getString("LAST_ACTIVE_NAV_PACKAGE", null)
        if (!lastNav.isNullOrBlank()) {
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(lastNav)
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    context.startActivity(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Izvršava determinističku sistemsku akciju iz prepoznate komande.
     */
    @SuppressLint("MissingPermission")
    fun execute(
        context: Context,
        command: ParsedCommandResult,
        contacts: List<Contact>,
        onResult: (String) -> Unit
    ) {
        val contact = command.contactName?.let { name ->
            contacts.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }

        when (command.action) {
            CommandAction.CALL -> {
                val phoneNumber = command.phoneNumber ?: contact?.phoneNumber
                if (!phoneNumber.isNullOrBlank()) {
                    val intent = Intent(Intent.ACTION_CALL).apply {
                        data = Uri.parse("tel:$phoneNumber")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        context.startActivity(intent)
                        onResult("Pozivam ${contact?.name ?: phoneNumber}.")
                        returnToActiveNavApp(context)
                    } catch (e: Exception) {
                        onResult("Greška pri pokretanju poziva.")
                    }
                } else {
                    onResult("Broj telefona nije pronađen.")
                }
            }

            CommandAction.SEND_SMS -> {
                val phoneNumber = command.phoneNumber ?: contact?.phoneNumber
                val messageText = command.messageText

                if (!phoneNumber.isNullOrBlank() && !messageText.isNullOrBlank()) {
                    try {
                        val smsManager = getSmsManager(context)
                        smsManager.sendTextMessage(phoneNumber, null, messageText, null, null)
                        onResult("Poruka predata telefonu za slanje za ${contact?.name ?: phoneNumber}.")
                        returnToActiveNavApp(context)
                    } catch (e: Exception) {
                        onResult("Greška pri slanju SMS poruke.")
                    }
                } else {
                    onResult("Nedostaje primalac ili tekst poruke.")
                }
            }

            CommandAction.ANSWER_CALL -> {
                try {
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        telecomManager.acceptRingingCall()
                        onResult("Poziv je prihvaćen.")
                    } else {
                        onResult("Prihvatanje poziva nije podržano na ovoj verziji Androida.")
                    }
                    returnToActiveNavApp(context)
                } catch (e: Exception) {
                    onResult("Greška pri javljanju na poziv.")
                }
            }

            CommandAction.REJECT_CALL -> {
                try {
                    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        telecomManager.endCall()
                        onResult("Poziv je odbijen.")
                    } else {
                        onResult("Odbijanje poziva nije podržano na ovoj verziji Androida.")
                    }
                    returnToActiveNavApp(context)
                } catch (e: Exception) {
                    onResult("Greška pri odbijanju poziva.")
                }
            }

            CommandAction.SILENCE_CALL -> {
                try {
                    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                    onResult("Zvono je utišano.")
                } catch (e: Exception) {
                    onResult("Greška pri utišavanju zvona.")
                }
            }

            CommandAction.SEND_SOS -> {
                try {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val loc = getBestLastKnownLocation(lm)

                    val sosText = if (loc != null) {
                        "Hitno! Potrebna pomoć na putu. Moja lokacija: https://maps.google.com/?q=${loc.latitude},${loc.longitude}"
                    } else {
                        "Hitno! Potrebna pomoć na putu!"
                    }

                    val sosContactNumber = contact?.phoneNumber
                    if (sosContactNumber != null) {
                        val smsManager = getSmsManager(context)
                        smsManager.sendTextMessage(sosContactNumber, null, sosText, null, null)
                        onResult("SOS poruka predata telefonu za slanje.")
                        returnToActiveNavApp(context)
                    } else {
                        onResult("Nije definisan SOS kontakt.")
                    }
                } catch (e: Exception) {
                    onResult("Greška pri slanju SOS lokacije.")
                }
            }

            CommandAction.WHERE_AM_I -> {
                try {
                    val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                    val loc = getBestLastKnownLocation(lm)

                    if (loc != null && Geocoder.isPresent()) {
                        val geocoder = Geocoder(context, Locale("sr", "RS"))
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        val address = addresses?.firstOrNull()
                        if (address != null) {
                            val locality = address.locality ?: address.subAdminArea ?: ""
                            val thoroughfare = address.thoroughfare ?: ""
                            val resText = if (locality.isNotBlank() || thoroughfare.isNotBlank()) {
                                "Nalazite se u mestu $locality $thoroughfare".trim()
                            } else {
                                "Nalazite se na koordinatama: ${String.format(Locale.ROOT, "%.4f, %.4f", loc.latitude, loc.longitude)}."
                            }
                            onResult(resText)
                        } else {
                            onResult("Nalazite se na koordinatama: ${String.format(Locale.ROOT, "%.4f, %.4f", loc.latitude, loc.longitude)}.")
                        }
                    } else if (loc != null) {
                        onResult("Nalazite se na koordinatama: ${String.format(Locale.ROOT, "%.4f, %.4f", loc.latitude, loc.longitude)}.")
                    } else {
                        onResult("Trenutna lokacija nije dostupna.")
                    }
                    returnToActiveNavApp(context)
                } catch (e: Exception) {
                    onResult("Ne mogu da odredim lokaciju.")
                }
            }

            CommandAction.WHAT_TIME -> {
                val timeFormat = SimpleDateFormat("HH 'sati i' mm 'minuta'", Locale("sr", "RS"))
                val currentTime = timeFormat.format(Date())
                onResult("Sada je $currentTime.")
                returnToActiveNavApp(context)
            }

            else -> {
                onResult("Akcija nije prepoznata.")
            }
        }
    }
}
