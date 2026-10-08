# AgriLink Android

One app for the three people on AgriLink: **farmers**, **buyers** and **drivers**. It talks to the Spring Boot API in `../backend` and follows the "Organic" design in `../AgriLink design`.

Kotlin, Jetpack Compose (Material 3), Retrofit + kotlinx.serialization, Coroutines/Flow, Navigation Compose, Room, WorkManager. Package `com.agrilink.app`, minSdk 26, target/compileSdk 36.

## Run it

1. Start the backend (see `../backend/README.md`). The no-Docker way is the test-scope `DevApplication`, which runs the API on port 8080 with an embedded PostgreSQL and demo data.
2. Pick how the phone reaches the API:

   | Device | API URL | How |
   |---|---|---|
   | Emulator | `http://10.0.2.2:8080/api/v1/` | the default, nothing to do |
   | Phone over USB or wireless ADB | `http://localhost:8080/api/v1/` | `adb reverse tcp:8080 tcp:8080`, build with `-PagrilinkApiUrl=http://localhost:8080/api/v1/` (no firewall changes needed) |
   | Phone on the same Wi-Fi | `http://<pc-lan-ip>:8080/api/v1/` | build with `-PagrilinkApiUrl=...`; allow port 8080 in the Windows firewall |

   Cleartext HTTP is allowed in **debug** builds only (`src/debug/res/xml/network_security_config.xml`). Release builds are HTTPS-only, except `localhost`/`10.0.2.2`.
3. Build and install:

   ```
   set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
   gradlew installDebug -PagrilinkApiUrl=http://localhost:8080/api/v1/
   ```
   The debug app id is `com.agrilink.app.debug`.

Demo accounts (password `Demo@12345`): farmers `0911000001` (Tolosa Bekele), `0911000002`; buyer `0922000001`; driver `0933000001`. In dev the OTP screen shows the test code as a tappable hint. `node ../backend/scripts/seed-demo-orders.mjs` creates orders in every stage.

Tests: `gradlew testDebugUnitTest` (30 JVM tests: formatting, Ethiopian calendar, phone parsing, session store, DTO parsing, outbox rules, and the HTTP stack against MockWebServer including single-flight token refresh). `gradlew lintDebug` is clean.

## What is in it

- **Onboarding:** language (English, አማርኛ, Afaan Oromoo) → role → sign up or sign in (SMS code or password) → profile → verification documents (photo upload) → home. Language can be changed later in Account and is applied per app with `AppCompatDelegate.setApplicationLocales`.
- **Farmer:** Today (money held/ready, new orders with Accept/Reject and countdown, upcoming pickups), Produce (list, 4-step add flow with photos, edit price/quantity, pause/activate/remove), Orders, Money (wallet, withdraw), pickup code + QR for the driver.
- **Buyer:** Market (search, categories, offline cache of the last results, "order again"), listing detail with live price quote, checkout, payment (Telebirr / CBE Birr / bank / card through the mock provider), order tracking with the driver's shared location, delivery code + QR, confirm delivery, ratings, report a problem with photos, dispute status.
- **Driver:** Jobs board with online/offline switch, job detail and accept, pickup confirmation (typed code or QR scan, weighed kg, crates, photos), start trip with foreground GPS pings every ~30 s, delivery confirmation with the buyer's code, trips and history, earnings and withdrawals.
- **Everyone:** notifications list with deep links, account/profile edit, sign out.

## How it is built

```
core/        AppError + ApiResult, formatting, Ethiopian calendar, phone helpers
data/api/    Retrofit service, DTOs mirroring the backend, token Authenticator
data/repo/   repositories (return ApiResult / ActionResult), Outbox for queued actions
data/local/  Room: listing cache, pending actions
data/prefs/  encrypted session store, settings
work/        WorkManager: OutboxWorker, NotificationSyncWorker
ui/          theme, components, one package per area, nav/Routes, AgriLinkRoot
```

- Manual DI (`AppContainer`), MVVM with `StateFlow`; screens render `Load.Loading/Ready/Failed`.
- One shared `Authenticator` refreshes the access token once on 401 (the server rotates refresh tokens) and signs the user out if the refresh is rejected.
- **Offline:** market results are cached in Room. Accept, reject and mark-ready are queued in Room when there is no signal and replayed by WorkManager (409/404 answers are dropped because replaying cannot help). Everything else needs a connection and says so.
- Role-aware buttons come from the server's `allowedActions`, so the app never guesses the order state machine.

## Verified on a real phone

Against the dev backend over wireless ADB (Samsung SM-A155F, Android 15) the whole trade was run through the app: farmer lists carrots and accepts an order, marks another ready; driver accepts the job, confirms pickup with the farmer's code, starts the trip (location pings reach the backend) and delivers with the buyer's code; buyer places an order, pays, confirms delivery and rates; the system notifications for those events appear on the phone; the app was switched to Amharic.

## Limitations

- No push (FCM needs a Firebase project): `NotificationSyncWorker` polls every 15 minutes and screens refresh while open. The notification channel and deep links are ready for FCM.
- Tracking shows the driver's last position and opens Google Maps; there is no embedded map (it needs a Maps API key). Driver GPS runs only while the trip screen is open (no background location permission).
- Amharic and Afaan Oromoo cover the main screens; the rest falls back to English. **The translations are a first draft and need review by native speakers.**
- Release builds are minified (R8) but not signed or tested on a device; add a signing config before publishing.
- iOS is not started.
