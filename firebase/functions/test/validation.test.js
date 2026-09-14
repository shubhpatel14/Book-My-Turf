const { test } = require("node:test");
const assert = require("node:assert/strict");
const { validateRequest, hourLabel, dateLabel, calculateTotal } = require("../booking-validation");
const now = new Date("2026-09-14T10:00:00Z");
const valid = { requestId: "12345678-abcd-1234-abcd", turfId: "strike", day: "2026-09-15", startHour: 19, duration: 1, expectedTotal: 967.6 };
test("valid request and authoritative totals", () => { assert.equal(validateRequest(valid, now), valid); assert.equal(calculateTotal([800]), 967.6); assert.equal(calculateTotal([800, 900]), 2029.6); });
test("dates and prices reject tampering", () => {
  for (const patch of [{ day: "2026-02-31" }, { day: "2026-09-13" }, { day: "2026-09-22" }, { startHour: 23 }, { duration: 0 }, { duration: 1.5 }, { expectedTotal: NaN }, { turfId: "a/b" }]) assert.throws(() => validateRequest({ ...valid, ...patch }, now));
  assert.throws(() => calculateTotal([-800]));
});
test("today uses India time and rejects a started slot", () => { assert.throws(() => validateRequest({ ...valid, day: "2026-09-14", startHour: 15 }, now)); assert.doesNotThrow(() => validateRequest({ ...valid, day: "2026-09-14", startHour: 16 }, now)); });
test("labels match Android serialization", () => { assert.equal(hourLabel(0), "12:00 AM"); assert.equal(hourLabel(19), "07:00 PM"); assert.equal(dateLabel("2026-09-15"), "15 September 2026"); });
