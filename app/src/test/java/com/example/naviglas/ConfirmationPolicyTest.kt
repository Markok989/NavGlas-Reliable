package com.example.naviglas

import com.example.naviglas.engine.ConfirmationPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConfirmationPolicyTest {
    @Test fun acceptsOnlyExplicitConfirmation() {
        listOf("Da", "potvrdi", "Pošalji!").forEach { assertTrue(ConfirmationPolicy.isAffirmative(it)) }
        listOf("ne šalji", "nemoj da zoveš", "kada", "da li", "ne potvrdi", "").forEach {
            assertFalse(ConfirmationPolicy.isAffirmative(it))
        }
    }
    @Test fun cancellationIsRecognized() {
        listOf("Otkaži", "ne", "ne šalji", "odustani").forEach { assertTrue(ConfirmationPolicy.isCancellation(it)) }
        assertFalse(ConfirmationPolicy.isCancellation("potvrdi"))
    }
}
