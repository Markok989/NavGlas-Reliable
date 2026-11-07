package com.example.naviglas
import com.example.naviglas.engine.*
import org.junit.Assert.*
import org.junit.Test

class SmsSendTrackerTest {
    @Test fun everyPartMustSucceedInAnyOrder() {
        for (count in 1..8) {
            val tracker = SmsSendTracker(count)
            val order = (0 until count).reversed().toList()
            order.forEachIndexed { i, part ->
                assertEquals(if (i == count - 1) SmsSendOutcome.SENT else null, tracker.record(part, true))
            }
            assertNull(tracker.timeout())
        }
    }
    @Test fun oneFailedPartNeverReportsSuccess() {
        for (failure in 0..4) {
            val tracker = SmsSendTracker(5)
            for (part in 0..4) {
                val result = tracker.record(part, part != failure)
                assertEquals(if (part == failure) SmsSendOutcome.FAILED else null, result)
            }
        }
    }
    @Test fun duplicateAndInvalidReceiptsCannotCompleteMessage() {
        val tracker = SmsSendTracker(2)
        assertNull(tracker.record(-1, true)); assertNull(tracker.record(2, true))
        assertNull(tracker.record(0, true)); assertNull(tracker.record(0, true))
        assertEquals(SmsSendOutcome.SENT, tracker.record(1, true))
        assertNull(tracker.record(1, false))
    }
    @Test fun timeoutRemainsUnknownEvenAfterLateSuccess() {
        val tracker = SmsSendTracker(2)
        assertNull(tracker.record(0, true))
        assertEquals(SmsSendOutcome.UNKNOWN, tracker.timeout())
        assertNull(tracker.record(1, true)); assertNull(tracker.timeout())
    }
}

