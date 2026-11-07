package com.example.naviglas.car

import android.content.Context
import com.example.naviglas.engine.PhoneNumbers
import org.json.JSONObject
import java.util.UUID

/** Private reply capabilities expire after 24 hours. Message bodies are not persisted. */
class CarMessageStore(context: Context) {
    private val prefs = context.getSharedPreferences("car_reply_capabilities", Context.MODE_PRIVATE)
    data class Message(val token: String, val sender: String, val receivedAt: Long)
    fun add(sender: String, now: Long = System.currentTimeMillis()): Message? = synchronized(lock) {
        val number = PhoneNumbers.normalize(sender) ?: return null
        val entries = prefs.all.mapNotNull { (key, value) -> decode(key, value as? String) }
        val edit = prefs.edit()
        entries.filter { now - it.receivedAt > TTL || it.receivedAt > now }.forEach { edit.remove(it.token) }
        entries.sortedByDescending { it.receivedAt }.drop(49).forEach { edit.remove(it.token) }
        val message = Message(UUID.randomUUID().toString(), number, now)
        edit.putString(message.token, JSONObject().put("sender", number).put("time", now).put("used", false).toString())
        if (edit.commit()) message else null
    }
    fun find(token: String, now: Long = System.currentTimeMillis()): Message? = synchronized(lock) {
        decode(token, prefs.getString(token, null))?.takeIf { now - it.receivedAt in 0..TTL }
    }
    /** Consume before submitting: a replay must never send the same reply twice. */
    fun claim(token: String, now: Long = System.currentTimeMillis()): Message? = synchronized(lock) {
        val message = find(token, now) ?: return null
        val json = JSONObject(prefs.getString(token, null) ?: return null)
        if (json.optBoolean("used", true)) return null
        if (!prefs.edit().putString(token, json.put("used", true).toString()).commit()) return null
        message
    }
    private fun decode(token: String, value: String?): Message? = try {
        value?.let { val json = JSONObject(it); Message(token, json.getString("sender"), json.getLong("time")) }
    } catch (_: Exception) { null }
    companion object { private val lock = Any(); const val TTL = 24 * 60 * 60 * 1000L }
}
