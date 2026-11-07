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
            for (sms in messages) {
                val sender = sms.originatingAddress ?: "Nepoznati broj"
                val body = sms.messageBody ?: ""

                val serviceIntent = Intent(context, VoiceAssistantService::class.java).apply {
                    action = VoiceAssistantService.ACTION_INCOMING_SMS
                    putExtra("EXTRA_SMS_SENDER", sender)
                    putExtra("EXTRA_SMS_BODY", body)
                }
                try {
                    ContextCompat.startForegroundService(context, serviceIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
