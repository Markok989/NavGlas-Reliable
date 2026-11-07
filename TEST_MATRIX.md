# Matrica verifikacije

Rezultat je važeći samo za commit i Android verziju iz konkretnog CI izveštaja.
Skripta u test APK-u isporučuje tekst i greške kroz Android SpeechRecognizer IPC; to nije stvarni govor u mikrofon.

| Oblast | Scenario | Provera |
|---|---|---|
| Pokretanje | Bez dozvola prikazuje se ekran za dozvole | emulator: skripta za snimanje |
| Pokretanje | Dozvoljen mikrofon, bez dodatnih dozvola | UI test početnog ekrana |
| Mikrofon | Drugi audio klijent preuzima ekskluzivni fokus | VoiceLifecycleTest: slušanje ostaje aktivno |
| Mikrofon | Odbijena dozvola | vidljiva greška, nema početka slušanja |
| Prepoznavanje | Mrežna greška | vidljiva greška, povratak u IDLE |
| Prepoznavanje | Tri uzastopne tišine pri potvrdi | ograničeni pokušaji, nema izvršavanja |
| Poziv | Isto ime, različiti kontakti | izbor pre potvrde, tačan broj |
| Poziv | Kontakt sa više brojeva | izabrani broj prenosi se do izvršavanja |
| Poziv | Nepoznat kontakt | nema izvršavanja |
| Poziv | Nedostaje CALL_PHONE | objašnjenje bez otvaranja poziva |
| SMS | Tekst poruke ostaje posle izbora kontakta/broja | unit + Android recognizer test |
| SMS | Izmena teksta ne menja primaoca | unit test |
| SMS | Nedostaje SEND_SMS | objašnjenje bez predaje poruke sistemu |
| SMS | Duga poruka: potvrda svih delova, bilo koji redosled | SmsSendTrackerTest |
| SMS | Duplikati i nevažeći rezultati | ne mogu izazvati lažan uspeh |
| SMS | Jedan deo neuspešan ili rezultat ne stiže | FAILED/UNKNOWN, bez lažnog uspeha |
| Brzi odgovor | Nema poslednjeg pošiljaoca | nema rezervnog primaoca niti slanja |
| SOS | Nema podešenog primaoca | nema slanja |
| SOS | Podešen primalac | čeka potvrdu |
| Glas | Nedostaje TTS | komanda ostaje spremna, nema automatskog izvršavanja |
| Glas | Nedostaje TTS, potvrda na ekranu | tačna komanda izvršena jednom |
| Glas | Nedostaje TTS, sledeći dodir mikrofona | potvrda se obrađuje kroz prepoznavanje |
| Prekid | Otkaži tokom izbora, diktiranja i potvrde | nema izvršavanja |
| Prekid | Zakasneli završetak govora posle otkazivanja | ne pokreće ponovo mikrofon |
| UI | Portret, horizontalni prikaz i podešavanja | testovi i pregled stvarnih snimaka |
| Podešavanja | Čuvanje SOS primaoca | UI test + SharedPreferences |

## Fizička provera koja ostaje otvorena

- S24: stvarni mikrofon, instalirani pružalac prepoznavanja i stvarni srpski izgovor.
- Bluetooth sa konkretnim automobilom/slušalicama; mikrofon i promena uređaja u toku sesije.
- Stvarni poziv, dolazni poziv, SMS operatera, isporuka i dve SIM kartice.
- Zaključan ekran, isključivanje ekrana, Samsung upravljanje baterijom i duži rad.
- Prekidi tokom stvarne navigacije i istovremeno reprodukovanje muzike.

Bez povezanog fizičkog uređaja ove stavke nisu potvrđene. Ne proglašavati ih uspešnim na osnovu UI testova ili simuliranog pružaoca prepoznavanja.

