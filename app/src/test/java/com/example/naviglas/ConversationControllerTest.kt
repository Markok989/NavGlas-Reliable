package com.example.naviglas

import com.example.naviglas.engine.*
import org.junit.Assert.*
import org.junit.Test

class ConversationControllerTest {
    private fun controller() = ConversationController().apply { contacts = listOf(
        Contact("Marko Petrović", "061111111"), Contact("Marko Nikolić", "062222222"),
        Contact("Ana", "063333333", listOf("063333333", "064444444", "065555555"))
    ) }
    @Test fun ambiguousRecipientNeedsExplicitChoice() {
        val c = controller()
        assertNull(c.accept("Pozovi Marka").command)
        assertTrue(c.awaitingInput)
        assertNull(c.accept("ne znam").command)
        assertNull(c.accept("drugi").command)
        val result = c.accept("potvrdi").command!!
        assertEquals("062222222", result.phoneNumber)
        assertFalse(c.awaitingInput)
    }
    @Test fun messageSurvivesContactChoice() {
        val c = controller()
        c.accept("Pošalji poruku Marku da stižem")
        c.accept("drugi")
        val result = c.accept("da").command!!
        assertEquals("Stižem.", result.messageText)
        assertEquals("062222222", result.phoneNumber)
    }
    @Test fun thirdPhoneNumberIsPreserved() {
        val c = controller()
        c.accept("Pozovi Anu")
        c.accept("treći")
        assertEquals("065555555", c.accept("potvrdi").command!!.phoneNumber)
    }
    @Test fun cancelDuringMessageDictationDoesNotSend() {
        val c = controller()
        c.accept("Pošalji poruku Ani")
        c.accept("prvi")
        assertNull(c.accept("otkaži").command)
        assertFalse(c.awaitingInput)
        assertNull(c.accept("da").command)
    }
    @Test fun negativeAndAmbiguousAnswersNeverConfirm() {
        val c = controller()
        c.accept("Pozovi Marka Petrovića")
        assertNull(c.accept("kada").command)
        assertNull(c.accept("nemoj da zoveš").command)
        assertNull(c.accept("ne").command)
        assertFalse(c.awaitingInput)
    }
    @Test fun missingContactDoesNotExecute() {
        val c = controller()
        assertNull(c.accept("Pozovi nepostojeći kontakt").command)
        assertFalse(c.awaitingInput)
    }
    @Test fun smsBodyDoesNotTriggerUnrelatedCommand() {
        val result = VoiceCommandParser.parseInput("Pošalji poruku Ani da javi se kada stigneš", controller().contacts)
        assertEquals(CommandAction.SEND_SMS, result.action)
    }
    @Test fun cyrillicCommandWorks() {
        assertEquals(CommandAction.CALL, VoiceCommandParser.parseInput("Позови Ану", controller().contacts).action)
    }
    @Test fun editPreservesSelectedNumber() {
        val c = controller()
        c.accept("Pošalji poruku Ani da stižem")
        c.accept("drugi"); c.accept("promeni"); c.accept("Kasnim pet minuta")
        val command = c.accept("potvrdi").command!!
        assertEquals("064444444", command.phoneNumber)
        assertEquals("Kasnim pet minuta", command.messageText)
    }
    @Test fun sameNameDifferentPeopleRemainDistinct() {
        val c = ConversationController().apply { contacts = listOf(Contact("Marko", "061111111"), Contact("Marko", "062222222")) }
        c.accept("Pozovi Marka")
        assertNull(c.accept("Marko").command)
        c.accept("drugi")
        assertEquals("062222222", c.accept("da").command!!.phoneNumber)
    }
    @Test fun sosNeedsRecipientAndConfirmation() {
        val c = controller()
        assertNull(c.accept("Šalji SOS").command)
        assertFalse(c.awaitingInput)
        c.sosRecipient = "+381641234567"
        assertNull(c.accept("Šalji SOS").command)
        assertEquals("+381641234567", c.accept("potvrdi").command!!.phoneNumber)
    }
    @Test fun shortcutNeedsConfirmation() {
        val c = controller()
        assertNull(c.submit(ParsedCommandResult(CommandAction.SEND_SMS, phoneNumber = "0641234567", messageText = "Stižem", spokenFeedback = "")).command)
        assertEquals(CommandAction.SEND_SMS, c.accept("potvrdi").command!!.action)
    }
    @Test fun unsafeNumbersRejected() {
        listOf("*123#", "123;456", "tel:12345", "+381;123", "Ana").forEach { assertNull(PhoneNumbers.normalize(it)) }
        assertEquals("+381641234567", PhoneNumbers.normalize("+381 (64) 123-4567"))
    }
}

