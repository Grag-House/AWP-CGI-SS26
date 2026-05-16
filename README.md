# Temi-CGI-App

Dieses Projekt ist eine Android-Anwendung für den **Temi-Roboter**, entwickelt für den Kunden 
**CGI** im Rahmen des
Anwendungsprojektes an der **Hochschule Karlsruhe (HKA)** im Sommersemester 2026.

## 🚀 Überblick

Die **Temi-CGI-App** dient als interaktive Schnittstelle für den Temi-Roboter. Das Projekt umfasst
ein modernes Dashboard, Systemeinstellungen und eine robuste Architektur zur Erweiterung um weitere
Roboter-Funktionen.

## ✨ Funktionen

- **Dashboard:** Zentrale Anlaufstelle für Informationen, Statusanzeigen (z.B. Webserver-Verbindung)
  und eine Übersicht der einzelnen Seiten.
- **Webserver-Integration:** Integration von lokalem Webserver oder Webserver in der IONOS Cloud
  direkt per WebView.
- **Navigation:** Anzeige der aktuellen Roboter-Position und der Umgebungskarte & Zielsteuerung: Auswahl von vordefinierten Räumen (z. B. Küche, Kaffeemaschine, Empfang), die der Roboter autonom anfährt.
- **Modus-Auswahl:** Konfiguration der Hosting-Umgebung (Wechsel zwischen **On-Premise** und **IONOS Cloud**).
- **Wetter:** Anzeige des örtlichen Wetters direkt in der Anwendung.
- **Einstellungen:** Konfiguration der Anwendung und Einblick in Systemparameter.
- **Sidebar Navigation:** Einfacher Wechsel zwischen den verschiedenen Screens (Dashboard, Settings, Navigation, etc.).
- **Netzwerk-Status:** Anzeige der WLAN-Signalstärke und Verbindungsinformationen.
- **Vollbild-Modus:** Optimierte UI für den Temi-Bildschirm durch Ausblenden der Systemleisten.

## 🛠 Technologien & Bibliotheken

- **Sprache:** [Kotlin](https://kotlinlang.org/)
- **UI-Framework:** [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Roboter-Schnittstelle:** [temi SDK](https://github.com/robotemi/sdk) 
- **Dependency Injection:** [Koin](https://insert-koin.io/)
- **Architektur:** MVVM (Model-View-ViewModel) mit einer Shell-basierten Navigationsstruktur.
- **Testing:** [JUnit 5](https://junit.org/) & [MockK](https://mockk.io/) für Unit-Tests.
- **Logging:** [Timber](https://github.com/JakeWharton/timber) für performantes loggen
- **Design:** Custom "CgiTheme" basierend auf Material Design 3 und der CGi Corporate Identity

## 📁 Projektstruktur

- `app/src/main/java/hka/awp/cgi/temi/app`
    - `feature/`: Enthält die verschiedenen Funktionsbereiche (Dashboard, Webserver, Settings,
      Navigation,
      Weather, Modus).
    - `ui/`: UI-Komponenten, Themes und die Shell-Struktur (Sidebar, MainShell).
    - `koin/`: Konfiguration der Dependency Injection.
    - `utils/`: Hilfsklassen für Zeit, Netzwerk und System-Interaktionen.
- `app/src/test/`: Unit-Tests für die Geschäftslogik (z.B. `TimeUtilsTest`, `NetworkManagerTest`).

## ⚙️ Setup & Installation

1. **Repository klonen:**
   ```bash
   git clone <repository-url>
   ```
2. **Projekt in Android Studio öffnen:**
   Empfohlen ist die aktuellste Version von Android Studio (Ladybug oder neuer).
3. **Gradle Sync:**
   Einen Gradle-Sync durchführen, um alle Abhängigkeiten zu laden.
4. **Ausführen:**
   Ein angeschlossenes Temi-Gerät oder einen Emulator auswählen und auf **Run** klicken.

## 🧪 Tests ausführen

Die Unit-Tests lassen sich über Android Studio oder die Kommandozeile starten:

```bash
./gradlew test
```

---
© 2026 Hochschule Karlsruhe (HKA) - AWP CGI Projekt
