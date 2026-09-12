package com.bookmyturf.app.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DemoBookMyTurfRepository : BookMyTurfRepository {
    private val _turfs = MutableStateFlow(seedTurfs())
    override val turfs = _turfs.asStateFlow()

    private val _bookings = MutableStateFlow(seedBookings())
    override val bookings = _bookings.asStateFlow()

    private val _notifications = MutableStateFlow(seedNotifications())
    override val notifications = _notifications.asStateFlow()

    override suspend fun toggleFavorite(turfId: String) {
        delay(120)
        _turfs.update { list -> list.map { if (it.id == turfId) it.copy(isFavorite = !it.isFavorite) else it } }
    }

    override suspend fun createBooking(turf: Turf, date: String, time: String, duration: Int, total: Double): Booking {
        delay(650)
        val booking = Booking(
            id = "BMT-20260910-${(4821 + _bookings.value.size)}",
            turfId = turf.id, turfName = turf.name, date = date, time = time,
            durationHours = duration, amount = total, status = BookingStatus.CONFIRMED
        )
        _bookings.update { listOf(booking) + it }
        _notifications.update {
            listOf(AppNotification("n-new", "Booking confirmed", "${turf.name} is ready for your game.", "Just now", true, "booking")) + it
        }
        return booking
    }

    override suspend fun cancelBooking(bookingId: String) {
        delay(300)
        _bookings.update { list -> list.map { if (it.id == bookingId) it.copy(status = BookingStatus.CANCELLED) else it } }
    }

    override suspend fun setTurfActive(turfId: String, active: Boolean) {
        _turfs.update { list -> list.map { if (it.id == turfId) it.copy(active = active) else it } }
    }

    override suspend fun deleteTurf(turfId: String) {
        _turfs.update { list -> list.filterNot { it.id == turfId } }
    }

    override suspend fun upsertTurf(turf: Turf) {
        delay(150)
        _turfs.update { list -> if (list.any { it.id == turf.id }) list.map { if (it.id == turf.id) turf else it } else listOf(turf) + list }
    }

    companion object {
        fun slots() = listOf(
            TurfSlot("06:00 AM", 600, SlotState.AVAILABLE), TurfSlot("07:00 AM", 600, SlotState.AVAILABLE),
            TurfSlot("08:00 AM", 700, SlotState.BOOKED), TurfSlot("09:00 AM", 700, SlotState.AVAILABLE),
            TurfSlot("10:00 AM", 700, SlotState.BLOCKED), TurfSlot("05:00 PM", 900, SlotState.AVAILABLE),
            TurfSlot("06:00 PM", 900, SlotState.AVAILABLE), TurfSlot("07:00 PM", 1000, SlotState.BOOKED),
            TurfSlot("08:00 PM", 1000, SlotState.AVAILABLE), TurfSlot("09:00 PM", 900, SlotState.AVAILABLE)
        )

        private fun seedTurfs() = listOf(
            Turf("strike", "Strike Arena", "Vadodara", "Gotri", listOf(Sport.FOOTBALL, Sport.CRICKET), 800, 4.8, 2.1,
                "A competition-grade box arena built for fast five-a-side football and power cricket under professional floodlights.",
                listOf("Parking", "Floodlights", "Changing Room", "Washroom", "Drinking Water", "Canteen", "Equipment Rental"), visualSeed = 0),
            Turf("turfx", "TurfX Sports Arena", "Vadodara", "Akota", listOf(Sport.FOOTBALL, Sport.CRICKET), 700, 4.7, 3.4,
                "High-grip synthetic turf with shock-pad flooring and a dedicated players' lounge.",
                listOf("Parking", "Floodlights", "Washroom", "Drinking Water"), isFavorite = true, visualSeed = 1),
            Turf("champions", "Champions Box", "Vadodara", "Alkapuri", listOf(Sport.FOOTBALL), 900, 4.9, 4.2,
                "Premium football box with tournament lighting, digital scoreboard and FIFA-style turf.",
                listOf("Parking", "Floodlights", "Changing Room", "Canteen"), visualSeed = 2),
            Turf("goalzone", "GoalZone Arena", "Ahmedabad", "Bodakdev", listOf(Sport.FOOTBALL, Sport.BASKETBALL), 850, 4.6, 6.8,
                "A versatile urban arena for teams who play hard, late into the night.",
                listOf("Floodlights", "Washroom", "Equipment Rental"), visualSeed = 3),
            Turf("crickethub", "The Cricket Hub", "Surat", "Vesu", listOf(Sport.CRICKET), 650, 4.5, 8.1,
                "Dedicated cricket nets and full box-cricket pitch with automated scoring.",
                listOf("Parking", "Drinking Water", "Equipment Rental"), isIndoor = true, visualSeed = 4),
            Turf("urban", "Urban Sports Club", "Pune", "Baner", listOf(Sport.BADMINTON, Sport.TENNIS, Sport.VOLLEYBALL), 750, 4.8, 9.6,
                "Indoor courts and professional surfaces for serious racquet and team sport sessions.",
                listOf("Parking", "Changing Room", "Washroom", "Canteen"), isIndoor = true, visualSeed = 5)
        )

        private fun seedBookings() = listOf(
            Booking("BMT-20260910-4821", "strike", "Strike Arena", "10 Sep 2026", "07:00 PM – 08:00 PM", 1, 967.60, BookingStatus.CONFIRMED),
            Booking("BMT-20260822-3910", "turfx", "TurfX Sports Arena", "22 Aug 2026", "06:00 PM – 08:00 PM", 2, 1711.20, BookingStatus.COMPLETED),
            Booking("BMT-20260718-2733", "champions", "Champions Box", "18 Jul 2026", "08:00 PM – 09:00 PM", 1, 1085.60, BookingStatus.CANCELLED)
        )

        private fun seedNotifications() = listOf(
            AppNotification("n1", "Booking confirmed", "Strike Arena • 10 Sep at 7:00 PM", "2 min ago", true, "booking"),
            AppNotification("n2", "Your game starts in 2 hours", "Warm up. Hydrate. Dominate.", "Today", true, "reminder"),
            AppNotification("n3", "Payment successful", "₹967.60 paid for BMT-20260910-4821", "Today", false, "payment"),
            AppNotification("n4", "New turf near you", "GoalZone Arena is now live in Vadodara.", "Yesterday", false, "new")
        )
    }
}
