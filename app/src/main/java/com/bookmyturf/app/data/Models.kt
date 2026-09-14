package com.bookmyturf.app.data

import androidx.compose.runtime.Immutable

@Immutable
enum class Sport(val label: String) {
    FOOTBALL("Football"), CRICKET("Cricket"), BADMINTON("Badminton"),
    BASKETBALL("Basketball"), TENNIS("Tennis"), VOLLEYBALL("Volleyball")
}

@Immutable
data class Turf(
    val id: String,
    val name: String,
    val city: String,
    val area: String,
    val sports: List<Sport>,
    val pricePerHour: Int,
    val rating: Double,
    val distanceKm: Double,
    val description: String,
    val facilities: List<String>,
    val isIndoor: Boolean = false,
    val isFavorite: Boolean = false,
    val active: Boolean = true,
    val visualSeed: Int = 0,
    // Add up to four R.drawable photo resources for the Explore carousel.
    val photoResIds: List<Int> = emptyList()
)

@Immutable
enum class SlotState { AVAILABLE, BOOKED, BLOCKED }
@Immutable
data class TurfSlot(val time: String, val price: Int, val state: SlotState)

@Immutable
enum class BookingStatus { CONFIRMED, COMPLETED, CANCELLED }
@Immutable
data class Booking(
    val id: String,
    val turfId: String,
    val turfName: String,
    val date: String,
    val time: String,
    val durationHours: Int,
    val amount: Double,
    val status: BookingStatus,
    val customer: String = "Arjun Patel"
)

@Immutable
data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    val unread: Boolean,
    val type: String
)

@Immutable
data class UserProfile(
    val name: String = "Arjun Patel",
    val phone: String = "+91 98765 43210",
    val email: String = "arjun.patel@example.com",
    val location: String = "Vadodara, Gujarat"
)

@Immutable
data class DashboardMetric(val label: String, val value: String, val delta: String)

@Immutable
data class PaymentRecord(
    val transactionId: String,
    val bookingId: String,
    val user: String,
    val amount: Double,
    val method: String,
    val status: String,
    val date: String
)

@Immutable
data class AdminUser(
    val name: String, val email: String, val phone: String,
    val bookings: Int, val spend: Int, val active: Boolean
)
