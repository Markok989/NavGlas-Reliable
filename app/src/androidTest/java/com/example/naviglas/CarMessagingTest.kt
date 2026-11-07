package com.example.naviglas

import android.app.Notification
import android.app.NotificationManager
import android.content.ComponentName
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.test.platform.app.InstrumentationRegistry
import com.example.naviglas.car.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.Rule
import androidx.test.ext.junit.rules.ActivityScenarioRule

class CarMessagingTest {
    @get:Rule val activity = ActivityScenarioRule(MainActivity::class.java)
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun replyCapabilitiesPersistAndAreConsumedOnce() {
        val store = CarMessageStore(context)
        val message = store.add("+38161111222")!!
        assertEquals(message, CarMessageStore(context).find(message.token))
        assertEquals(message, store.claim(message.token))
        assertNull(CarMessageStore(context).claim(message.token))
    }
    @Test fun expiredUnknownAndNonNumericSendersCannotBeUsed() {
        val store = CarMessageStore(context)
        assertNull(store.add("Banka"))
        assertNull(store.claim("unknown"))
        val message = store.add("061222333", 1000)!!
        assertNull(store.claim(message.token, 1001 + CarMessageStore.TTL))
        assertNull(store.claim(message.token, 999))
    }
    @Test fun notificationHasHostReplyAndReadActionsWithoutOpeningPhone() {
        val message = CarMessageStore(context).add("061222333")!!
        val notification = CarMessaging.build(context, message, "Stižem za deset minuta.")
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(notification)!!
        assertEquals("Stižem za deset minuta.", style.messages.single().text.toString())
        assertEquals(message.sender, style.messages.single().person!!.name.toString())
        val reply = requireNotNull(NotificationCompat.getAction(notification, 0))
        assertEquals(NotificationCompat.Action.SEMANTIC_ACTION_REPLY, reply.semanticAction)
        assertFalse(reply.showsUserInterface)
        assertEquals(CarMessaging.INPUT, reply.remoteInputs!!.single().resultKey)
        val read = requireNotNull(NotificationCompat.getAction(notification, 1))
        assertEquals(NotificationCompat.Action.SEMANTIC_ACTION_MARK_AS_READ, read.semanticAction)
        assertFalse(read.showsUserInterface)
        assertEquals(Notification.VISIBILITY_PRIVATE, notification.visibility)
        if (Build.VERSION.SDK_INT >= 31) {
            assertFalse(requireNotNull(reply.actionIntent).isImmutable)
            assertTrue(requireNotNull(read.actionIntent).isImmutable)
        }
    }
    @Test fun readDoesNotConsumeReplyAndCannotHideNewerMessage() {
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS").use {
                java.io.FileInputStream(it.fileDescriptor).use { stream -> stream.readBytes() }
            }
        }
        val store = CarMessageStore(context)
        val old = store.add("061222333")!!
        val newer = store.add("061222333")!!
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(newer.sender, 2100, CarMessaging.build(context, newer, "Novija poruka"))
        waitFor { manager.activeNotifications.any { it.tag == newer.sender } }
        CarMessaging.dismiss(context, old)
        assertTrue(manager.activeNotifications.any { it.tag == newer.sender })
        CarMessaging.dismiss(context, newer)
        waitFor { manager.activeNotifications.none { it.tag == newer.sender } }
        assertFalse(manager.activeNotifications.any { it.tag == newer.sender })
        assertEquals(newer, store.claim(newer.token))
    }
    @Test fun actionServicesAreNotExported() {
        for (type in listOf(CarReplyService::class.java, CarReadService::class.java)) {
            @Suppress("DEPRECATION")
            val info = context.packageManager.getServiceInfo(ComponentName(context, type), 0)
            assertFalse(info.exported)
        }
    }
    @Test fun readPendingIntentCannotBeRedirectedToAnotherSender() {
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS").use {
                java.io.FileInputStream(it.fileDescriptor).use { stream -> stream.readBytes() }
            }
        }
        val store = CarMessageStore(context)
        val first = store.add("061222333")!!
        val other = store.add("062333444")!!
        val firstNotification = CarMessaging.build(context, first, "Prva poruka")
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.notify(first.sender, 2100, firstNotification)
        manager.notify(other.sender, 2100, CarMessaging.build(context, other, "Druga poruka"))
        waitFor { manager.activeNotifications.count { it.id == 2100 && it.tag in listOf(first.sender, other.sender) } == 2 }
        val fillIn = android.content.Intent().setData(android.net.Uri.parse("navglas://${context.packageName}/${other.token}"))
        requireNotNull(NotificationCompat.getAction(firstNotification, 1)?.actionIntent).send(context, 0, fillIn)
        waitFor { manager.activeNotifications.none { it.id == 2100 && it.tag == first.sender } }
        assertTrue(manager.activeNotifications.any { it.id == 2100 && it.tag == other.sender })
        assertEquals(first, store.claim(first.token))
        CarMessaging.dismiss(context, other)
    }
    private fun waitFor(condition: () -> Boolean) {
        val deadline = android.os.SystemClock.elapsedRealtime() + 3000
        while (!condition() && android.os.SystemClock.elapsedRealtime() < deadline) android.os.SystemClock.sleep(30)
        assertTrue("Notification state did not settle", condition())
    }
}
