package com.example.naviglas.car

import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.RemoteInput
import com.example.naviglas.engine.SmsSender

/** A host-confirmed RemoteInput reply never opens the phone or starts its microphone. */
class CarReplyService : Service() {
    private var active = 0
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // A foreground PendingIntent must always be promoted, even for a rejected replay.
        if (Build.VERSION.SDK_INT >= 34) startForeground(2101, CarMessaging.progress(this), ServiceInfo.FOREGROUND_SERVICE_TYPE_REMOTE_MESSAGING)
        else startForeground(2101, CarMessaging.progress(this))
        val token = CarMessaging.token(this, intent)
        val text = intent?.let { RemoteInput.getResultsFromIntent(it)?.getCharSequence(CarMessaging.INPUT)?.toString()?.trim() }
        if (intent?.action != CarMessaging.REPLY || token == null || text.isNullOrBlank() || text.length > 2000) {
            finishIfIdle(); return START_NOT_STICKY
        }
        val message = CarMessageStore(this).claim(token)
        if (message == null) { finishIfIdle(); return START_NOT_STICKY }
        active++
        CarMessaging.dismiss(this, message)
        SmsSender.sendTracked(this, message.sender, text,
            { status -> CarMessaging.status(this, message.sender, status) },
            { active--; finishIfIdle() })
        return START_NOT_STICKY
    }
    private fun finishIfIdle() {
        if (active == 0) { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf() }
    }
}
