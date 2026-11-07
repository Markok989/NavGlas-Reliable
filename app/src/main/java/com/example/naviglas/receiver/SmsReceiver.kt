package com.example.naviglas.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.naviglas.service.VoiceAssistantService

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            if (messages.isEmpty()) return
            val sender = messages.first().originatingAddress ?: "Nepoznati broj"
            val body = messages.joinToString("") { it.messageBody.orEmpty() }
            val serviceIntent = Intent(context, VoiceAssistantService::class.java).apply {
                action = VoiceAssistantService.ACTION_INCOMING_SMS
                putExtra("EXTRA_SMS_SENDER", sender)
                putExtra("EXTRA_SMS_BODY", body)
            }
            if (VoiceAssistantService.isRunning) {
                com.example.naviglas.car.CarMessaging.incoming(context, sender, body)
                try { context.startService(serviceIntent) } catch (_: IllegalStateException) { }
            }
        }
    }
}
