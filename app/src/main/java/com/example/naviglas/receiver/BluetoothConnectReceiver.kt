package com.example.naviglas.receiver
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
/** Background events never start a microphone service automatically. */
class BluetoothConnectReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { }
}

