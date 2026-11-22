# HyperCalendar Widget

**🇬🇧 English** · [🇮🇹 Italiano](#italiano)

A free, open-source Android home-screen calendar widget inspired by the HyperOS calendar widget that Xiaomi ships only in China. Made for HyperOS Global phones, works on any Android 8.0+ device.

<!-- Add a screenshot: upload it to the repo (e.g. docs/screenshot.png) and uncomment the next line -->
<!-- ![HyperCalendar Widget](docs/screenshot.png) -->

## Features

- Month grid with today highlighted, plus a side panel with the day and your upcoming events
- **Month + photo of the day** variant (Bing, Wikimedia Commons, or your own gallery)
- Dots under days that have events (multi-day events included)
- Choose which calendars / accounts to show
- Custom colors for month title and dots, with a live preview in settings
- Week starts on Monday or Sunday
- Follows the system language: English, Italiano, Français, Español, Русский
- Light / dark theme, 12/24-hour format from system settings
- Reads events from the system calendar (works with the HyperOS Global Calendar, Google Calendar, etc.)

## Install

1. Open the [**Releases**](../../releases) page and download the latest `.apk` (or use the artifact of the latest successful run under **Actions**).
2. Open the file on your phone and allow installation from this source.
3. Open **HyperCalendar Widget**, grant calendar access, then tap **Add** next to the widget you want, or long-press the home screen › Widgets.

If the widget stops updating on HyperOS/MIUI: Settings › Apps › HyperCalendar Widget › enable *Autostart* and set battery saver to *No restrictions*.

## Privacy

- The only permission for your data is **read-only calendar access**, used to show events on the widget. Nothing is collected, stored elsewhere or sent anywhere.
- The Internet permission is used only to download the photo of the day, if you enable it. Only the image is requested; no personal data is sent.

## Build from source

Android Studio (Koala or newer): open the folder and run **Build › Build APK(s)**.
Without Android Studio: push the project to GitHub; the workflow in `.github/workflows` builds the APK (Actions › latest run › Artifacts).

Requirements: JDK 17, Android SDK 34, Gradle 8.7, Kotlin. Package: `it.hypercal.widget`.

## Support

The app is free. If you find it useful you can buy me a coffee:

[![Support me on Ko-fi](https://ko-fi.com/img/githubbutton_sm.svg)](https://ko-fi.com/damaste)

Bug reports and ideas are welcome in [Issues](../../issues).

## Disclaimer

This is an independent project, not affiliated with or endorsed by Xiaomi. "HyperOS" and "Xiaomi" are trademarks of their respective owners and are used only to describe compatibility. Photos shown by the widget belong to their respective authors and providers (Bing, Wikimedia Commons).

## License

[MIT](LICENSE)

---

## Italiano

Widget calendario per la schermata Home di Android, gratuito e open source, ispirato a quello del calendario HyperOS venduto solo in Cina. Pensato per i telefoni con HyperOS Global, funziona su qualsiasi Android 8.0+.

**Funzioni**
- Griglia mensile con il giorno corrente evidenziato e pannello con data e prossimi eventi
- Variante **mese + foto del giorno** (Bing, Wikimedia Commons o la tua galleria)
- Pallino sotto i giorni con eventi (anche eventi su più giorni)
- Scelta dei calendari / account da mostrare
- Colori personalizzati per titolo del mese e pallini, con anteprima dal vivo
- Settimana che inizia di lunedì o domenica
- Segue la lingua di sistema: italiano, inglese, francese, spagnolo, russo
- Tema chiaro/scuro, formato 12/24 ore di sistema

**Installazione**
1. Scarica l'ultimo `.apk` dalla pagina [Releases](../../releases).
2. Aprilo sul telefono e consenti l'installazione da questa origine.
3. Apri l'app, consenti l'accesso al calendario e tocca **Aggiungi** accanto al widget (oppure tieni premuto sulla Home › Widget).

Se il widget non si aggiorna su HyperOS: Impostazioni › App › HyperCalendar Widget › Avvio automatico, e Risparmio energetico › Nessuna restrizione.

**Privacy**: l'app legge il calendario solo in lettura, per mostrare gli eventi; non raccoglie né invia dati. Internet serve solo per scaricare la foto del giorno, se attivata.

**Supporto**: l'app è gratuita; se ti piace puoi offrirmi un caffè su [Ko-fi](https://ko-fi.com/damaste).
