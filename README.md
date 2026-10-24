# TicTacToe

A native Android Tic-Tac-Toe game with cloud accounts, an AI opponent powered by Google Gemini, match history with statistics, four languages, and full light/dark theming.

Built with **Java** and traditional **XML layouts** (View system), backed by **Firebase** and the **Gemini API**.

---

## Features

- **Two-player local mode** — play against a friend on the same device, with a coin-flip to assign X and O.
- **Single-player vs AI** — three difficulties (Easy, Medium, Hard) driven by the Google Gemini API, with difficulty expressed through prompt engineering.
- **Offline fallback** — if the AI request fails, the game offers to continue against a local random-move opponent so play never gets stuck.
- **Accounts** — email/password sign-up and login via Firebase Authentication.
- **Match history & stats** — every game is saved to Cloud Firestore; the history screen shows per-player win/loss/draw records and a win/loss/draw pie chart.
- **Four languages** — English, French, German, Turkish, switchable in-app.
- **Light & dark themes** — a lavender light theme and a gold-on-black dark theme, with the choice persisted across restarts.
- **Polish** — win/lose/draw sound effects, a screen flash and vibration on losing to the AI, and animated screen transitions.

---

## Tech stack

| Area | Technology |
|------|------------|
| Language | Java (UI in XML layouts, View Binding) |
| Min / Target / Compile SDK | 24 / 35 / 35 |
| Build system | Gradle (Kotlin DSL), Gradle 9.4.1 wrapper |
| Auth & storage | Firebase Authentication, Cloud Firestore |
| AI opponent | Google Gemini API (`gemini-2.5-flash`) |
| Charts | MPAndroidChart `v3.1.0` (via JitPack) |
| UI | AndroidX, Material Components |

---

## Project structure

```
TicTacToe/
├── app/
│   ├── build.gradle.kts            # Module config: SDK levels, deps, BuildConfig key injection
│   ├── google-services.json        # Firebase config (do not commit real credentials)
│   └── src/main/
│       ├── AndroidManifest.xml     # App config, permissions, activity registration
│       ├── java/com/example/tictactoe/
│       │   ├── TicTacToeApp.java        # Application class — applies theme/language at startup
│       │   ├── LoginActivity.java       # Launcher screen, Firebase sign-in
│       │   ├── RegisterActivity.java    # Sign-up with live field validation
│       │   ├── MainMenuActivity.java    # Hub: mode selection + settings drawer
│       │   ├── AddPlayers.java          # Two-player name entry
│       │   ├── MainActivity.java        # The game board and core logic
│       │   ├── GeminiHelper.java        # Single-player AI: builds prompt, calls Gemini
│       │   ├── HistoryActivity.java     # Stats aggregation + charts
│       │   ├── ResultDialog.java        # Win/draw popup
│       │   └── PreferencesManager.java  # SharedPreferences wrapper
│       └── res/
│           ├── layout/             # Screen layouts (one per activity)
│           ├── values/             # Colors, strings, themes, dimensions
│           ├── values-night/       # Dark-mode color & theme overrides
│           ├── values-de|fr|tr/    # Translated strings
│           ├── drawable/           # Shapes, gradients, vector icons
│           ├── anim/               # Screen transition animations
│           └── raw/                # Win/lose/draw sound effects
└── gradle/
    └── libs.versions.toml          # Centralized dependency version catalog
```

---

## How it works

### Game logic
The board is a single `int[]` of nine cells (`0` empty, `1` player one, `2` player two). A move is recorded, then outcomes are checked in a fixed order: **win first, then draw, then continue**. Win detection compares all eight winning lines against the current player.

### AI opponent
`GeminiHelper` runs the network request on a background thread. It encodes the board as a readable grid (empty cells show their index number), builds a difficulty-specific prompt, sends it to the Gemini API, then filters the reply down to a single legal move before posting it back to the UI thread. Two independent validation layers ensure an illegal move can never land on the board.

### Theming & localization
Every resource is referenced by name and resolved against the device's current configuration. Dark mode and language are both just folder qualifiers (`values-night`, `values-fr`, etc.), so switching either one simply triggers Android to re-resolve resources — no per-color or per-string logic in the Java. The chosen settings are persisted to `SharedPreferences` and re-applied at startup by `TicTacToeApp` before the first frame draws.

---

## Getting started

### Prerequisites
- Android Studio (recent stable release)
- JDK 21
- A Firebase project
- A Google Gemini API key

### Setup

1. **Clone the repository**
   ```bash
   git clone <your-repo-url>
   cd TicTacToe
   ```

2. **Connect Firebase**
   - Create a Firebase project and add an Android app with package name `com.example.tictactoe`.
   - Enable **Email/Password** sign-in under Authentication.
   - Create a **Cloud Firestore** database.
   - Download your `google-services.json` and place it in `app/`.

3. **Add your Gemini API key** to `local.properties` in the project root:
   ```properties
   GEMINI_API_KEY=your_api_key_here
   ```
   This file is read at build time and injected into `BuildConfig`. It is gitignored and must not be committed.

4. **Build and run**
   ```bash
   ./gradlew assembleDebug
   ```
   or open the project in Android Studio and run on a device/emulator (API 24+).

---

## Firestore data model

Games are stored per user:

```
users/{uid}/games/{autoId}
    player1: String        # player one's name
    player2: String        # player two's name (or "AI (Hard)" etc.)
    winner:  String        # a player's name, or "Draw"
    isSinglePlayer: Boolean
    timestamp: Timestamp    # server-side
```

The stats screen reads this collection, splits it into single-player and multiplayer, and aggregates per-player records by simple string comparison against the `winner` field.

---

## Security notes

> **Important:** Do not commit real credentials. Both `local.properties` (Gemini key) and `google-services.json` (Firebase config) should be kept out of version control and treated as secrets.

- Injecting the Gemini key via `BuildConfig` keeps it out of source, **but not out of the shipped APK** — anyone can decompile the app to read it. For a published app, route Gemini calls through a backend server that holds the key instead.
- Restrict your keys in the Google Cloud / Firebase console (limit the API key by app package name and signing certificate, and to only the APIs you use).
- Configure Firestore **security rules** so each user can only read and write their own `users/{uid}` document and subcollections.

---

## Possible improvements

- Replace the random-move offline fallback with a local **minimax** routine for a provably unbeatable Hard mode and full offline play.
- Cache aggregate stat counters instead of recomputing from full history on every visit.
- Add a failure listener to the game-save call so failed writes are retried or surfaced.
- Remove the unused Jetpack Compose theme files and dependencies, since the app uses XML layouts.

---

## License

Add your chosen license here (e.g. MIT). No license file is currently included.
