package com.example.naviglas.engine

import java.util.Locale

data class ConversationReply(val text: String, val command: ParsedCommandResult? = null)

/** Pure conversation logic. No microphone, Android lifecycle, or side effects. */
class ConversationController {
    var contacts: List<Contact> = emptyList()
    var sosRecipient: String = ""
    private var pending: ParsedCommandResult? = null
    private var choices: List<Contact> = emptyList()
    private var numberContact: Contact? = null
    private var needsMessage = false
    val awaitingInput: Boolean get() = pending != null
    val awaitingConfirmation: Boolean get() = pending != null && choices.isEmpty() && numberContact == null && !needsMessage

    fun cancel() { pending = null; choices = emptyList(); numberContact = null; needsMessage = false }
    private fun norm(text: String) = SerbianTextNormalizer.stripDiacritics(text).lowercase(Locale.ROOT).trim().trimEnd('.', '!', '?')
    private fun ordinal(text: String): Int? = listOf(
        setOf("prvi", "prva", "jedan", "1"), setOf("drugi", "druga", "dva", "2"),
        setOf("treci", "treca", "tri", "3"), setOf("cetvrti", "cetiri", "4"),
        setOf("peti", "pet", "5"), setOf("sesti", "sest", "6")
    ).indexOfFirst { norm(text) in it }.takeIf { it >= 0 }

    fun accept(input: String): ConversationReply {
        if (ConfirmationPolicy.isCancellation(input)) { cancel(); return ConversationReply("Radnja je otkazana.") }
        if (input.isBlank()) return ConversationReply("Nisam čuo odgovor. Ponovite ili recite otkaži.")
        if (choices.isNotEmpty()) {
            val selected = ordinal(input)?.let { choices.getOrNull(it) }
                ?: choices.filter { norm(it.name) == norm(input) }.singleOrNull()
            if (selected == null) return ConversationReply(choicePrompt())
            choices = emptyList()
            return selectContact(selected)
        }
        numberContact?.let { contact ->
            val number = ordinal(input)?.let { contact.phoneNumbers.getOrNull(it) }
            if (number == null) return ConversationReply(numberPrompt(contact))
            numberContact = null
            return prepare(pending!!.copy(contactName = contact.name, phoneNumber = number))
        }
        if (needsMessage) {
            needsMessage = false
            return prepare(pending!!.copy(messageText = input.trim()))
        }
        pending?.let { action ->
            if (ConfirmationPolicy.isAffirmative(input)) {
                cancel()
                return ConversationReply("", action)
            }
            if (norm(input) in setOf("ponovi", "procitaj", "procitaj poruku")) return ConversationReply(confirmationPrompt(action))
            if (action.action == CommandAction.SEND_SMS && norm(input) in setOf("promeni", "izmeni", "nova poruka")) {
                needsMessage = true
                return ConversationReply("Izgovorite novi tekst poruke za ${action.contactName}.")
            }
            return ConversationReply("Recite potvrdi za izvršavanje ili otkaži. " + confirmationPrompt(action))
        }
        return submit(VoiceCommandParser.parseInput(input, contacts))
    }

    fun submit(command: ParsedCommandResult): ConversationReply {
        cancel()
        if (command.action == CommandAction.CANCEL) return ConversationReply("Radnja je otkazana.")
        if (command.action !in setOf(CommandAction.CALL, CommandAction.SEND_SMS, CommandAction.SEND_SOS)) {
            return if (command.action == CommandAction.UNKNOWN) ConversationReply(command.spokenFeedback)
            else ConversationReply("", command)
        }
        pending = command
        val query = command.recipientQuery ?: command.contactName ?: if (command.action == CommandAction.SEND_SOS) sosRecipient else ""
        if (command.phoneNumber != null) {
            val number = PhoneNumbers.normalize(command.phoneNumber)
            if (number == null) { cancel(); return ConversationReply("Broj telefona nije ispravan.") }
            return prepare(command.copy(phoneNumber = number))
        }
        PhoneNumbers.normalize(query)?.let { return prepare(command.copy(contactName = query, phoneNumber = it)) }
        if (query.isBlank()) { cancel(); return ConversationReply("Prvo izaberite primaoca u podešavanjima.") }
        val matches = ContactMatcher.findAllMatches(query, contacts)
        if (matches.size > 1) { choices = matches; return ConversationReply(choicePrompt()) }
        val match = matches.singleOrNull() ?: ContactMatcher.findBestMatch(query, contacts).contact
        if (match == null) { cancel(); return ConversationReply("Kontakt $query nije pronađen. Ponovite puno ime.") }
        return selectContact(match)
    }

    private fun selectContact(contact: Contact): ConversationReply {
        pending = pending!!.copy(contactName = contact.name, phoneNumber = contact.phoneNumber)
        if (contact.phoneNumbers.size > 1) {
            numberContact = contact
            return ConversationReply(numberPrompt(contact))
        }
        return prepare(pending!!)
    }
    private fun prepare(command: ParsedCommandResult): ConversationReply {
        if (PhoneNumbers.normalize(command.phoneNumber.orEmpty()) == null) { cancel(); return ConversationReply("Kontakt nema ispravan broj telefona.") }
        pending = command
        if (command.action == CommandAction.SEND_SMS && command.messageText.isNullOrBlank()) {
            needsMessage = true
            return ConversationReply("Koju poruku želite da pošaljete za ${command.contactName}?")
        }
        return ConversationReply(confirmationPrompt(command))
    }
    private fun confirmationPrompt(command: ParsedCommandResult): String = when (command.action) {
        CommandAction.CALL -> "Poziv za ${command.contactName}, broj ${command.phoneNumber}. Recite potvrdi ili otkaži."
        CommandAction.SEND_SOS -> "SOS poruka za ${command.contactName}. Sadržaće poslednju dostupnu lokaciju, ako postoji. Recite potvrdi ili otkaži."
        else -> "Poruka za ${command.contactName}, broj ${command.phoneNumber}: ${command.messageText}. Recite potvrdi, promeni ili otkaži."
    }
    private fun choicePrompt() = "Izaberite kontakt: " + choices.mapIndexed { i, c -> "${i + 1}. ${c.name}" }.joinToString("; ") + ". Recite puno ime ili redni broj."
    private fun numberPrompt(contact: Contact) = "Kontakt ${contact.name} ima više brojeva: " + contact.phoneNumbers.mapIndexed { i, n -> "${i + 1}. $n" }.joinToString("; ") + ". Izaberite redni broj ili otkaži."
}

object PhoneNumbers {
    fun normalize(raw: String): String? {
        val clean = raw.trim().replace(Regex("[ ()-]"), "")
        return clean.takeIf { Regex("\\+?[0-9]{3,15}").matches(it) }
    }
}

