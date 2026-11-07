package com.example.naviglas.engine

import java.util.Locale

/** Only a complete, explicit answer authorizes a pending action. */
object ConfirmationPolicy {
    private fun normalize(text: String) = SerbianTextNormalizer.stripDiacritics(text)
        .lowercase(Locale.ROOT).trim().trimEnd('.', '!', '?').trim()

    fun isAffirmative(text: String) = normalize(text) in setOf(
        "da", "potvrdi", "potvrdjujem", "posalji", "salji", "pozovi", "zovi"
    )

    fun isCancellation(text: String) = normalize(text) in setOf(
        "ne", "otkazi", "stop", "nemoj", "prekini radnju", "odustani", "ne salji", "ne zovi"
    )
}
