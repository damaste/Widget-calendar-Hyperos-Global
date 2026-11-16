# Calendario IT — widget calendario in stile HyperOS, in italiano

Widget 4×2 (ridimensionabile) per Android / HyperOS:
- griglia mensile con settimana che inizia di lunedì, giorni di sabato/domenica attenuati, oggi evidenziato in blu
- mese e giorni della settimana in italiano (es. "Novembre 2026", L M M G V S D)
- pannello a destra con giorno, data e prossimi 3 eventi letti dal Calendario di sistema (CalendarContract: funziona con il Calendario HyperOS Global)
- tocco su un giorno → apre il Calendario su quella data; ‹ › cambiano mese; tocco sul titolo → torna a oggi
- pallino sotto i giorni che hanno eventi (anche eventi su più giorni)
- schermata di configurazione: colore, primo giorno della settimana (lunedì/domenica), pallini on/off, numero di eventi mostrati
- tema chiaro/scuro automatico

## Compilare
1. Apri la cartella in Android Studio (Koala o più recente) e lascia che faccia la sincronizzazione Gradle (genera da solo il wrapper).
2. Run ▶ su dispositivo, oppure Build › Build APK(s).
3. Apri l'app, tocca "Consenti accesso al calendario", poi "Aggiungi il widget alla Home" (oppure tieni premuto sulla Home › Widget › Calendario IT).

## Su HyperOS
Se il widget non si aggiorna: Impostazioni › App › Calendario IT › Avvio automatico ON, e Risparmio energetico › Nessuna restrizione.

## Ottenere l'APK senza Android Studio
Carica la cartella su un repository GitHub: il workflow `.github/workflows/build-apk.yml` compila l'APK debug
(Actions › Build APK › Artifacts › CalendarioIT-debug-apk). Da Android Studio: Build › Build APK(s).
