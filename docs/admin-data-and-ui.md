# Admin data and customer UI

The app uses a shared observable repository backed by `filesDir/bookmyturf-store-v1.bin`.
Writes are serialized and saved atomically before publishing changes. The first launch
loads seed venues and demo bookings; subsequent launches load saved data.

## Implemented

- Customer typography, onboarding, authentication, saved venues, notifications, and shared controls use the rounded purple appearance. Admin retains its own lime theme and layout.
- Admin venue editor: name, area, city, description, facilities, sports, indoor/outdoor,
  hourly price, rating, distance, opening/closing hours, and four photos. Photo picker
  imports bounded-size images into app-private storage and handles EXIF rotation.
- Admin photo changes appear on Home, search, Explore, favorites, venue details, and the booking summary. First photo is the cover; replace, remove and promote-to-cover are supported.
- Per-venue, per-date slot blocking and custom hourly pricing. Customer dates cover today through the next six days.
- Reservation commit checks current availability and prices; competing reservations cannot claim the same slot. Cancellation releases reserved hours, respecting admin blocks.
- Admin totals use saved records; payment listings are explicitly simulated booking amounts.
- Offer banner creation/editing and visibility sync to Home. These are informational banners, not a coupon redemption engine.
- Review moderation syncs to venue pages. User list derives from saved profiles; email sign-in can be disabled. Notification read state persists.
- Global booking acceptance switch pauses/resumes customer availability.

## Boundaries

This is local persistence, not a remote production backend. Admin and customer screens
in this installation share changes immediately. Separate phones do not synchronize.
No Firebase project configuration, authentication service, media storage service, or
payment provider is connected. The Firebase directory contains draft rules only.
Admin credentials and payment handling remain a demo. A configured backend with
server-enforced roles, transactional reservations and payment verification is required
before production use. Banner discounts, push reminders, legal documents and live
support are not connected services.

Existing booking names and amounts are retained as historical snapshots when a venue
is edited. Removing a photo from a venue does not destroy its file; this avoids breaking
an in-flight edit. Uninstalling/clearing app data removes local records and imported photos.

## Validation

`gradlew.bat :app:testDebugUnitTest` covers create/favorite flows, edit/photo/offer/review
persistence, restart behavior, write failure, duplicate reservations, cancellation,
and stale/inactive/blocked checkout rejection.
