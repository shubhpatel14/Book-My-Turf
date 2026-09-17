package com.bookmyturf.app.data

import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private data class StoreSnapshot(
    val turfs: List<Turf>, val bookings: List<Booking>, val notifications: List<AppNotification>,
    val operations: OperationsState = OperationsState()
) : Serializable

/** One atomic, persistent source for all screens. No network backend is configured. */
class DemoBookMyTurfRepository(private val storeFile: File? = null) : BookMyTurfRepository {
    private val tournamentState = MutableStateFlow(sampleTournaments())
    private val registrationState = MutableStateFlow<List<TournamentRegistration>>(emptyList())
    override val tournaments = tournamentState
    override val tournamentRegistrations = registrationState
    override val tournamentSyncUnavailable = MutableStateFlow(false)
    private val initial = if (storeFile?.exists() == true)
        ObjectInputStream(storeFile.inputStream().buffered()).use { it.readObject() as StoreSnapshot }
        else StoreSnapshot(seedTurfs(), seedBookings(), seedNotifications())
    private val state = MutableStateFlow(initial)
    private val mutex = Mutex()
    override val turfs = state.map { it.turfs }.distinctUntilChanged()
    override val bookings = state.map { it.bookings }.distinctUntilChanged()
    override val notifications = state.map { it.notifications }.distinctUntilChanged()
    override val operations = state.map { it.operations }.distinctUntilChanged()

    override suspend fun upsertTournament(tournament: Tournament) {
        validateTournament(tournament)
        tournamentState.value = (tournamentState.value.filterNot { it.id == tournament.id } + tournament).sortedBy { it.date }
    }
    override suspend fun registerTournament(tournament: Tournament, teamName: String, captainName: String, captainPhone: String, transactionId: String): TournamentRegistration {
        val current = tournamentState.value.firstOrNull { it.id == tournament.id } ?: error("Tournament unavailable.")
        validateTournamentRegistration(current, teamName, captainName, captainPhone)
        require(registrationState.value.count { it.tournamentId == current.id && it.status == "CONFIRMED" } < current.maxTeams) { "This tournament is full." }
        val registration = TournamentRegistration(UUID.randomUUID().toString(), current.id, current.name, current.sport, teamName.trim(), captainName.trim(), captainPhone.trim(), current.entryFee, transactionId)
        registrationState.value = listOf(registration) + registrationState.value
        return registration
    }
    override suspend fun setTournamentActive(id: String, active: Boolean) { tournamentState.value = tournamentState.value.map { if (it.id == id) it.copy(active = active) else it } }
    override suspend fun setTournamentRegistrationStatus(id: String, status: String) {
        require(status in listOf("CONFIRMED", "CANCELLED"))
        registrationState.value = registrationState.value.map { if (it.id == id) it.copy(status = status) else it }
    }

    private suspend fun mutate(change: (StoreSnapshot) -> StoreSnapshot) = mutex.withLock {
        val next = change(state.value)
        withContext(Dispatchers.IO) {
            storeFile?.let { target ->
                target.parentFile?.mkdirs()
                val temporary = File(target.parentFile, target.name + ".pending")
                FileOutputStream(temporary).use { stream ->
                    val output = ObjectOutputStream(stream)
                    output.writeObject(next)
                    output.flush()
                    stream.fd.sync()
                }
                Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            }
        }
        state.value = next
    }

    override suspend fun updateOperations(change: (OperationsState) -> OperationsState) {
        mutate { it.copy(operations = change(it.operations)) }
    }
    override suspend fun markNotificationsRead() {
        mutate { it.copy(notifications = it.notifications.map { n -> n.copy(unread = false) }) }
    }
    override suspend fun toggleFavorite(turfId: String) {
        mutate { it.copy(turfs = it.turfs.map { turf -> if (turf.id == turfId) turf.copy(isFavorite = !turf.isFavorite) else turf }) }
    }
    override suspend fun createBooking(turf: Turf, date: String, time: String, duration: Int, total: Double, customer: String, transactionId: String): Booking {
        var created: Booking? = null
        val txId = transactionId.ifBlank { "BMT-DEMO-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase() }
        mutate { current ->
            val latest = current.turfs.firstOrNull { it.id == turf.id } ?: error("This venue is no longer available.")
            require(latest.active && duration > 0) { "Select an active venue and available slots." }
            val available = venueSlots(latest, date, current.operations, current.bookings)
            val start = available.indexOfFirst { it.time == bookingStart(time).format(slotTimeFormat) }
            require(start >= 0 && start + duration <= available.size) { "These hours are outside the venue schedule." }
            val selected = available.subList(start, start + duration)
            require(selected.all { it.state == SlotState.AVAILABLE }) { "A selected slot is no longer available. Choose again." }
            val expected = (selected.sumOf { it.price } + 20.0) * 1.18
            require(kotlin.math.abs(total - expected) < .02) { "Pricing changed. Please select your slots again." }
            val booking = Booking("BMT-" + UUID.randomUUID().toString().take(8).uppercase(), latest.id, latest.name, date, time, duration, expected, BookingStatus.CONFIRMED, customer, paymentMethod = "DEMO_UPI", paymentStatus = "DEMO_PAID", transactionId = txId)
            created = booking
            current.copy(bookings = listOf(booking) + current.bookings,
                notifications = listOf(AppNotification(UUID.randomUUID().toString(), "Booking confirmed", "${latest.name} · $date · $time", "Just now", true, "booking")) + current.notifications)
        }
        return checkNotNull(created)
    }
    override suspend fun cancelBooking(bookingId: String) {
        mutate { it.copy(bookings = it.bookings.map { b -> if (b.id == bookingId && b.status == BookingStatus.CONFIRMED) b.copy(status = BookingStatus.CANCELLED) else b }) }
    }
    override suspend fun setTurfActive(turfId: String, active: Boolean) {
        mutate { it.copy(turfs = it.turfs.map { t -> if (t.id == turfId) t.copy(active = active) else t }) }
    }
    override suspend fun deleteTurf(turfId: String) {
        mutate { it.copy(turfs = it.turfs.filterNot { t -> t.id == turfId }) }
    }
    override suspend fun upsertTurf(turf: Turf) {
        require(turf.name.isNotBlank() && turf.city.isNotBlank() && turf.area.isNotBlank()) { "Name, area and city are required." }
        require(turf.pricePerHour > 0 && turf.sports.isNotEmpty() && turf.openingHour in 0..22 && turf.closingHour in (turf.openingHour + 1)..23) { "Check price, sports and opening hours." }
        require(turf.rating in 0.0..5.0 && turf.distanceKm >= 0 && turf.photoUris.size <= 4)
        mutate { current ->
            val saved = turf.copy(isFavorite = current.turfs.firstOrNull { it.id == turf.id }?.isFavorite ?: turf.isFavorite)
            current.copy(turfs = if (current.turfs.any { it.id == turf.id }) current.turfs.map { if (it.id == turf.id) saved else it } else listOf(saved) + current.turfs)
        }
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
