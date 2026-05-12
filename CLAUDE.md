
This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android app for personal English vocabulary learning. Single-user (the owner of this repo), no backend, no Play Store, all data local in SQLite. Built greenfield from a Spring Boot template that was discarded in commit 1 ("Spring Boot template snapshot").

**Product goal**: solve the owner's pain with existing flashcard apps — opaque memory model, weak collection-from-reading loop, oral-English oriented vocabulary. The differentiation lever is **data transparency**: every rating is logged to `review_logs` so a future "vocabulary profile" page can expose the FSRS state (stability/difficulty/retrievability) directly.

**v0.1 status (current)**: word browser + Share Sheet ingestion + preloaded dictionary + FSRS review loop are all done. WorkManager daily reminder and a stats page remain (week 5–6 of the original plan).

## Commands

- Build debug APK: `./gradlew :app:assembleDebug` (output: `app/build/outputs/apk/debug/app-debug.apk`)
- Compile only (faster check): `./gradlew :app:compileDebugKotlin`
- Install on connected device: `./gradlew :app:installDebug`
- Run unit tests: `./gradlew :app:testDebugUnitTest`
- Single test: `./gradlew :app:testDebugUnitTest --tests com.workplat.englishpulish.domain.fsrs.FsrsTest`
- Rebuild the bundled dictionary from raw NDJSON sources: `python3 scripts/build_preload.py` (raw zips under `scripts/raw/` are gitignored — re-download from kajweb/dict via jsDelivr if missing; see commit history for URLs)

## Environment

- Java 21 (Microsoft OpenJDK), Android SDK at `~/Library/Android/sdk`
- Installed: build-tools 36.1.0, platforms android-36 — `compileSdk = 36`, `buildToolsVersion = "36.1.0"` are pinned to what's local because the SDK manager cannot fetch other revisions from this network
- `minSdk = 34`, `targetSdk = 36`
- `local.properties` holds `sdk.dir` and is gitignored

## Network / build quirks (important — do not "fix" these)

- The global `~/.gradle/gradle.properties` contains a broken proxy config (empty host, port 80). The project's `gradle.properties` explicitly blanks out `systemProp.{http,https}.proxy{Host,Port}` to force direct connections. Do not remove these overrides.
- `settings.gradle.kts` prefers Aliyun mirrors before Google/Maven Central — Google Maven is reachable but slow/intermittent from this network. GitHub raw is effectively unreachable; use `cdn.jsdelivr.net/gh/...` mirror for any GitHub-hosted asset.
- `ksp.useKSP2=false` is required. KSP2 + Hilt 2.52 throws "unexpected jvm signature V". Stay on KSP1 until Hilt ≥ 2.54 is adopted.

## Architecture

Single-module Android app. **MVVM + Repository**, Jetpack Compose UI, Hilt DI, Paging 3, kotlinx.serialization for the bundled dictionary.

```
app/src/main/java/com/workplat/englishpulish/
├── EnglishPulishApp.kt         @HiltAndroidApp entry
├── MainActivity.kt             single Activity, NavHost with word_list + review routes
├── data/
│   ├── db/                     Room: entities + DAOs + AppDatabase
│   ├── preload/                PreloadEntry + PreloadSource (reads assets/preload.json)
│   └── repo/                   WordRepository, ReviewRepository, WordFilter, AddResult
├── di/                         Hilt @Module providers (DatabaseModule)
├── domain/
│   ├── fsrs/                   FSRS-4.5 pure-function implementation + Rating/CardState
│   ├── model/                  ReviewCard (UI-facing, built on demand)
│   └── text/                   TextParser (Share Sheet word/sentence split)
├── tts/                        TtsManager (system TextToSpeech singleton wrapper)
└── ui/
    ├── theme/                  Material 3 theme (minimal, tool-feel — do not redesign without asking)
    ├── words/                  WordListScreen + ViewModel: paged browser, search, source filter, FAB, TTS row buttons, source chips
    ├── review/                 ReviewScreen + ViewModel: 4-button FSRS review state machine
    └── share/                  ShareReceiverActivity + ViewModel: transparent Activity + ModalBottomSheet
```

## Data model (Room v1)

- `words` — lemma is unique (case-insensitive at the app layer, exact at the DB layer), soft-delete via `deletedAt`, UUID string id (sync-friendly), `source` ∈ {"preload-gaozhong", "preload-kaoyan", "preload-both", "share", "manual", "seed"}
- `review_states` — 1:1 with `words` via `wordId`, holds FSRS state (`stability`, `difficulty`, `state` enum, `lastReviewAt`, `dueAt`, `lapses`, `reps`). Indexed on `dueAt` for "due today" queries
- `review_logs` — append-only history of every rating; columns include `stabilityBefore/After` and `elapsed/scheduled days`. **Never prune** — this is the fuel for the future "vocabulary profile" page (the product's differentiation)

Schema snapshots export to `app/schemas/` (KSP arg). Future migrations must be added with `addMigrations(...)` on `Room.databaseBuilder`.

## Bundled dictionary (`assets/preload.json`)

6045 entries built from `kajweb/dict` (an open-source Chinese-learner word dataset). The Python script `scripts/build_preload.py`:
- Merges 11 PEPGaoZhong + 3 KaoYan NDJSON files
- Drops phrases (multi-token), abbreviations (`BC`, `PE`), and entries missing a Chinese gloss
- Strips redundant `pos.` prefixes from `tranCn`
- Keeps the first 2 senses and the first example sentence
- Marks each entry with `level: "gaozhong" | "kaoyan" | "both"` (kaoyan wins on conflict, level → "both")

The Android side never re-derives this — it's a pure asset. To regenerate after editing the script: `python3 scripts/build_preload.py` writes back to `app/src/main/assets/preload.json`, rebuild the APK.

## FSRS algorithm

`domain/fsrs/Fsrs.kt` — FSRS-4.5 with default 19-weight set and 0.9 desired retention. Pure functions, zero Android dependencies, 7 unit tests covering: new-card bootstrap, Again increments lapses, Easy > Good interval, consecutive Good grows interval, Again drops stability, retrievability decays, difficulty clamps to [1, 10].

Used by `ReviewRepository.rate(card, rating)` which: (1) runs FSRS, (2) writes the new `ReviewStateEntity`, (3) appends a `ReviewLogEntity`. The review session is **transactional UI** — `ReviewViewModel` snapshots the queue on entry and advances by cursor, not by re-observing the Flow. Don't change this to "live" observation: it would cause cards to vanish/reorder mid-session.

## Today's queue

`ReviewRepository.todayQueue(newLimit = 20)`:
1. All "old" due cards (state ≠ 0, dueAt ≤ now) sorted by dueAt
2. Up to `newLimit` "new" cards (state = 0) sorted alphabetically by lemma
3. Old then new (committed product decision — see week-3 conversation)

`observeTodayDueCount()` re-emits via a single SQL that does `old_count + MIN(new_count, newLimit)`. The home-screen review button binds to this.

The 20-per-day new card cap is currently **hardcoded** in `ReviewRepository.DEFAULT_NEW_LIMIT`. A settings page (v0.2) will surface it.

## Conventions

- All IDs are `String` UUIDs, not auto-increment Long — required for future device sync without ID collisions
- All timestamps are `Long` epoch millis (UTC) — no `Instant`/`LocalDateTime` at the Room layer
- ViewModels use `StateFlow` exposed via `stateIn(viewModelScope, WhileSubscribed(5_000), …)`; Compose collects with `collectAsStateWithLifecycle()`
- One-shot UI events (Toast messages) go through a `Channel<String>` exposed as `events: Flow<String>`, **not** a StateFlow — Toasts should not re-fire on recomposition
- Repositories are `@Singleton`, DAOs are unscoped, `AppDatabase` is `@Singleton`
- FSRS and TextParser are pure-function singletons (Kotlin `object`) under `domain/` — must be unit-testable without Android dependencies
- Paging 3 queries live on the DAO and accept normalized parameters (lowercased prefix, expanded source list, plus `sourcesEmpty` bypass flag for empty IN lists)
- Material 3 theme is intentionally minimal/tool-feel. Do not introduce extra accent colors, animations, or gamification surfaces without explicit user confirmation — the owner picked "极简 / 工具感" in week 1

## Scope discipline

This is a personal-use app. Resist these urges:
- ❌ Adding multi-user, login, cloud sync — explicitly out of scope
- ❌ Adding LLM calls for definitions — the preload already covers it; LLM budget is reserved for v0.3 (oral practice, mnemonics, personalized examples)
- ❌ Adding charts/graphs to a stats page — v0.1 should ship with just numbers
- ❌ Adding a tagging UI — the schema reserves space but the UI is deferred
- ❌ Writing unit tests for ViewModels/UI/Repos — only `domain/` gets tests
- ❌ Backwards-compatibility shims or feature flags — the app has one user

## Open product threads (next time you pick this up)

In rough priority order — confirm with the owner before starting:

1. **Daily reminder via WorkManager** — one fixed time (the owner picked "one fixed time", default 21:00 hardcoded for now). PeriodicWorkRequest that re-queries `observeTodayDueCount` and fires a local notification deep-linking to the review screen.
2. **Stats page** — numbers only, no charts: today reviewed / total in library / streak days / mastered-vs-learning split. Powered by `review_logs` aggregations.
3. **Settings page** — surface the new-card limit, reminder time, and (later) FSRS desired retention.
4. **Undo last rating** — review screen toolbar action; reads back the last `review_logs` row and inverts the `review_states` update.
5. **End-to-end device validation** — the owner hasn't installed any build on a real phone yet. The first install will probably surface UX issues with Share Sheet, TTS locale, and the initial 6045-row seed timing.
