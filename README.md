# BookMyTurf

A premium dark-mode Android turf booking prototype built with Kotlin, Jetpack Compose, Material 3, Navigation Compose and MVVM.

The bundled typography pairs **Bebas Neue** for athletic display copy with **Manrope** for readable interface text. Both are distributed under the SIL Open Font License; license files are included in `app/src/main/assets/licenses/`.

## Run the demo

1. Open this directory in Android Studio.
2. Let Gradle sync and run the `app` configuration on Android 8.0 (API 26) or newer.
3. Tap **Continue with demo user**, or use the labelled admin credentials:
   - `admin@bookmyturf.demo`
   - `Admin@12345`

The default build is deliberately self-contained: turf search, filters, favorites, consecutive slot selection, price calculation, simulated UPI processing, booking creation/cancellation, notifications, analytics, turf/slot management and admin views work without credentials or network access. The payment QR is a visual demo ticket and is explicitly marked non-production.

For phone demos, build `assembleOptimized` and install `app/build/outputs/apk/optimized/app-optimized.apk`. This variant uses the same demo application ID and debug signing key, but enables R8/resource shrinking and bundled startup profiles for substantially lower runtime overhead. Keep the regular debug variant for Android Studio inspection.

## Architecture

- `data/`: domain models, repository contract and observable demo repository.
- `ui/`: app state holder/ViewModel, reusable components, theme and screen modules.
- `firebase/`: deployable starting-point Firestore/Storage rules and indexes.

`BookMyTurfRepository`, `AuthGateway` and `PaymentGateway` are the production seams. Replace the demo repository with Firebase-backed implementations using Authentication, Cloud Firestore, Storage and FCM. Payment order creation and signature verification must run on a trusted backend; do not place gateway secrets or sensitive UPI credentials in the Android client.

Recommended Firestore collections: `users`, `admins`, `turfs`, `sports`, `slots`, `bookings`, `payments`, `reviews`, `favorites`, `coupons`, and `notifications`.

## Firebase setup for production

1. Register the production application ID in Firebase and place `google-services.json` in `app/` (it is gitignored).
2. Add the Google Services plugin and Firebase Android BoM, then Auth, Firestore, Storage, and Messaging dependencies.
3. Implement the gateway/repository interfaces and switch dependency injection away from `DemoBookMyTurfRepository`.
4. Assign the `admin` custom claim only from a trusted backend and deploy the rules in `firebase/`.
5. Use App Check, server-generated payment orders, idempotent webhooks and server-side verification before confirming a booking.

Demo credentials are intentionally isolated to `AppViewModel.login` and must be removed from production builds.
