package com.example.naviglas.engine

import java.util.Locale

/**
 * Lematizator za padeže u srpskom jeziku (ekavica).
 * Prepoznaje deklinisane oblike imena u akuzativu, dativu, genitivu, lokativu i instrumentalima
 * i vraća listu najverovatnijih nominativnih kandidata (lemata).
 */
object SerbianCaseLemmatizer {

    /**
     * Generiše sve potencijalne oblike imena u nominativu za dati izgovoreni deklinisani oblik.
     * Npr. "Tijanu" -> ["Tijana", "Tijan", "Tijanu"]
     *      "Verici" -> ["Verica", "Verici"]
     *      "Marka"  -> ["Marko", "Marka", "Mark"]
     *      "Urošu"  -> ["Uroš", "Urošu"]
     */
    fun getCandidateLemmas(spokenName: String): List<String> {
        val trimmed = spokenName.trim()
        if (trimmed.isEmpty()) return emptyList()

        val result = mutableSetOf<String>()
        val lowercase = trimmed.lowercase(Locale.ROOT)

        // Dodajemo i sam izgovoreni oblik
        result.add(trimmed)

        // Pravila za srpska ženska i muška imena sa sufiksima
        when {
            // "u" -> -a (npr. Tijanu -> Tijana, Milicu -> Milica, Nikolu -> Nikola)
            lowercase.endsWith("u") && lowercase.length > 2 -> {
                val stem = trimmed.substring(0, trimmed.length - 1)
                result.add("${stem}a")
                result.add(stem) // npr. Urošu -> Uroš, Milošu -> Miloš
            }

            // "i" -> -a (npr. Tijani -> Tijana, Verici -> Verica [c->ca])
            lowercase.endsWith("i") && lowercase.length > 2 -> {
                val stem = trimmed.substring(0, trimmed.length - 1)
                result.add("${stem}a")
                // Sibilarizacija: -ci -> -ca (Verici -> Verica, Milici -> Milica)
                if (lowercase.endsWith("ci")) {
                    val cStem = trimmed.substring(0, trimmed.length - 2)
                    result.add("${cStem}ca")
                }
                // Sibilarizacija: -zi -> -ga (Dragi -> Draga)
                if (lowercase.endsWith("zi")) {
                    val zStem = trimmed.substring(0, trimmed.length - 2)
                    result.add("${zStem}ga")
                }
            }

            // "e" -> -a (npr. Milice -> Milica, Tijane -> Tijana)
            lowercase.endsWith("e") && lowercase.length > 2 -> {
                val stem = trimmed.substring(0, trimmed.length - 1)
                result.add("${stem}a")
            }

            // "a" -> -o ili bez nastavka (npr. Marka -> Marko / Mark, Slobodana -> Slobodan, Ivana -> Ivan)
            lowercase.endsWith("a") && lowercase.length > 2 -> {
                val stem = trimmed.substring(0, trimmed.length - 1)
                result.add("${stem}o")
                result.add(stem)
            }

            // "om" / "em" -> -o / -a / suglasnik (npr. Markom -> Marko, Urošem -> Uroš)
            lowercase.endsWith("om") && lowercase.length > 3 -> {
                val stem = trimmed.substring(0, trimmed.length - 2)
                result.add("${stem}o")
                result.add("${stem}a")
                result.add(stem)
            }
            lowercase.endsWith("em") && lowercase.length > 3 -> {
                val stem = trimmed.substring(0, trimmed.length - 2)
                result.add(stem)
                result.add("${stem}o")
            }

            // "eta" / "ea" -> -e (npr. Mareta -> Mare, Marea -> Mare)
            lowercase.endsWith("eta") && lowercase.length > 4 -> {
                val stem = trimmed.substring(0, trimmed.length - 3)
                result.add("${stem}e")
                result.add("${stem}o")
            }
        }

        return result.toList()
    }
}
