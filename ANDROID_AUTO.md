# Android Auto: stvarni obim podrške

Originalna aplikacija u D:\Android\NavGlas nema Android Auto manifest, CarAppService niti komunikacione notifikacije. Bluetooth prijemnik i overlay na telefonu nisu Android Auto integracija.

## Implementirano

- Deklaracija Android Auto podrške za komunikaciona obaveštenja.
- Dolazni SMS tokom uključenog NaviGlas režima vožnje pravi privatno MessagingStyle obaveštenje sa akcijama odgovora i označavanja kao pročitano.
- Android Auto može da pročita obaveštenje i prosledi glasovni odgovor kroz RemoteInput. Auto host vodi diktiranje i potvrdu; NaviGlas ne otvara ekran telefona niti njegov mikrofon.
- Primalac odgovora dolazi iz privatne, trajno sačuvane evidencije. Mutable PendingIntent ne određuje telefonski broj. Nepoznata ili istekla identifikacija se odbija.
- Isti odgovor se može predati samo jednom; nema automatskog ponavljanja kada je rezultat nepoznat.
- Slanje koristi remoteMessaging foreground servis, prati sve delove SMS-a i završava se posle rezultata ili isteka čekanja. „Poslato mreži” nije tvrdnja o isporuci.
- Čitanje briše samo odgovarajuće NaviGlas obaveštenje. Ne menja SMS bazu drugog programa niti uklanja noviju poruku.

Za ovaj put potrebne su dozvole RECEIVE_SMS, SEND_SMS i obaveštenja, SIM sa uslugom SMS-a, uključen režim vožnje i Android Auto koji prihvata razvojnu instalaciju. Operater može naplatiti SMS po tarifi; aplikacija ne koristi plaćeni AI API.

## Nije potvrđeno / nije implementirano

- Stvarna projekcija i glasovni odgovor u automobilu nisu testirani bez fizičkog Android Auto hosta ili DHU.
- Android Auto bira svoj govorni servis i dostupne jezike. Ova integracija ne dodaje srpski jezik njegovom asistentu.
- Ovo nije zasebna aplikacija sa ikonama i proizvoljnim ekranima na displeju automobila. Nije navigaciona aplikacija.
- Nema samostalnog hotword asistenta preko mikrofona automobila niti zamene za Gemini/Google Assistant.
- Pozivanje preko Android Auta ostaje u sistemskom interfejsu telefona/automobila. NaviGlas nema sopstvenu Car calling integraciju.
- Telefonsko stalno slušanje ključne reči još nije implementirano. SpeechRecognizer nije namenjen neprekidnom slušanju. Potrebno je posebno lokalno prepoznavanje i fizička provera pre tvrdnje da je vožnja potpuno bez dodira.
- SMS dozvole i distribucija na Google Play zahtevaju posebnu proveru pravila i prihvatljive namene; razvojni APK nije potvrda Play odobrenja.

## Primarni izvori provereni 7.11.2026.

- https://developer.android.com/training/cars/communication/notification-messaging
- https://developer.android.com/develop/background-work/services/fgs/service-types#remote-messaging
- https://developer.android.com/reference/android/speech/SpeechRecognizer
- https://developer.android.com/training/cars/testing/dhu

Automatski testovi proveravaju strukturu obaveštenja, akcije, privatnost, trajnost primaoca, istek, odbijanje duplikata i zaštitu novijeg obaveštenja. Ne predstavljaju test govora, stvarne SMS mreže ili automobila.
