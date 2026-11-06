# Reading Counter

Proof of Concept Android nativa per verificare la gestione dei tasti volume tramite `MediaSession` e `VolumeProvider`, primo incremento del blueprint Reading Counter.

## Stato

Questa prima milestone attiva una sessione foreground e registra il percorso tecnico per intercettare Volume UP/DOWN. La compatibilità simultanea con Kindle o Google Play Libri non è garantita e deve essere verificata su dispositivo reale.

## Verifica PoC

1. Build e installazione dell’APK debug.
2. Attivare “Counter Mode” nell’app.
3. Provare Volume DOWN/UP con Kindle e Google Play Libri, in foreground, background e a schermo spento.
4. Compilare `docs/volume-key-test-matrix.md` con i risultati osservati.

## Build

Richiede JDK 17, Android SDK 35 e Gradle 8.7+. `./gradlew assembleDebug` produce `app/build/outputs/apk/debug/app-debug.apk`.

## Roadmap

Room e DataStore, sessioni e aggregazioni giornaliere, notifica interattiva, modalità Reader/Counter, UI completa e test strumentali.
