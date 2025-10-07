# Solora\nKotlin Android app (Jetpack Compose).
# Solora

![App Logo](README asset/Solora Logo.jpg)


Solora is a Kotlin Android app for solar sales consultants to calculate system sizes, generate quotes, and manage leads. It integrates with Firebase (Auth, Firestore, Analytics, Callable Functions) and a lightweight backend implemented as Firebase Cloud Functions. The app supports offline Firestore persistence, PDF generation, and Google Sign-In.

### Key Highlights
- **Quote calculator** with optional NASA POWER irradiance inputs (via Cloud Function)
- **Lead management** with search/filter and real-time updates
- **User profile & settings** synced via Functions and Firestore
- **Cloud Functions API** for secure operations and server-side logic
- **Offline support** for Firestore
- **PDF generation** (iText) and file sharing via `FileProvider`
- **Google and Email/Password authentication**


## Table of Contents
- Features
- Architecture Overview
- Tech Stack and Libraries
- Cloud Functions API (Callable Endpoints)
- Data Models
- App Navigation and Screens
- Permissions
- Setup and Installation
  - Prerequisites
  - Android App Setup and Run
  - Firebase Project Setup
  - Cloud Functions: Develop, Emulate, Deploy
- Troubleshooting
- Project Structure


## Features
- **Quotes**
  - Create and update quotes with client, address, tariff, panel size, etc.
  - Calculate system size, estimated generation, monthly savings, and payback period.
  - Optionally fetch NASA solar irradiance and derive average sun-hours.
  - List/search previous quotes; open quote details.

- **Leads**
  - Create, update, and list leads linked to the authenticated consultant.
  - Search by name/email/phone and filter by status.

- **User Profile & Settings**
  - Register/login (email/password and Google Sign-In).
  - Store and sync user/company info used to enrich saved quotes.

- **PDF Generation & Sharing**
  - Generate quote PDFs (iText) and share via Android `FileProvider`.

- **Offline & Sync**
  - Firestore offline persistence for quotes and leads.
  - Optional "sync" Cloud Function to merge offline data to cloud.


## Architecture Overview
- **Android app (Kotlin, MVVM-ish)**
  - ViewModels in `dev.solora.quotes`, `dev.solora.leads`, `dev.solora.settings` orchestrate flows.
  - `FirebaseRepository` centralizes data access to Firestore and Cloud Functions.
  - `AuthRepository` handles Firebase Auth and persists minimal user info via DataStore.
  - `FirebaseFunctionsApi` wraps callable Cloud Functions for server-side logic.
  - Navigation is fragment-based with the Android Navigation Component.

- **Backend (Firebase Cloud Functions)**
  - Node.js Functions implement callable endpoints for calculations, data access, and sync.
  - NASA POWER API consulted during quote calculation when coordinates are available.


## Tech Stack and Libraries

### Android
- Kotlin 2.0.20, AGP 8.5.2, JVM target 17, compile/target SDK 35, min SDK 24
- Firebase
  - BOM `com.google.firebase:firebase-bom:33.3.0`
  - Auth (`firebase-auth-ktx`)
  - Firestore (`firebase-firestore-ktx`)
  - Analytics (`firebase-analytics-ktx`)
  - Callable Functions (`firebase-functions-ktx:20.4.0`)
- Google Sign-In: `com.google.android.gms:play-services-auth:20.7.0`
- AndroidX
  - Core KTX, Fragment KTX
  - Navigation: `androidx.navigation:navigation-fragment-ktx` / `navigation-ui-ktx` (2.8.0)
  - Lifecycle: ViewModel/Livedata/Runtime KTX (2.8.7)
  - WorkManager: `androidx.work:work-runtime-ktx:2.9.1`
  - DataStore Preferences: `androidx.datastore:datastore-preferences{,-core}:1.1.1`
- Networking: Ktor Client (2.3.12) [core, android, logging, content-negotiation, kotlinx-json]
- Permissions helper: `com.google.accompanist:accompanist-permissions:0.36.0`
- PDF: iText 7 (note: both `8.0.5` and `7.2.5` are present)
- Material Components: `com.google.android.material:material:1.12.0`

### Cloud Functions
- Node.js 22 (see `functions/package.json`)
- `firebase-functions@^6`, `firebase-admin@^12`, `axios@^1.12`


## Cloud Functions API (Callable Endpoints)
All callable endpoints require the user to be authenticated.

- `calculateQuote(data)`
  - Input: `{ address, usageKwh?, billRands?, tariff, panelWatt, latitude?, longitude? }`
  - Output: `{ success, calculation: { panels, systemKwp, inverterKw, monthlySavings, estimatedGeneration, paybackMonths, nasaData? } }`
  - Behavior: Optionally calls NASA POWER API to compute average sun-hours.

- `saveQuote(data)`
  - Input: quote fields (reference, client details, calculations...). Server enriches with company info from `user_settings` and timestamps.
  - Output: `{ success, quoteId }`

- `getQuoteById({ quoteId })`
  - Output: `{ success, quote? }` (only if owned by current user)

- `getQuotes({ search?, limit?=50 })`
  - Output: `{ success, quotes }` filtered to current user; simple search over `reference`, `clientName`, `address`.

- `saveLead(data)`
  - Input: `{ name, email, phone, status?, notes?, quoteId? }`
  - Output: `{ success, leadId }`

- `getLeads({ search?, status?, limit?=50 })`
  - Output: `{ success, leads }` filtered to current user; optional status filter and client-side search.

- `getSettings()` / `updateSettings({ settings })`
  - Output: `{ success, settings }` / `{ success, message }`

- `syncData({ offlineData })`
  - Merges local leads/quotes/settings into cloud; returns per-collection counts and errors.

- `healthCheck` (HTTP)
  - GET returns `{ status, timestamp, service, version, endpoints[] }`

Android app integrates these via `dev.solora.api.FirebaseFunctionsApi` and `dev.solora.data.FirebaseRepository` (which also provides Firestore fallbacks and real-time listeners).


## Data Models (Firestore)
- `FirebaseQuote`: id, reference, clientName, address, usageKwh, billRands, tariff, panelWatt, lat/lon, irradiance/sun-hours, calculation outputs, company/consultant snapshot, `userId`, `createdAt`, `updatedAt`.
- `FirebaseLead`: id, name, email, phone, status, notes, `quoteId`, `userId`, timestamps.
- `FirebaseUser`: id, name, surname, email, phone?, company?, role, timestamps.


## App Navigation and Screens
- `MainActivity` hosts a fragment-based navigation flow (see `res/navigation`).
- `HomeFragment` displays counts, shortcuts, and recent quotes.
- Leads and Quotes sections have list/detail flows, backed by `LeadsViewModel` and `QuotesViewModel`.
- Settings/Profile screens manage language, company info, and account actions.


## Permissions
Declared in `AndroidManifest.xml`:
- `INTERNET` (network, Firebase, NASA API)
- `POST_NOTIFICATIONS` (optional notifications)
- `READ_EXTERNAL_STORAGE` / `WRITE_EXTERNAL_STORAGE` (PDF export on older Androids)

`FileProvider` is configured via `xml/file_paths.xml` for secure sharing of generated files (e.g., PDFs).


## Setup and Installation

### Prerequisites
- Android Studio (Hedgehog / Jellyfish or newer)
- Java 17 (bundled with recent Android Studio)
- Android SDK 35
- A Firebase project with:
  - Email/Password Auth enabled
  - Google Sign-In enabled (OAuth client configured)
  - Firestore in Native mode
  - Firebase Functions enabled

### Android App Setup and Run
1) Clone the repo and open the project root in Android Studio.
2) Ensure `google-services.json` is present at `app/google-services.json` (already included here for the target project). If using your own Firebase project, download a new `google-services.json` from the Firebase Console and replace it.
3) Sync Gradle. The app targets SDK 35 and uses Kotlin 2.0.20.
4) Run on a device/emulator:
   - Use the Run button in Android Studio, or
   - CLI: `./gradlew :app:assembleDebug` then install the APK.
5) Sign in:
   - Use Email/Password or Google Sign-In, depending on what you enabled.

Release signing: `app/build.gradle.kts` references a release keystore at `app/keystore/solora-release-key.keystore`. Adjust or remove this block if you use your own signing configuration.

### Firebase Project Setup
1) In Firebase Console, create a project (or reuse an existing one) and add an Android app with the correct `applicationId` (`dev.solora` by default).
2) Enable Auth providers (Email/Password, Google) in Authentication settings.
3) Enable Firestore (Native mode). For production queries with compound ordering/filters, you may need indexes. The app logs when an index is required; create the suggested index in the Firebase Console if you see those logs.
4) Download and place `google-services.json` into `app/`.

### Cloud Functions: Develop, Emulate, Deploy
From the repo root:

Local develop with emulators:
```bash
cd functions
npm ci
npm run serve  # runs Firebase emulators for functions
```

Deploy to Firebase:
```bash
cd functions
npm ci
npm run deploy
```

Other scripts:
```bash
npm run logs   # tail function logs
npm run shell  # interactive function shell
```

Functions runtime: Node 22 (see `functions/package.json`). Ensure your Firebase CLI is up to date and that your project is selected (`firebase use`).


## Troubleshooting
- **Auth errors calling Functions**: Ensure you’re signed in before invoking callable functions. All callable endpoints require authentication.
- **Missing Firestore index**: Logs may show an index requirement for queries on `quotes` or `leads` (ordered by `createdAt` filtered by `userId`). Create the suggested index in the Firebase Console under Firestore Indexes.
- **Google Sign-In configuration**: Make sure your SHA-1/256 fingerprints and OAuth client are configured in Firebase Console for the app package `dev.solora`.
- **Duplicate iText versions**: Dependencies include `com.itextpdf:itext7-core:8.0.5` and `7.2.5`. Prefer a single aligned version to avoid conflicts.
- **Old storage permissions**: On Android 10+, consider using `MediaStore`/`scoped storage`; WRITE permission may not be necessary depending on your PDF flow.


## Project Structure (high-level)
```
app/
  src/main/java/dev/solora/
    api/               # FirebaseFunctionsApi wrapper
    auth/              # AuthRepository and auth flows
    data/              # FirebaseRepository, models
    leads/, quotes/    # Feature ViewModels & UI controllers
    navigation/        # Fragments and navigation glue
    settings/, i18n/   # Settings, language store
    SoloraApp.kt       # App initialization (Firebase, Firestore offline, i18n)
    MainActivity.kt    # Activity host
  src/main/res/        # layouts, drawables, nav graphs, values
functions/             # Firebase Cloud Functions (Node.js)
```


## License
This project is for educational purposes. Review any third-party libraries’ licenses (e.g., iText) before production use.


