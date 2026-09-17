const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { join } = require('node:path');
const { initializeTestEnvironment, assertSucceeds, assertFails } = require('@firebase/rules-unit-testing');
const { doc, getDoc, runTransaction, setDoc } = require('firebase/firestore');

test('a signed-in captain can register and reserve one team place', async () => {
  const environment = await initializeTestEnvironment({
    projectId: 'demo-bookmyturf-tournaments',
    firestore: { rules: readFileSync(join(__dirname, '../../firestore.rules'), 'utf8') },
  });
  try {
    await environment.withSecurityRulesDisabled(async (context) => {
      await setDoc(doc(context.firestore(), 'tournaments', 'cup'), {
        name: 'Demo Cup', sport: 'FOOTBALL', city: 'Vadodara', venue: 'Demo Arena',
        date: '2026-10-10', registrationDeadline: '2026-10-07', teamSize: 2,
        maxTeams: 4, entryFee: 100, prizePool: 1000, description: '', rules: '',
        active: true, registeredTeams: 0,
      });
    });
    const db = environment.authenticatedContext('captain-1').firestore();
    const tournament = doc(db, 'tournaments', 'cup');
    const registration = doc(db, 'tournamentRegistrations', 'captain-1_cup');
    await assertFails(getDoc(registration));
    await assertSucceeds(runTransaction(db, async (transaction) => {
      const event = await transaction.get(tournament);
      transaction.update(tournament, { registeredTeams: event.data().registeredTeams + 1 });
      transaction.set(registration, {
        userId: 'captain-1', tournamentId: 'cup', tournamentName: 'Demo Cup',
        sport: 'FOOTBALL', teamName: 'Test Team', captainName: 'Test Captain',
        captainPhone: '9876543210',
        amount: 100, transactionId: 'BMT-DEMO-12345678',
        paymentStatus: 'DEMO_PAID', status: 'CONFIRMED', createdAt: new Date(),
      });
    }));
    await assertFails(runTransaction(db, async (transaction) => {
      const event = await transaction.get(tournament);
      transaction.update(tournament, { registeredTeams: event.data().registeredTeams + 1 });
      transaction.set(registration, { status: 'CONFIRMED' });
    }));
  } finally {
    await environment.cleanup();
  }
});
