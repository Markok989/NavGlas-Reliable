package com.example.naviglas.engine

/**
 * Normalizator teksta za srpski jezik (ekavica).
 * Pruža konverziju pisma (ćirilica -> latinica i latinica -> ćirilica),
 * uklanjanje dijakritika radi mečovanja, i širenje skraćenica/brojeva u izgovorne reči.
 */
object SerbianTextNormalizer {

    private val cyrillicToLatinMap = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d",
        'ђ' to "đ", 'е' to "e", 'ж' to "ž", 'з' to "z", 'и' to "i",
        'ј' to "j", 'к' to "k", 'л' to "l", 'љ' to "lj", 'м' to "m",
        'н' to "n", 'њ' to "nj", 'о' to "o", 'п' to "p", 'р' to "r",
        'с' to "s", 'т' to "t", 'ћ' to "ć", 'у' to "u", 'ф' to "f",
        'х' to "h", 'ц' to "c", 'ч' to "č", 'џ' to "dž", 'ш' to "š",
        'А' to "A", 'Б' to "B", 'В' to "V", 'Г' to "G", 'Д' to "D",
        'Ђ' to "Đ", 'Е' to "E", 'Ж' to "Ž", 'З' to "Z", 'И' to "I",
        'Ј' to "J", 'К' to "K", 'Л' to "L", 'Љ' to "Lj", 'М' to "M",
        'Н' to "N", 'Њ' to "Nj", 'О' to "O", 'П' to "P", 'Р' to "R",
        'С' to "S", 'Т' to "T", 'Ћ' to "Ć", 'У' to "U", 'Ф' to "F",
        'Х' to "H", 'Ц' to "C", 'Ч' to "Č", 'Џ' to "Dž", 'Ш' to "Š"
    )

    private val numberWords = mapOf(
        0 to "nula", 1 to "jedan", 2 to "dva", 3 to "tri", 4 to "četiri", 5 to "pet",
        6 to "šest", 7 to "sedam", 8 to "osam", 9 to "devet", 10 to "deset",
        15 to "petnaest", 20 to "dvadeset", 30 to "trideset", 45 to "četrdeset pet"
    )

    /**
     * Konvertuje ćirilični tekst u latinicu.
     */
    fun cyrillicToLatin(text: String): String {
        val sb = StringBuilder()
        for (char in text) {
            sb.append(cyrillicToLatinMap[char] ?: char)
        }
        return sb.toString()
    }

    /**
     * Konvertuje latinicu u ćirilicu za maksimalnu fonetsku prirodnost u Google TTS sintetičaru.
     */
    fun latinToCyrillic(text: String): String {
        val t = text
            .replace("lj", "љ").replace("Lj", "Љ").replace("LJ", "Љ")
            .replace("nj", "њ").replace("Nj", "Њ").replace("NJ", "Њ")
            .replace("dž", "џ").replace("Dž", "Џ").replace("DŽ", "Џ")
            .replace("dj", "ђ").replace("Dj", "Ђ").replace("DJ", "Ђ")

        val result = StringBuilder()
        for (ch in t) {
            val converted = when (ch) {
                'a' -> 'а'; 'b' -> 'б'; 'v' -> 'в'; 'g' -> 'г'; 'd' -> 'д'; 'đ' -> 'ђ'
                'e' -> 'е'; 'ž' -> 'ж'; 'z' -> 'з'; 'i' -> 'и'; 'j' -> 'ј'; 'k' -> 'к'
                'l' -> 'л'; 'm' -> 'м'; 'n' -> 'н'; 'o' -> 'о'; 'p' -> 'п'; 'r' -> 'р'
                's' -> 'с'; 't' -> 'т'; 'ć' -> 'ћ'; 'u' -> 'у'; 'f' -> 'ф'; 'h' -> 'х'
                'c' -> 'ц'; 'č' -> 'ч'; 'š' -> 'ш'
                'A' -> 'А'; 'B' -> 'Б'; 'V' -> 'В'; 'G' -> 'Г'; 'D' -> 'Д'; 'Đ' -> 'Ђ'
                'E' -> 'Е'; 'Ž' -> 'Ж'; 'Z' -> 'З'; 'I' -> 'И'; 'J' -> 'Ј'; 'K' -> 'К'
                'L' -> 'Л'; 'M' -> 'М'; 'N' -> 'Н'; 'O' -> 'О'; 'P' -> 'П'; 'R' -> 'Р'
                'S' -> 'С'; 'T' -> 'Т'; 'Ć' -> 'Ћ'; 'U' -> 'У'; 'F' -> 'Ф'; 'H' -> 'Х'
                'C' -> 'Ц'; 'Č' -> 'Ч'; 'Š' -> 'Ш'
                else -> ch
            }
            result.append(converted)
        }
        return result.toString()
    }

    /**
     * Uklanja dijakritike (č, ć, š, ž, đ -> c, c, s, z, dj) radi fleksibilnog mečovanja imenika.
     */
    fun stripDiacritics(text: String): String {
        val latin = cyrillicToLatin(text)
        return latin
            .replace("č", "c").replace("Č", "C")
            .replace("ć", "c").replace("Ć", "C")
            .replace("š", "s").replace("Š", "S")
            .replace("ž", "z").replace("Ž", "Z")
            .replace("đ", "dj").replace("Đ", "Dj")
    }

    /**
     * Širi skraćenice i brojeve u celovite reči na srpskom jeziku kako bi ih TTS izgovorio tečno i prirodno.
     */
    fun prepareForTts(text: String): String {
        var result = cyrillicToLatin(text)

        // Skraćenice
        result = result.replace(Regex("(?i)\\bSMS\\b"), "es-em-es poruka")
        result = result.replace(Regex("(?i)\\bkm/h\\b"), "kilometara na čas")
        result = result.replace(Regex("(?i)\\bmin\\b"), "minuta")
        result = result.replace(Regex("(?i)\\bsec\\b"), "sekundi")

        // Zamena uobičajenih cifara rečima
        numberWords.forEach { (num, word) ->
            result = result.replace(Regex("\\b$num\\b"), word)
        }

        // Ujednačavamo pismo za podešeni srpski TTS glas.
        return latinToCyrillic(result)
    }
}

