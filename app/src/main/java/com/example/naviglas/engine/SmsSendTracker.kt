package com.example.naviglas.engine

enum class SmsSendOutcome { SENT, FAILED, UNKNOWN }

/** A multipart SMS succeeds only after every distinct part has succeeded. */
class SmsSendTracker(private val partCount: Int) {
    init { require(partCount > 0) }
    private val received = mutableSetOf<Int>()
    private var finished = false
    fun record(part: Int, success: Boolean): SmsSendOutcome? {
        if (finished || part !in 0 until partCount || !received.add(part)) return null
        val outcome = when {
            !success -> SmsSendOutcome.FAILED
            received.size == partCount -> SmsSendOutcome.SENT
            else -> null
        }
        if (outcome != null) finished = true
        return outcome
    }
    fun timeout(): SmsSendOutcome? {
        if (finished) return null
        finished = true
        return SmsSendOutcome.UNKNOWN
    }
}

