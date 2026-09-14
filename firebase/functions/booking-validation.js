"use strict";
const dateFormat = new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Kolkata", year: "numeric", month: "2-digit", day: "2-digit" });
const hourFormat = new Intl.DateTimeFormat("en-GB", { timeZone: "Asia/Kolkata", hour: "2-digit", hourCycle: "h23" });
function validateRequest(data, now = new Date()) {
  if (!data || typeof data.requestId !== "string" || !/^[a-zA-Z0-9-]{16,64}$/.test(data.requestId)) throw new Error("Invalid request identifier.");
  if (typeof data.turfId !== "string" || !/^[a-zA-Z0-9_-]{1,128}$/.test(data.turfId)) throw new Error("Invalid venue.");
  if (typeof data.day !== "string" || !/^\d{4}-\d{2}-\d{2}$/.test(data.day)) throw new Error("Invalid date.");
  const parsed = new Date(data.day + "T00:00:00Z");
  if (!Number.isFinite(parsed.getTime()) || parsed.toISOString().slice(0, 10) !== data.day) throw new Error("Invalid date.");
  const today = dateFormat.format(now);
  const last = new Date(today + "T00:00:00Z"); last.setUTCDate(last.getUTCDate() + 6);
  if (data.day < today || data.day > last.toISOString().slice(0, 10)) throw new Error("Choose a date in the next seven days.");
  if (!Number.isInteger(data.startHour) || !Number.isInteger(data.duration) || data.startHour < 0 || data.duration < 1 || data.duration > 23 || data.startHour + data.duration > 23) throw new Error("Invalid booking hours.");
  if (data.day === today && data.startHour <= Number(hourFormat.format(now))) throw new Error("This time has already passed.");
  if (!Number.isFinite(data.expectedTotal) || data.expectedTotal <= 0) throw new Error("Invalid total.");
  return data;
}
function hourLabel(hour) { return `${String(hour % 12 || 12).padStart(2, "0")}:00 ${hour < 12 ? "AM" : "PM"}`; }
function dateLabel(day) { return new Intl.DateTimeFormat("en-GB", { timeZone: "UTC", day: "numeric", month: "long", year: "numeric" }).format(new Date(day + "T00:00:00Z")); }
function calculateTotal(prices) {
  if (!prices.length || prices.some(price => !Number.isInteger(price) || price <= 0 || price > 1000000)) throw new Error("Invalid venue pricing.");
  return Math.round((prices.reduce((a, b) => a + b, 0) + 20) * 118) / 100;
}
module.exports = { validateRequest, hourLabel, dateLabel, calculateTotal };
