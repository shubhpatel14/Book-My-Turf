package com.bookmyturf.app.data

import java.io.Serializable
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

val bookingDateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.US)
val slotTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.US)
fun bookingDate(value: String): LocalDate = runCatching { LocalDate.parse(value, bookingDateFormat) }
    .getOrElse { LocalDate.parse(value, DateTimeFormatter.ofPattern("d MMM yyyy", Locale.US)) }
fun bookingStart(value: String): LocalTime = LocalTime.parse(value.substringBefore(" - ").substringBefore(" – ").trim(), slotTimeFormat)

data class VenueOffer(val id: String, val code: String, val detail: String, val active: Boolean = true) : Serializable
data class VenueReview(val id: String, val turfId: String, val author: String, val stars: Int, val text: String, val published: Boolean = true) : Serializable
data class OperationsState(
    val slotOverrides: Map<String, List<TurfSlot>> = emptyMap(),
    val offers: List<VenueOffer> = listOf(VenueOffer("first", "FIRSTGAME", "20% off your first game · Demo offer")),
    val reviews: List<VenueReview> = listOf(
        VenueReview("review-1", "strike", "Rohan Mehta", 5, "Top-quality turf and bright floodlights. Great staff."),
        VenueReview("review-2", "turfx", "Kabir Shah", 4, "Very good pitch. Parking gets busy after 7 PM.")
    ),
    val profiles: List<UserProfile> = listOf(UserProfile()),
    val disabledEmails: Set<String> = emptySet(),
    val settings: Map<String, Boolean> = mapOf("acceptBookings" to true)
) : Serializable

fun venueSlots(turf: Turf, date: String, operations: OperationsState, bookings: List<Booking>): List<TurfSlot> {
    val day = bookingDate(date)
    val overrides = operations.slotOverrides["${turf.id}|$date"].orEmpty().associateBy { it.time }
    return (turf.openingHour until turf.closingHour).map { hour ->
        val time = LocalTime.of(hour, 0)
        val label = time.format(slotTimeFormat)
        val occupied = bookings.any { booking ->
            booking.turfId == turf.id && booking.status != BookingStatus.CANCELLED &&
                runCatching { bookingDate(booking.date) == day && time >= bookingStart(booking.time) && time < bookingStart(booking.time).plusHours(booking.durationHours.toLong()) }.getOrDefault(false)
        }
        val slot = overrides[label] ?: TurfSlot(label, turf.pricePerHour, SlotState.AVAILABLE)
        slot.copy(state = when {
            occupied -> SlotState.BOOKED
            !turf.active || operations.settings["acceptBookings"] == false || day < LocalDate.now() || (day == LocalDate.now() && time <= LocalTime.now()) -> SlotState.BLOCKED
            else -> slot.state
        })
    }
}
