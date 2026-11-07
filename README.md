# NaviGlas Reliable

Glasovni Android asistent na srpskom, sa jednostavnim ekranom za vožnju.
Razvojna verzija **1.1.1-dev**. Podržava Android 8.0 (API 26) i noviji.

Aplikacija ne zahteva pretplatu, API ključ niti plaćeni cloud servis. Koristi instalirane Android servise za govor i prepoznavanje. SMS, pozive i mobilni internet obračunava operater prema korisnikovom paketu.

## Šta radi

- Pozivanje kontakta ili broja uz izričitu potvrdu.
- Diktiranje SMS-a, čitanje pripremljene poruke, izmena i potvrda slanja.
- Razlikovanje kontakata sa istim imenom i izbor jednog od više brojeva.
- Brzi odgovor poslednjem pošiljaocu; nema rezervnog slanja prvom kontaktu.
- SOS izabranom primaocu, uz potvrdu i poslednju lokaciju mlađu od pet minuta, ako je dostupna.
- Komande za vreme, lokaciju, čitanje poslednjeg SMS-a pristiglog tokom rada i upravljanje dolaznim pozivom gde Android dozvoljava.
- Mehur preko navigacije i pločica u brzim podešavanjima.

Glasovni servis je jedini vlasnik mikrofona i TTS-a. Ekran posmatra stvarno stanje servisa.
Prepoznavanje upravlja svojim audio fokusom: aplikacija ne prekida mikrofon kada pružalac prepoznavanja preuzme zvuk.
Ako podrazumevani TTS nema srpski, pokušava se instalirani Google TTS. Ako glas nije dostupan, komanda ostaje vidljiva i čeka izričitu potvrdu na ekranu ili novi dodir mikrofona za glasovni odgovor.
Slušanje se pokreće dodirom; nema stalnog osluškivanja ključne reči niti automatskog pokretanja mikrofona pri Bluetooth povezivanju.
Zvuk koristi rutu koju je izabrao Android. Stvarni Bluetooth uređaj zahteva zaseban praktičan test.

## Primeri

- „Pozovi Marka” → izbor kontakta/broja ako je potrebno → „potvrdi”.
- „Pošalji poruku Ani da stižem za deset minuta” → pregled → „potvrdi” ili „promeni”.
- „Otkaži” prekida razgovor bez izvršavanja.
- „Koliko je sati”, „Gde sam”, „Pročitaj poruku opet”.

## Prvo pokretanje

1. Dozvolite mikrofon. Kontakti, pozivi, SMS i lokacija potrebni su samo za odgovarajuće radnje.
2. U sistemskim podešavanjima govora instalirajte/podesite srpski TTS glas i poslušajte primer.
3. U podešavanjima aplikacije unesite SOS primaoca i tekst brzog odgovora.
4. Ako imate dve SIM kartice, podesite podrazumevanu karticu za SMS.
5. Za mehur posebno dozvolite prikaz preko drugih aplikacija.

Poslednji SMS se čuva samo u memoriji aktivnog servisa. Njegovo telo se ne čita automatski naglas.
Podaci o kontaktima i porukama se ne šalju posebnom cloud servisu iz aplikacije.
Sistemsko prepoznavanje govora može koristiti internet i zavisi od instaliranog pružaoca usluge.
SMS status razlikuje predaju telefonu, rezultat slanja i neproverenu isporuku primaocu.
SOS nije zamena za hitne službe. Lokacija se ne prikazuje kao trenutna ako je zastarela.

## Build i testovi

Potreban je JDK 25, Android SDK 37.0, Build Tools 37.0.0 i SDK putanja u lokalnom `local.properties` ili `ANDROID_HOME`.

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
./gradlew connectedDebugAndroidTest
```

Na Windowsu koristite `gradlew.bat`. `local.properties`, keystore fajlovi i build izlazi ne pripadaju repozitorijumu.
GitHub Actions pokreće unit testove, lint, debug build i emulator testove, i čuva APK, izveštaje i snimke ekrana kao artefakte.
Testni RecognitionService i skriptovani odgovori nalaze se isključivo u test APK-u. Oni proveravaju Android IPC i životni ciklus prepoznavanja, ali ne predstavljaju akustički test stvarnog srpskog prepoznavanja.
Proverite rezultat poslednjeg workflow pokretanja za odgovarajući commit; postojanje workflow fajla nije dokaz uspešnog testa.

## Ograničenja verifikacije

Original u `D:\Android\NavGlas` ostaje neizmenjen.
Ovo nije izdanje za Google Play. SMS/Call Log i druge osetljive mogućnosti podležu posebnim pravilima distribucije.
Stvarni pozivi, SMS operatera, srpsko prepoznavanje govora u buci i Bluetooth u automobilu zahtevaju proveru na fizičkom S24.
Testovi na emulatoru ne dokazuju te mogućnosti.

## Tehnički izvori

- https://developer.android.com/reference/android/speech/SpeechRecognizer
- https://developer.android.com/reference/android/speech/tts/TextToSpeech
- https://developer.android.com/reference/android/telephony/SmsManager
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- https://developer.android.com/develop/ui/compose/testing

