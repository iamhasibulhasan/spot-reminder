# Spot Reminder

React Native (Expo) travel app: save spots, get notified when you're near, track your path on OpenStreetMap, and calculate trip costs.

## Features
- Email sign-up / sign-in, plus **Continue with Google**
- Add a spot by country, district or name (OpenStreetMap Nominatim search) or by tapping the map
- Geofence reminders: a notification fires when you come within the spot's radius (200 m – 5 km)
- Per-spot costs by category (transport, food, stay, tickets, shopping, other), travel start/end (from, to, dates, mode), total / per person / per day
- Record your travel path in the background, view it on the map, share a summary or export GPX
- Profile: name, phone, photo, currency, light/dark/system theme
- Maps: OpenStreetMap tiles via Leaflet — free, no API key

## Get the APK
Every push to `master`/`main` runs `.github/workflows/build-apk.yml`, which builds a release APK and publishes it under **Releases** (also available as a build artifact). Open the repo → *Releases* → download `spot-reminder.apk`.

## Google sign-in (optional)
1. Google Cloud Console → *APIs & Services* → *Credentials* → create an **Android** OAuth client. Package name: `com.spotreminder.app`. SHA-1: printed in the workflow log under "Print signing SHA-1".
2. Create a **Web** OAuth client too.
3. In GitHub → *Settings → Secrets and variables → Actions*, add `GOOGLE_ANDROID_CLIENT_ID` and `GOOGLE_WEB_CLIENT_ID`.
4. Push again. Without these, the Google button explains it isn't configured; email sign-up still works.

## Develop
```
npm install
npx expo start          # needs a dev build for background location; Expo Go is limited
npm run icons           # regenerate icons (node scripts/generate-icons.js)
```
Accounts and data are stored on the device (AsyncStorage). Notifications and tracking need "Allow all the time" location permission.
