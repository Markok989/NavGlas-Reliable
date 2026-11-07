package com.example.naviglas.car

import android.app.IntentService
import android.content.Intent

/** Marks only NaviGlas's notification as read; does not modify the system SMS database. */
@Suppress("DEPRECATION")
class CarReadService : IntentService("CarReadService") {
    override fun onHandleIntent(intent: Intent?) {
        if (intent?.action != CarMessaging.READ) return
        val token = CarMessaging.token(this, intent) ?: return
        val message = CarMessageStore(this).find(token) ?: return
        CarMessaging.dismiss(this, message)
    }
}
