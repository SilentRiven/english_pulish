
This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android app for personal English vocabulary learning. Single-user, no backend, local SQLite via Room. Architecture and product decisions are tracked in conversation, not in extra docs.

## Commands

- Build debug APK: `./gradlew :app:assembleDebug` (output: `app/build/outputs/apk/debug/app-debug.apk`)
- Compile only (faster check): `./gradlew :app:compileDebugKotlin`
- Install on connected device: `./gradlew :app:installDebug`
- Run unit tests (when added): `./gradlew :app:testDebugUnitTest`
- Single test: `./gradlew :app:testDebugUnitTest --tests com.workplat.englishpulish.SomeTest`

## Environment

- Java 21 (Microsoft OpenJDK), Android SDK at `~/Library/Android/sdk`
- Installed: build-tools 36.1.0, platforms android-36 — `compileSdk = 36`, `buildToolsVersion = "36.1.0"` are pinned to what's local because the SDK manager cannot fetch other revisions from this network
- `minSdk = 34`, `targetSdk = 36`
- `local.properties` holds `sdk.dir` and is gitignored

## Network / build quirks (important)

- The global `~/.gradle/gradle.properties` contains a broken proxy config (empty host, port 80). The project's `gradle.properties` explicitly blanks out `systemProp.{http,https}.proxy{Host,Port}` to force direct connections. Do not remove these overrides.
- `settings.gradle.kts` prefers Aliyun mirrors before Google/Maven Central — Google Maven is reachable but slow/intermittent from this network.
- `ksp.useKSP2=false` is required. KSP2 + Hilt 2.52 throws "unexpected jvm signature V". Stay on KSP1 until Hilt ≥ 2.54 is adopted.

## Architecture

Single-module Android app, MVVM + Repository, Jetpack Compose UI, Hilt DI.

```
app/src/main/java/com/workplat/englishpulish/
├── EnglishPulishApp.kt         @HiltAndroidApp entry
├── MainActivity.kt             single Activity host
├── data/
│   ├── db/                     Room: entities + DAOs + AppDatabase
│   └── repo/                   repositories (data ↔ ViewModel boundary)
├── di/                         Hilt @Module providers
└── ui/
    ├── theme/                  Material 3 theme (minimal, tool-feel)
    └── words/                  WordListScreen + ViewModel (v0.1 acceptance surface)
```

Data model (Room v1):

- `words` — lemma is unique, soft-delete via `deletedAt`, UUID string id (sync-friendly)
- `review_states` — 1:1 with `words`, holds FSRS state (stability, difficulty, dueAt, state). Indexed on `dueAt` for "due today" queries
- `review_logs` — append-only history of every rating; powers the future "vocabulary profile" feature. **Do not prune** — this is the differentiation fuel

Schema snapshots export to `app/schemas/` (KSP arg). Future migrations should be added with `addMigrations(...)` on `Room.databaseBuilder`.

## Conventions

- All IDs are `String` UUIDs, not auto-increment Long — required for future device sync without ID collisions
- All timestamps are `Long` epoch millis (UTC) — no `Instant`/`LocalDateTime` at the Room layer
- ViewModels use `StateFlow` exposed via `stateIn(viewModelScope, WhileSubscribed(5_000), …)`; Compose collects with `collectAsStateWithLifecycle()`
- Repositories are `@Singleton`, DAOs are unscoped, `AppDatabase` is `@Singleton`
- FSRS algorithm (when added) lives under `domain/fsrs/` as pure functions — must be unit-testable without Android dependencies
