# CLAUDE.md

Guidance for Claude Code (or any agent) working in this repository.

## What this repo is

Despite the parent folder name (`learning-react`), this is **not** a JavaScript/React project. It's the source for **Spot Reminder**, a native Android app written in Kotlin (package `com.spotreminder.app`) — a location-triggered travel reminder with trip tracking, cost stats, and optional Google Drive backup. See [README.md](README.md) for the product description.

## Repo layout

This is a normal Gradle Android project, rooted at the repo root:

- `build.gradle.kts`, `settings.gradle.kts`, `gradle.properties` — top-level Gradle config.
- `app/build.gradle.kts` — module config: `applicationId`, SDK versions, dependencies, debug signing config.
- `app/debug.keystore` — the app's signing keystore (see "Signing" below).
- `app/src/main/AndroidManifest.xml` — permissions, activities, service, FileProvider.
- `app/src/main/java/com/spotreminder/app/` — all Kotlin sources.
- `app/src/main/res/` — layouts (`layout/`), drawables, string/color/theme resources (`values/`, `values-night/`).
- `.github/workflows/build-apk.yml` — CI: builds a debug APK on every push and publishes it to Releases on the default branch.

Edit source files directly under `app/src/main/` like any normal Android project — there is no code generation step and nothing is embedded in the workflow file anymore.

> Historical note: earlier versions of this repo had the entire project embedded as heredoc blocks inside `build-apk.yml`, generated at CI time into an untracked `proj/` directory. That was flattened into a real, directly-editable source tree — if you see references to that setup elsewhere (old branches, forks, issues), they're describing the prior structure, not the current one.

## App architecture

- `App.kt` — Application class, applies saved theme on launch.
- `MainActivity.kt` — Home screen: detected-city status, quick-add city+spot form (OSM autocomplete via `Osm.kt`), kicks off the periodic WorkManager job.
- `SpotsActivity.kt` / `SpotDetailActivity.kt` — city/spot list and per-city checklist + trip start/stop.
- `MapPickerActivity.kt` / `TripMapActivity.kt` — osmdroid map screens (pick a location / replay a trip route).
- `StatsActivity.kt` — aggregates `Trip` records: distance (haversine), cost, per-category breakdown.
- `ProfileActivity.kt` — local profile fields + Google Sign-In, triggers `DriveBackup.kt`.
- `SettingsActivity.kt` — theme + location/notification permission management.
- `Store.kt` — SharedPreferences-backed JSON store for cities/spots/theme/profile/last-notified-city. `rawData()`/`setRawData()` exist specifically for Drive backup/restore serialization.
- `TripStore.kt` / `Trip.kt` — separate SharedPreferences-backed store for `Trip` records (path points, cost items, note) + haversine distance calc.
- `TripTrackingService.kt` — foreground Service recording GPS points (~10s/15m interval) during an active trip.
- `LocationChecker.kt` — core arrival-detection logic: last/fresh GPS fix → reverse geocode (Android `Geocoder`, falls back to Nominatim REST) → normalize/fuzzy-match against saved city keys → `Notifier`.
- `LocationWorker.kt` — WorkManager `Worker` running `LocationChecker` roughly every 15 minutes.
- `Notifier.kt` — builds the "Welcome to `<city>`! You wanted to visit: ..." notification (channel `arrival`).
- `DriveBackup.kt` — raw Drive v3 REST calls to back up/restore the `Store` JSON blob to the signed-in user's private `appDataFolder`.

No Room/SQLite, no Retrofit/OkHttp, no DI framework, no Navigation component, no tests — everything is hand-rolled with `Intent`-based navigation and `HttpURLConnection`.

## CI / auto-build

`.github/workflows/build-apk.yml` triggers on **every push, to any branch**, plus manual `workflow_dispatch`:

1. `actions/checkout` → `actions/setup-java` (Temurin 17) → `gradle/actions/setup-gradle` (Gradle 8.7).
2. Accepts Android SDK licenses.
3. `gradle assembleDebug --no-daemon --stacktrace` at the repo root.
4. Uploads `app/build/outputs/apk/debug/spot-reminder.apk` as a workflow artifact (every branch).
5. Publishes that APK to GitHub Releases (tag `build-<run_number>`) — gated with `if: github.ref_name == github.event.repository.default_branch` so feature-branch pushes get a build artifact but don't spam Releases.

When adding a new module, dependency, or resource type, keep the workflow's Gradle invocation in sync (e.g. if a wrapper is added later, switch `gradle` to `./gradlew`).

## Signing

`app/debug.keystore` is a fixed, committed keystore (password `android`, alias `androiddebugkey`) used for **both debug and release build types** (see `signingConfigs`/`buildTypes` in `app/build.gradle.kts`). This is intentional: its SHA-1 fingerprint is registered with Google Sign-In / Drive API, so builds stay installable/updatable and Drive backup keeps working across CI runs. Do not regenerate or replace it without understanding this dependency — doing so silently breaks sign-in and Drive backup, and invalidates in-place updates for anyone who already installed a build signed with the old key.

Note this also means anyone with repo access can produce APKs that Android will treat as update-compatible with real releases — acceptable for a hobby/personal project relying on a fixed debug key, but worth flagging if the project ever needs real distribution security guarantees (Play Store release signing, restricted key access, etc.).

## Testing

No test framework or test sources exist (no JUnit/Espresso/Robolectric, no `test`/`androidTest` source sets). If adding meaningful new logic (e.g. to `LocationChecker`'s matching logic or `Trip`'s distance calc), flag to the user that there's no test harness rather than silently skipping verification.

## Don't assume this is a React/JS project

The parent directory is named `learning-react`, but this repo has no `package.json`, no JS/TS, nothing Node-related. Run `git ls-tree -r HEAD --name-only` if ever in doubt about what's actually tracked.
