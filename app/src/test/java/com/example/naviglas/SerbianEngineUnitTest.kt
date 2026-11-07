package com.example.naviglas

import com.example.naviglas.engine.CommandAction
import com.example.naviglas.engine.ConfidenceLevel
import com.example.naviglas.engine.Contact
import com.example.naviglas.engine.ContactMatcher
import com.example.naviglas.engine.SerbianCaseLemmatizer
import com.example.naviglas.engine.SerbianTextNormalizer
import com.example.naviglas.engine.VoiceCommandParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SerbianEngineUnitTest {

    private val contacts = listOf(
        Contact("Tijana Stanković", "064111222"),
        Contact("Verica Popović", "065222333"),
        Contact("Marko Petrović", "063333444"),
        Contact("Uroš Nikolić", "062444555")
    )

    @Test
    fun testSerbianCaseLemmatizer() {
        val tijanuLemmas = SerbianCaseLemmatizer.getCandidateLemmas("Tijanu")
        assertTrue(tijanuLemmas.contains("Tijana"))

        val vericiLemmas = SerbianCaseLemmatizer.getCandidateLemmas("Verici")
        assertTrue(vericiLemmas.contains("Verica"))

        val markaLemmas = SerbianCaseLemmatizer.getCandidateLemmas("Marka")
        assertTrue(markaLemmas.contains("Marko"))

        val uroshuLemmas = SerbianCaseLemmatizer.getCandidateLemmas("Urošu")
        assertTrue(uroshuLemmas.contains("Uroš"))
    }

    @Test
    fun testSerbianTextNormalizer() {
        val cyrillic = "Марко Петровић"
        assertEquals("Marko Petrović", SerbianTextNormalizer.cyrillicToLatin(cyrillic))

        val stripped = SerbianTextNormalizer.stripDiacritics("Čedomir Đorđević")
        assertEquals("Cedomir Djordjevic", stripped)

        val ttsText = SerbianTextNormalizer.cyrillicToLatin(
            SerbianTextNormalizer.prepareForTts("Stižem za 10 min, SMS šaljem")
        )
        assertTrue(ttsText.contains("deset minuta"))
        assertTrue(ttsText.contains("es-em-es poruka"))
    }

    @Test
    fun testContactMatcher() {
        val matchTijana = ContactMatcher.findBestMatch("Tijanu", contacts)
        assertNotNull(matchTijana.contact)
        assertEquals("Tijana Stanković", matchTijana.contact?.name)
        assertEquals(ConfidenceLevel.HIGH, matchTijana.confidence)

        val matchVerica = ContactMatcher.findBestMatch("Verici", contacts)
        assertNotNull(matchVerica.contact)
        assertEquals("Verica Popović", matchVerica.contact?.name)

        val matchMarko = ContactMatcher.findBestMatch("Marka", contacts)
        assertNotNull(matchMarko.contact)
        assertEquals("Marko Petrović", matchMarko.contact?.name)

        val vipMap = mapOf("zena" to "Tijana Stanković")
        val matchVip = ContactMatcher.findBestMatch("zena", contacts, vipMap)
        assertNotNull(matchVip.contact)
        assertEquals("Tijana Stanković", matchVip.contact?.name)
    }

    @Test
    fun testVoiceCommandParserCall() {
        val res = VoiceCommandParser.parseInput("Pozovi Tijanu", contacts)
        assertEquals(CommandAction.CALL, res.action)
        assertEquals("Tijana Stanković", res.contactName)
    }

    @Test
    fun testVoiceCommandParserSms() {
        val res = VoiceCommandParser.parseInput("Pošalji poruku Tijani da stižem za desetak minuta jer je gužva", contacts)
        assertEquals(CommandAction.SEND_SMS, res.action)
        assertEquals("Tijana Stanković", res.contactName)
        assertEquals("Stižem za desetak minuta jer je gužva.", res.messageText)
    }

    @Test
    fun testVoiceCommandParserAnswerAndReject() {
        val answerRes = VoiceCommandParser.parseInput("Javi se")
        assertEquals(CommandAction.ANSWER_CALL, answerRes.action)

        val rejectRes = VoiceCommandParser.parseInput("Odbij poziv")
        assertEquals(CommandAction.REJECT_CALL, rejectRes.action)

        val silenceRes = VoiceCommandParser.parseInput("Utišaj zvono")
        assertEquals(CommandAction.SILENCE_CALL, silenceRes.action)
    }

    @Test
    fun testVoiceCommandParserSosAndLocation() {
        val sosRes = VoiceCommandParser.parseInput("Šalji SOS")
        assertEquals(CommandAction.SEND_SOS, sosRes.action)

        val locRes = VoiceCommandParser.parseInput("Gde se nalazim")
        assertEquals(CommandAction.WHERE_AM_I, locRes.action)
    }
}
