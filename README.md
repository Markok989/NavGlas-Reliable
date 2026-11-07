# NavGlas Reliable

Android glasovni asistent na srpskom, preuzet iz postojećeg NavGlas projekta.

## Status

Razvojna verzija. APK i rad na Samsung Galaxy S24 još nisu verifikovani.
Prvobitni projekat u D:\Android\NavGlas ostaje neizmenjen.

## Prve izmene

- Samo izričita potvrda odobrava poziv ili SMS; otkazivanje prekida dijalog.
- Nejasan izbor ne odabira automatski prvi kontakt ili broj.
- Izabrani broj prenosi se do izvršavanja poziva i poruke.
- Greške slušanja imaju najviše dva ponavljanja; gašenje uklanja zakazane zadatke.
- SOS ne bira prvi kontakt iz imenika. Bez određenog primaoca odbija slanje.
- Poruka o SMS-u razlikuje predaju telefonu od potvrđene isporuke.
- Manifest uključuje vidljivost govornih servisa.

## Provera

Standardna komanda: `gradlew.bat :app:testDebugUnitTest :app:assembleDebug`.
U izolovanom okruženju Gradle 9.6 prijavljuje radni direktorijum kao neupisiv iako pojedinačni upis uspeva.
Odvojena kompilacija engine paketa i JUnit testovi koriste lokalni Kotlin 2.2.10 i Android 37.0 API.
To ne zamenjuje Gradle build koji je podešen na Kotlin 2.0.21, Android testove ili proveru na telefonu.

## Sledeći prioriteti

1. Proveriti pun build i usaglasiti verzije alata.
2. Uvesti jedinstveno stanje razgovora i identitet kontakta sa brojem kroz ceo tok.
3. Sačuvati i proveriti SOS primaoca; izbaciti opasne rezervne izbore.
4. Dodati potvrde slanja i isporuke SMS-a, duge poruke i izbor SIM kartice.
5. Proveriti dozvole, pokretanje servisa u pozadini i Bluetooth na S24.
6. Povezati stanje servisa sa ekranom i prikazati greške korisniku.
7. Smanjiti dozvole i preispitati čitanje poruka naglas i cloud obradu.

## Izvori

- https://developer.android.com/reference/android/speech/SpeechRecognizer
- https://developer.android.com/reference/android/speech/tts/TextToSpeech
- https://developer.android.com/reference/android/telephony/SmsManager
