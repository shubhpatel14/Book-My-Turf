# Firebase connection

Project: `bookmyturf-aeea3`. Registered Android package: `com.bookmyturf.app.demo`.
The configuration is saved to `app/google-services.json` (already excluded from Git).
It configures the debug/optimized app; register `com.bookmyturf.app` separately before
building the release variant. Do not alter the JSON package to pretend it is registered.

## Console preparation

Billing is intentionally disabled at the owner's request. Do not upgrade the project,
provision Storage, or deploy Cloud Functions without new approval.

1. Enable Authentication > Google. Register the app's SHA-1 and refresh `google-services.json`.
2. Create Cloud Firestore Standard edition (default database). Use production rules.
3. Sign Firebase CLI in as `shubhp069@gmail.com`, which has access to this project.

The code is integrated but these console services must be initialized before sign-in
and data sync work. The existing installed local app has not been replaced by this build.
`CloudFeatures.kt` disables photo uploads and the prepared callable booking flow.
This does not mean bookings inherently require billing: a separately designed and
tested free-plan reservation flow could be implemented later.

## Deploy

From the repository root:

```powershell
npx --yes firebase-tools@14.17.0 login:add
npx --yes firebase-tools@14.17.0 projects:list
cd firebase/functions
npm ci
npm test
cd ../..
npx --yes firebase-tools@14.17.0 deploy --project bookmyturf-aeea3 --account shubhp069@gmail.com --only firestore
```

Deployment requires owner/deployer access; the Android configuration does not grant it.
The callable functions use `asia-south1` (matching the Android client).

## Admin access

Sign in with Google as `shubhp069@gmail.com`. Assign its Firebase Authentication
UID the `admin: true` custom claim from a trusted Admin SDK environment, then sign out
and back in. Never add an admin flag to a writable profile document or embed a service
account key in the Android app. No default admin password is shipped with cloud login.

## Data flow

- `users/{uid}` stores profiles; each user's `favorites` subcollection is private.
- `turfs` stores venue metadata and Cloud Storage references, not local file paths.
- `slots` stores date-specific admin pricing and blocks.
- `reservations` publishes availability without customer identities.
- `bookings` and `notifications` are filtered to the signed-in user (admin can read bookings).
- `offers`, `reviews`, and `config/public` drive the matching app screens.
- Photos are uploaded to `turf-images/{turfId}/{uuid}.jpg` and fetched via authenticated Storage SDK requests.
- `reserveDemoBooking` validates the user, venue, date, prices and slot availability on
  the server, then atomically creates reservations. Retries use an idempotent request ID.
- `cancelBooking` verifies ownership/admin access and atomically releases slots.

All payments remain **SIMULATED**. The callable function does not verify or collect
money. Real payment orders and webhooks must be implemented before production checkout.

The existing local database remains on the phone. It is not silently uploaded as user
accounts/bookings, because local demo records have no Firebase user ownership.

## Verification

`gradlew.bat :app:testDebugUnitTest :app:assembleDebug` verifies the Android build.
`npm test` in `firebase/functions` validates booking input, India-time handling and totals.
Do not call data sync live until Authentication, Firestore, security rules and the first
administrator are configured and tested on separate accounts. Storage and functions are
prepared only; do not deploy them while billing is disabled. Enabling their app flags
alone does not deploy or verify the backend. Do not store image bytes in Firestore.
