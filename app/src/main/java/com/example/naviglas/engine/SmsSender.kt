package com.example.naviglas.engine

import android.app.Activity
import android.app.PendingIntent
import android.content.*
import android.os.*
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import java.util.UUID

/** Reports radio send results without claiming delivery. */
object SmsSender {
    fun send(context: Context, number: String, text: String, onResult: (String) -> Unit) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler(Looper.getMainLooper()).post { send(context, number, text, onResult) }
            return
        }
        try { sendOnMain(context, number, text, onResult) }
        catch (e: Exception) { onResult("SMS nije dostupan. Proverite dozvole, SIM karticu i podršku telefona.") }
    }
    private fun sendOnMain(context: Context, number: String, text: String, onResult: (String) -> Unit) {
        val app = context.applicationContext
        val manager = if (Build.VERSION.SDK_INT >= 31) app.getSystemService(SmsManager::class.java) else {
            @Suppress("DEPRECATION") SmsManager.getDefault()
        }
        if (manager.subscriptionId < 0 || manager.subscriptionId == SubscriptionManager.DEFAULT_SUBSCRIPTION_ID) {
            onResult("Izaberite podrazumevanu SIM karticu za SMS u podešavanjima telefona.")
            return
        }
        val parts = manager.divideMessage(text)
        val action = "${app.packageName}.SMS_SENT.${UUID.randomUUID()}"
        val handler = Handler(Looper.getMainLooper())
        val received = mutableSetOf<Int>()
        var registered = false
        var finished = false
        lateinit var receiver: BroadcastReceiver
        fun finish(message: String) {
            if (finished) return
            finished = true
            handler.removeCallbacksAndMessages(null)
            if (registered) { app.unregisterReceiver(receiver); registered = false }
            onResult(message)
        }
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val index = intent?.getIntExtra("part", -1) ?: return
                if (index !in parts.indices || !received.add(index)) return
                if (resultCode != Activity.RESULT_OK) {
                    finish("Slanje SMS-a nije potvrđeno (kod $resultCode). Proverite mrežu i kredit. Proverite poruke pre ponovnog slanja.")
                } else if (received.size == parts.size) {
                    finish("SMS je poslat mreži. Isporuka primaocu nije potvrđena.")
                }
            }
        }
        try {
            if (Build.VERSION.SDK_INT >= 33) app.registerReceiver(receiver, IntentFilter(action), Context.RECEIVER_NOT_EXPORTED)
            else { @Suppress("DEPRECATION") app.registerReceiver(receiver, IntentFilter(action)) }
            registered = true
            val intents = ArrayList(parts.indices.map { index ->
                PendingIntent.getBroadcast(app, index, Intent(action).setPackage(app.packageName).putExtra("part", index), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
            })
            handler.postDelayed({ finish("Nije stigla potvrda slanja SMS-a. Proverite aplikaciju za poruke pre ponovnog slanja.") }, 45000)
            manager.sendMultipartTextMessage(number, null, parts, intents, null)
            onResult("SMS je predat telefonu. Čekam potvrdu slanja.")
        } catch (e: SecurityException) { finish("Nedostaje dozvola za slanje SMS poruka.") }
        catch (e: Exception) { finish("SMS nije mogao da se pošalje. Proverite SIM karticu i mrežu.") }
    }
}

