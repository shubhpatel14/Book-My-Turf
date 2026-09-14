const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const { doc, setDoc, getDoc, updateDoc, collection, query, where, getDocs } = require('firebase/firestore');
const { getFirestore } = require('firebase-admin/firestore');
const { reserveDemoBooking, cancelBooking } = require('./index');

async function main() {
  if (!process.env.FIRESTORE_EMULATOR_HOST || process.env.GCLOUD_PROJECT !== 'demo-bookmyturf') throw new Error('Run only against the demo-bookmyturf Firestore emulator.');
  const env = await initializeTestEnvironment({ projectId: 'demo-bookmyturf', firestore: { host: '127.0.0.1', port: 8080, rules: fs.readFileSync(path.join(__dirname, '../firestore.rules'), 'utf8') } });
  try {
    const admin = getFirestore();
    for (const uid of ['alice', 'bob', 'owner']) await admin.doc(`users/${uid}`).set({ uid, name: uid, email: `${uid}@example.com`, phone: '', location: '', disabled: false });
    await admin.doc('turfs/strike').set({ name: 'Strike', area: 'Gotri', city: 'Vadodara', sports: ['FOOTBALL'], pricePerHour: 800, rating: 4.8, distanceKm: 2, description: 'Test', facilities: [], isIndoor: false, active: true, visualSeed: 0, photoUris: [], openingHour: 6, closingHour: 23 });
    await admin.doc('turfs/hidden').set({ active: false });
    const alice = env.authenticatedContext('alice', { email: 'alice@example.com' }).firestore();
    const owner = env.authenticatedContext('owner', { email: 'owner@example.com', admin: true }).firestore();
    await assertFails(getDoc(doc(env.unauthenticatedContext().firestore(), 'turfs/strike')));
    await assertSucceeds(getDoc(doc(alice, 'turfs/strike')));
    await assertFails(getDoc(doc(alice, 'turfs/hidden')));
    await assertFails(getDoc(doc(alice, 'users/bob')));
    await assertFails(updateDoc(doc(alice, 'users/alice'), { admin: true }));
    await assertFails(updateDoc(doc(alice, 'users/alice'), { disabled: true }));
    await assertSucceeds(updateDoc(doc(alice, 'users/alice'), { name: 'Alice updated' }));
    await assertSucceeds(getDocs(query(collection(alice, 'turfs'), where('active', '==', true))));
    await assertFails(updateDoc(doc(alice, 'turfs/strike'), { pricePerHour: 1 }));
    await assertSucceeds(updateDoc(doc(owner, 'turfs/strike'), { pricePerHour: 900 }));
    await assertFails(setDoc(doc(alice, 'reservations/fake'), { time: '07:00 PM' }));
    await assertFails(setDoc(doc(alice, 'bookings/fake'), { userId: 'alice', amount: 1, status: 'CONFIRMED' }));
    await admin.doc('turfs/strike').update({ pricePerHour: 800 });
    const tomorrow = new Date(); tomorrow.setDate(tomorrow.getDate() + 1);
    const day = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Kolkata', year: 'numeric', month: '2-digit', day: '2-digit' }).format(tomorrow);
    const data = { requestId: '11111111-2222-3333-4444', turfId: 'strike', day, startHour: 19, duration: 1, expectedTotal: 967.6 };
    const request = (uid, body) => ({ auth: { uid, token: { email: `${uid}@example.com` } }, data: body });
    const results = await Promise.allSettled(['alice', 'bob'].map(uid => reserveDemoBooking.run(request(uid, data))));
    assert.equal(results.filter(r => r.status === 'fulfilled').length, 1, 'exactly one concurrent customer wins');
    const winnerIndex = results.findIndex(r => r.status === 'fulfilled');
    const winner = ['alice', 'bob'][winnerIndex];
    const loser = winner === 'alice' ? 'bob' : 'alice';
    const booking = results[winnerIndex].value;
    const retry = await reserveDemoBooking.run(request(winner, data));
    assert.equal(retry.id, booking.id, 'request retries are idempotent');
    const loserDb = env.authenticatedContext(loser, { email: `${loser}@example.com` }).firestore();
    await assertFails(getDoc(doc(loserDb, `bookings/${booking.id}`)));
    const winnerDb = env.authenticatedContext(winner, { email: `${winner}@example.com` }).firestore();
    await assertSucceeds(getDoc(doc(winnerDb, `bookings/${booking.id}`)));
    await assertSucceeds(getDocs(query(collection(winnerDb, 'bookings'), where('userId', '==', winner))));
    await assert.rejects(() => cancelBooking.run(request(loser, { bookingId: booking.id })));
    await cancelBooking.run(request(winner, { bookingId: booking.id }));
    const next = await reserveDemoBooking.run(request(loser, { ...data, requestId: '55555555-6666-7777-8888' }));
    assert.equal(next.status, 'CONFIRMED');
    await admin.doc('users/alice').update({ disabled: true });
    await assertFails(getDoc(doc(alice, 'turfs/strike')));
    console.log('PASS: privacy, admin restrictions, disabled accounts, concurrent booking, idempotency and cancellation.');
  } finally { await env.cleanup(); }
}
main().catch(error => { console.error(error); process.exitCode = 1; });
