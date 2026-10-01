# Spot Reminder

**Spot Reminder** is a native Android app that reminds you what you wanted to do or see in a city, right when you arrive there. Save a city and a checklist of "spots" (places, food, activities), and the app watches your location in the background — when it detects you're in a saved city, it fires a notification listing what you wanted to visit.

It has grown from a simple arrival-reminder into a small travel companion app:

- **Home** — add a city and spot via a popup (OpenStreetMap-based autocomplete), see the currently detected city, and trigger a manual location check.
- **Spots** — browse saved cities and check off spots as you visit them.
- **Trip tracking** — start a trip from a spot and the app records your GPS path in the background (foreground service). Costs can be logged as many times as you like, during an active trip or added later to a finished one — no fixed category list required.
- **Navigate to a spot** — see your current location and the spot on a map with the real driving route, distance and estimated time (via the free OSRM routing service — no live traffic; see "Tech stack" below), plus a recenter button.
- **Stats** — total distance traveled (haversine), total cost, trip count, and a cost breakdown by category.
- **Map** — pick a city location on an OpenStreetMap view, or replay a trip's recorded route.
- **Profile** — a local profile (name/phone/address) plus optional Google Sign-In, used to back up and restore your data to your own Google Drive.
- **Settings** — light/dark/system theme, and location & notification permission management.

## Project structure

This is a standard Gradle Android project rooted at the repo root:

```
├── app/
│   ├── build.gradle.kts          module config, dependencies, debug signing
│   ├── debug.keystore             fixed debug keystore (see "Signing" below)
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/spotreminder/app/   all Kotlin sources (activities, services, data stores)
│       └── res/                   layouts, drawables, values
├── build.gradle.kts               top-level plugin versions
├── settings.gradle.kts
├── gradle.properties
└── .github/workflows/build-apk.yml   CI: builds a debug APK on every push
```

## Continuous integration

[.github/workflows/build-apk.yml](.github/workflows/build-apk.yml) builds a debug APK automatically on **every push, to any branch**, and can also be run manually from the Actions tab (`workflow_dispatch`):

1. Checks out the repo, sets up JDK 17 and Gradle.
2. Runs `gradle assembleDebug`.
3. Uploads `spot-reminder.apk` as a workflow artifact (available on every branch's runs).
4. On pushes to the default branch only, also publishes the APK to GitHub Releases (tag `build-<run_number>`).

## Getting the app

- **Easiest**: grab the latest APK from this repo's [Releases](../../releases) page, or from the artifacts of the latest [build-apk workflow run](../../actions/workflows/build-apk.yml) for any branch.
- **Manual trigger**: run the "build-apk" workflow via the Actions tab (`workflow_dispatch`).
- **Locally**: open the repo root in Android Studio, or run `gradle assembleDebug` (or `./gradlew assembleDebug` once you generate a wrapper) from the repo root.

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Classic Android Views (XML layouts), Material Components, ConstraintLayout — no Jetpack Compose |
| Background work | WorkManager (periodic ~15 min location check), a foreground Service (trip GPS tracking) |
| Maps | osmdroid (OpenStreetMap tiles), Nominatim REST API for search/reverse-geocoding, free public OSRM demo server for driving routes/ETA (no live traffic — that needs a paid API) |
| Auth / backup | Google Sign-In + raw Drive v3 REST calls to the user's private `appDataFolder` |
| Persistence | `SharedPreferences`, storing hand-rolled JSON blobs (no Room/SQLite) |
| Networking | Raw `HttpURLConnection` (no Retrofit/OkHttp) |
| Build | Gradle Kotlin DSL, AGP 8.5.2, Kotlin 1.9.24, compileSdk/targetSdk 34, minSdk 26 |
| Tests | None currently |

## Permissions

`ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`, `ACCESS_BACKGROUND_LOCATION` (arrival detection while the app isn't open), `POST_NOTIFICATIONS` (arrival alerts), `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_LOCATION` (live trip tracking), `INTERNET` / `ACCESS_NETWORK_STATE` (map tiles, Nominatim, Drive backup).

## License

MIT — see [LICENSE](LICENSE).
