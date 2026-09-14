"use strict";
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { onCall, HttpsError } = require("firebase-functions/v2/https");
const { createHash } = require("node:crypto");
const { validateRequest, hourLabel, dateLabel, calculateTotal } = require("./booking-validation");
initializeApp();
const db = getFirestore();
const options = { region: "asia-south1", maxInstances: 5 };
function requireAuth(request) { if (!request.auth) throw new HttpsError("unauthenticated", "Sign in first."); return request.auth.uid; }

// This endpoint reserves a demo booking. It never represents a verified money transfer.
exports.reserveDemoBooking = onCall(options, async request => {
  const uid = requireAuth(request);
  let data;
  try { data = validateRequest(request.data); } catch (error) { throw new HttpsError("invalid-argument", error.message); }
  const id = "BMT-" + createHash("sha256").update(uid + ":" + data.requestId).digest("hex").slice(0, 24);
  const bookingRef = db.collection("bookings").doc(id);
  return db.runTransaction(async tx => {
    const existing = await tx.get(bookingRef);
    const profile = await tx.get(db.collection("users").doc(uid));
    if (!profile.exists || profile.get("disabled") === true) throw new HttpsError("permission-denied", "Account is unavailable.");
    if (existing.exists) {
      const previous = existing.data();
      if (previous.userId !== uid || previous.turfId !== data.turfId || previous.day !== data.day || previous.startHour !== data.startHour || previous.durationHours !== data.duration) throw new HttpsError("already-exists", "Request was already used for a different booking.");
      if (previous.status === "CANCELLED") throw new HttpsError("failed-precondition", "That request was cancelled. Start a new booking.");
      return { ...previous, createdAt: null };
    }
    const turfRef = db.collection("turfs").doc(data.turfId);
    const turfDoc = await tx.get(turfRef);
    const settings = await tx.get(db.doc("config/public"));
    if (settings.exists && settings.get("acceptBookings") === false) throw new HttpsError("failed-precondition", "New bookings are temporarily paused.");
    if (!turfDoc.exists || turfDoc.get("active") !== true) throw new HttpsError("failed-precondition", "This venue is unavailable.");
    const turf = turfDoc.data();
    if (!Number.isInteger(turf.openingHour) || !Number.isInteger(turf.closingHour) || data.startHour < turf.openingHour || data.startHour + data.duration > turf.closingHour) throw new HttpsError("failed-precondition", "These hours are outside the venue schedule.");
    const hours = Array.from({ length: data.duration }, (_, i) => data.startHour + i);
    const ids = hours.map(hour => `${data.turfId}_${data.day}_${hour}`);
    const reservations = await tx.getAll(...ids.map(key => db.collection("reservations").doc(key)));
    const slots = await tx.getAll(...ids.map(key => db.collection("slots").doc(key)));
    if (reservations.some(doc => doc.exists)) throw new HttpsError("already-exists", "A selected slot was just booked. Choose another time.");
    if (slots.some(doc => doc.exists && doc.get("state") !== "AVAILABLE")) throw new HttpsError("failed-precondition", "A selected slot is blocked.");
    const prices = slots.map(doc => doc.exists ? doc.get("price") : turf.pricePerHour);
    let amount;
    try { amount = calculateTotal(prices); } catch (error) { throw new HttpsError("failed-precondition", error.message); }
    if (Math.abs(amount - data.expectedTotal) > .02) throw new HttpsError("failed-precondition", "Pricing changed. Please select your slots again.");
    const date = dateLabel(data.day);
    const time = `${hourLabel(data.startHour)} - ${hourLabel(data.startHour + data.duration)}`;
    const booking = { id, userId: uid, turfId: data.turfId, turfName: turf.name, date, day: data.day, time, startHour: data.startHour, durationHours: data.duration, amount, status: "CONFIRMED", customer: profile.get("name") || "Player", paymentStatus: "SIMULATED", slotIds: ids };
    tx.create(bookingRef, { ...booking, createdAt: FieldValue.serverTimestamp() });
    ids.forEach((key, i) => tx.create(db.collection("reservations").doc(key), { turfId: data.turfId, day: data.day, date, time: hourLabel(hours[i]), price: prices[i] }));
    tx.create(db.collection("notifications").doc(id), { userId: uid, title: "Booking confirmed", message: `${turf.name} · ${date} · ${time}`, time: "Just now", unread: true, type: "booking" });
    return booking;
  });
});

exports.cancelBooking = onCall(options, async request => {
  const uid = requireAuth(request);
  const id = request.data?.bookingId;
  if (typeof id !== "string" || !/^BMT-[a-f0-9]{24}$/.test(id)) throw new HttpsError("invalid-argument", "Invalid booking.");
  return db.runTransaction(async tx => {
    const ref = db.collection("bookings").doc(id);
    const doc = await tx.get(ref);
    const profile = await tx.get(db.collection("users").doc(uid));
    if (profile.get("disabled") === true) throw new HttpsError("permission-denied", "Account disabled.");
    if (!doc.exists) throw new HttpsError("not-found", "Booking not found.");
    const booking = doc.data();
    if (booking.userId !== uid && request.auth.token.admin !== true) throw new HttpsError("permission-denied", "This booking belongs to another user.");
    if (booking.status === "CANCELLED") return { cancelled: true };
    if (booking.status !== "CONFIRMED") throw new HttpsError("failed-precondition", "Only confirmed bookings can be cancelled.");
    tx.update(ref, { status: "CANCELLED", cancelledAt: FieldValue.serverTimestamp() });
    booking.slotIds.forEach(key => tx.delete(db.collection("reservations").doc(key)));
    return { cancelled: true };
  });
});
