package com.example.naviglas.car

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.example.naviglas.MainActivity
import com.example.naviglas.R

/** Standard Android Auto MessagingStyle integration; the car host owns voice recognition. */
object CarMessaging {
    const val REPLY = "com.example.naviglas.car.REPLY"
    const val READ = "com.example.naviglas.car.READ"
    const val INPUT = "car_reply_text"
    private const val CHANNEL = "car_messages"
    private const val MESSAGE_ID = 2100
    fun incoming(context: Context, sender: String, body: String) {
        if (body.isBlank() || !canNotify(context)) return
        val message = CarMessageStore(context).add(sender) ?: return
        val notification = build(context, message, body)
        manager(context).notify(message.sender, MESSAGE_ID, notification)
    }
    fun build(context: Context, message: CarMessageStore.Message, body: String): Notification {
        channel(context)
        val replyIntent = Intent(context, CarReplyService::class.java).setAction(REPLY)
            .setData(uri(context, message.token))
        val mutable = if (Build.VERSION.SDK_INT >= 31) PendingIntent.FLAG_MUTABLE else 0
        val reply = PendingIntent.getForegroundService(context, 0, replyIntent, PendingIntent.FLAG_UPDATE_CURRENT or mutable)
        val read = PendingIntent.getService(context, 0,
            Intent(context, CarReadService::class.java).setAction(READ).setData(uri(context, message.token)),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val open = PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val sender = Person.Builder().setName(message.sender).setKey(message.sender).build()
        val style = NotificationCompat.MessagingStyle(Person.Builder().setName("Vi").build())
            .setGroupConversation(false).addMessage(body, message.receivedAt, sender)
        return NotificationCompat.Builder(context, CHANNEL)
            .addExtras(android.os.Bundle().apply { putString("car_message_token", message.token) })
            .setSmallIcon(R.drawable.ic_launcher_foreground).setStyle(style)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE).setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(open)
            .setAutoCancel(true).setOnlyAlertOnce(true)
            .addAction(NotificationCompat.Action.Builder(0, "Odgovori", reply)
                .addRemoteInput(RemoteInput.Builder(INPUT).setLabel("Odgovor").build())
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_REPLY)
                .setShowsUserInterface(false).setAllowGeneratedReplies(false).build())
            .addAction(NotificationCompat.Action.Builder(0, "Označi kao pročitano", read)
                .setSemanticAction(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ)
                .setShowsUserInterface(false).build()).build()
    }
    fun token(context: Context, intent: Intent?): String? = intent?.data?.takeIf {
        it.scheme == "navglas" && it.authority == context.packageName && it.pathSegments.size == 1
    }?.lastPathSegment
    private fun uri(context: Context, token: String) = Uri.Builder().scheme("navglas").authority(context.packageName).appendPath(token).build()
    fun dismiss(context: Context, message: CarMessageStore.Message) {
        // A late read/reply for an older message must not hide a newer notification.
        if (manager(context).activeNotifications.any {
            it.tag == message.sender && it.id == MESSAGE_ID && it.notification.extras.getString("car_message_token") == message.token
        }) manager(context).cancel(message.sender, MESSAGE_ID)
    }
    fun status(context: Context, sender: String, text: String) {
        if (!canNotify(context)) return
        channel(context)
        manager(context).notify("reply_$sender", MESSAGE_ID, NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_launcher_foreground).setContentTitle("SMS odgovor: $sender")
            .setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE).setOnlyAlertOnce(true).build())
    }
    fun progress(context: Context): Notification {
        channel(context)
        return NotificationCompat.Builder(context, CHANNEL).setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("NaviGlas šalje odgovor").setContentText("Čekam rezultat slanja SMS-a.").setOngoing(true).build()
    }
    private fun canNotify(context: Context) = Build.VERSION.SDK_INT < 33 || context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    private fun manager(context: Context) = context.getSystemService(NotificationManager::class.java)
    private fun channel(context: Context) { manager(context).createNotificationChannel(NotificationChannel(CHANNEL, "Poruke u vožnji", NotificationManager.IMPORTANCE_DEFAULT)) }
}
