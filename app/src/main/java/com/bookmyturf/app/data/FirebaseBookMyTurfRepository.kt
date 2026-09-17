package com.bookmyturf.app.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

data class CloudSession(val profile: UserProfile, val admin: Boolean)

/** Firestore listeners are scoped to the authenticated account; no local demo fallback. */
class FirebaseBookMyTurfRepository : BookMyTurfRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val functions = FirebaseFunctions.getInstance("asia-south1")
    private val listeners = mutableListOf<ListenerRegistration>()
    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val errors = _errors.asSharedFlow()
    private val _turfs = MutableStateFlow<List<Turf>>(emptyList())
    private val _bookings = MutableStateFlow<List<Booking>>(emptyList())
    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    private val _operations = MutableStateFlow(emptyOperations())
    private val _tournaments = MutableStateFlow<List<Tournament>>(emptyList())
    private val _tournamentRegistrations = MutableStateFlow<List<TournamentRegistration>>(emptyList())
    private val _tournamentSyncUnavailable = MutableStateFlow(false)
    override val turfs = _turfs.asStateFlow()
    override val bookings = _bookings.asStateFlow()
    override val notifications = _notifications.asStateFlow()
    override val operations = _operations.asStateFlow()
    override val tournaments = _tournaments.asStateFlow()
    override val tournamentRegistrations = _tournamentRegistrations.asStateFlow()
    override val tournamentSyncUnavailable = _tournamentSyncUnavailable.asStateFlow()
    private var favorites = emptySet<String>()
    private var venues = emptyList<Turf>()
    private var overrides = emptyMap<String, List<TurfSlot>>()
    private var reservations = emptyMap<String, List<TurfSlot>>()
    private var usersByEmail = emptyMap<String, String>()
    private var isAdmin = false
    private var pendingBooking: Pair<String, String>? = null

    suspend fun signIn(email: String, password: String): CloudSession {
        clearSession()
        auth.signInWithEmailAndPassword(email.trim(), password).await()
        return try { openSession() } catch (e: Exception) { signOut(); throw e }
    }
    suspend fun signInWithGoogle(idToken: String): CloudSession {
        clearSession()
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null)).await()
        return try { openSession() } catch (e: Exception) { signOut(); throw e }
    }
    suspend fun restoreSession(): CloudSession? {
        if (auth.currentUser == null) return null
        clearSession()
        return try { openSession() } catch (e: Exception) { signOut(); throw e }
    }
    suspend fun register(profile: UserProfile, password: String): CloudSession {
        clearSession()
        val user = auth.createUserWithEmailAndPassword(profile.email.trim(), password).await().user ?: error("Account creation failed.")
        user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(profile.name).build()).await()
        // Keep a successfully created Auth account if Firestore setup is incomplete; sign-in can retry profile initialization.
        return try {
            saveProfile(profile)
            openSession()
        } catch (e: Exception) { signOut(); throw IllegalStateException("Account created, but its profile could not be saved. Check Firestore setup, then sign in. ${e.message}", e) }
    }
    suspend fun resetPassword(email: String) { auth.sendPasswordResetEmail(email.trim()).await() }
    fun signOut() { clearSession(); auth.signOut() }
    private fun clearSession() {
        listeners.forEach { it.remove() }; listeners.clear()
        isAdmin = false; usersByEmail = emptyMap(); pendingBooking = null
        venues = emptyList(); favorites = emptySet(); overrides = emptyMap(); reservations = emptyMap()
        _turfs.value = emptyList(); _bookings.value = emptyList(); _notifications.value = emptyList(); _operations.value = emptyOperations()
        _tournaments.value = emptyList(); _tournamentRegistrations.value = emptyList()
        _tournamentSyncUnavailable.value = false
    }
    private suspend fun openSession(): CloudSession {
        val user = auth.currentUser ?: error("Sign in first.")
        isAdmin = user.getIdToken(true).await().claims["admin"] == true
        val ref = db.collection("users").document(user.uid)
        var data = ref.get(com.google.firebase.firestore.Source.SERVER).await().data
        if (data == null) {
            val profile = UserProfile(user.displayName ?: "Player", "", user.email.orEmpty(), "")
            saveProfile(profile)
            data = ref.get(com.google.firebase.firestore.Source.SERVER).await().data
        }
        check(data?.get("disabled") != true) { "This account has been disabled. Contact the administrator." }
        val profile = profileFrom(data.orEmpty())
        listenCollection(db.collection("turfs").let { if (isAdmin) it else it.whereEqualTo("active", true) }) { records ->
            venues = records.map { (id, value) -> turfFrom(id, value) }; publishTurfs()
        }
        listenCollection(ref.collection("favorites")) { favorites = it.map { pair -> pair.first }.toSet(); publishTurfs() }
        listenCollection(db.collection("bookings").let { if (isAdmin) it else it.whereEqualTo("userId", user.uid) }) {
            _bookings.value = it.map { pair -> bookingFrom(pair.first, pair.second) }.sortedByDescending { it.id }
        }
        listenCollection(db.collection("tournaments").let { if (isAdmin) it else it.whereEqualTo("active", true) }, { _tournamentSyncUnavailable.value = true }) {
            _tournaments.value = it.map { (id, data) -> tournamentFrom(id, data) }.sortedBy { tournament -> tournament.date }
        }
        listenCollection(db.collection("tournamentRegistrations").let { if (isAdmin) it else it.whereEqualTo("userId", user.uid) }, { _tournamentSyncUnavailable.value = true }) {
            _tournamentRegistrations.value = it.map { (id, data) -> tournamentRegistrationFrom(id, data) }
        }
        listenCollection(db.collection("notifications").whereEqualTo("userId", user.uid)) {
            _notifications.value = it.map { (id, d) -> AppNotification(id, d.text("title"), d.text("message"), d.text("time"), d["unread"] == true, d.text("type")) }
        }
        listenCollection(db.collection("slots")) { records ->
            overrides = records.groupBy { it.second.text("turfId") + "|" + it.second.text("date") }.mapValues { entry ->
                entry.value.map { (_, d) -> TurfSlot(d.text("time"), d.number("price").toInt(), SlotState.valueOf(d.text("state"))) }
            }; publishSlots()
        }
        listenCollection(db.collection("reservations")) { records ->
            reservations = records.groupBy { it.second.text("turfId") + "|" + it.second.text("date") }.mapValues { entry ->
                entry.value.map { (_, d) -> TurfSlot(d.text("time"), d.number("price").toInt(), SlotState.BOOKED) }
            }; publishSlots()
        }
        listenCollection(db.collection("offers").let { if (isAdmin) it else it.whereEqualTo("active", true) }) {
            _operations.value = _operations.value.copy(offers = it.map { (id, d) -> VenueOffer(id, d.text("code"), d.text("detail"), d["active"] == true) })
        }
        listenCollection(db.collection("reviews").let { if (isAdmin) it else it.whereEqualTo("published", true) }) {
            _operations.value = _operations.value.copy(reviews = it.map { (id, d) -> VenueReview(id, d.text("turfId"), d.text("author"), d.number("stars").toInt(), d.text("text"), d["published"] == true) })
        }
        val receiveProfiles: (List<Pair<String, Map<String, Any>>>) -> Unit = { records ->
            usersByEmail = records.associate { it.second.text("email").lowercase() to it.first }
            _operations.value = _operations.value.copy(profiles = records.map { profileFrom(it.second) }, disabledEmails = records.filter { it.second["disabled"] == true }.map { it.second.text("email").lowercase() }.toSet())
        }
        if (isAdmin) listenCollection(db.collection("users"), onData = receiveProfiles)
        else listeners += ref.addSnapshotListener { snapshot, error ->
            if (auth.currentUser?.uid == user.uid) {
                if (error != null) report(error)
                else receiveProfiles(listOf(user.uid to snapshot?.data.orEmpty()))
            }
        }
        listeners += db.document("config/public").addSnapshotListener { snapshot, error ->
            if (auth.currentUser?.uid == user.uid) {
                if (error != null) report(error) else _operations.value = _operations.value.copy(settings = mapOf("acceptBookings" to (snapshot?.getBoolean("acceptBookings") != false)))
            }
        }
        return CloudSession(profile, isAdmin)
    }
    private fun listenCollection(query: com.google.firebase.firestore.Query, onError: (Exception) -> Unit = ::report, onData: (List<Pair<String, Map<String, Any>>>) -> Unit) {
        val uid = auth.currentUser?.uid
        listeners += query.addSnapshotListener { snapshot, error ->
            if (uid != auth.currentUser?.uid) return@addSnapshotListener
            if (error != null) onError(error)
            else runCatching { onData(snapshot?.documents.orEmpty().map { it.id to it.data.orEmpty() }) }.onFailure(::report)
        }
    }
    private fun report(error: Throwable) { _errors.tryEmit("Cloud sync: ${error.message ?: "Could not load data. Check your connection."}") }
    private fun publishTurfs() { _turfs.value = venues.map { it.copy(isFavorite = it.id in favorites) } }
    private fun publishSlots() {
        val merged = (overrides.keys + reservations.keys).associateWith { key ->
            (overrides[key].orEmpty().associateBy { it.time } + reservations[key].orEmpty().associateBy { it.time }).values.toList()
        }
        _operations.value = _operations.value.copy(slotOverrides = merged)
    }
    override suspend fun toggleFavorite(turfId: String) {
        val uid = auth.currentUser?.uid ?: error("Sign in first.")
        val ref = db.collection("users").document(uid).collection("favorites").document(turfId)
        db.runTransaction { tx -> if (tx.get(ref).exists()) tx.delete(ref) else tx.set(ref, mapOf("turfId" to turfId)); Unit }.await()
    }
    suspend fun saveProfile(profile: UserProfile) {
        val uid = auth.currentUser?.uid ?: error("Sign in first.")
        db.collection("users").document(uid).set(mapOf("uid" to uid, "name" to profile.name, "phone" to profile.phone, "email" to profile.email.lowercase(), "location" to profile.location), SetOptions.merge()).await()
    }
    override suspend fun markNotificationsRead() {
        _notifications.value.filter { it.unread }.chunked(400).forEach { list ->
            db.batch().apply { list.forEach { update(db.collection("notifications").document(it.id), "unread", false) } }.commit().await()
        }
    }
    override suspend fun createBooking(
        turf: Turf,
        date: String,
        time: String,
        duration: Int,
        total: Double,
        customer: String,
        transactionId: String
    ): Booking {
        check(CloudFeatures.bookingsEnabled) { CloudFeatures.bookingUnavailable }
        val uid = auth.currentUser?.uid ?: error("Sign in before booking.")
        val dayStr = bookingDate(date).toString()
        val startHour = bookingStart(time).hour
        val hours = (0 until duration).map { startHour + it }
        val slotIds = hours.map { "${turf.id}_${dayStr}_$it" }
        val generatedBookingId = "BMT-" + UUID.randomUUID().toString().replace("-", "").take(10).uppercase()
        val effectiveTxId = transactionId.ifBlank { "BMT-DEMO-" + UUID.randomUUID().toString().replace("-", "").take(8).uppercase() }
        val customerName = customer.ifBlank { auth.currentUser?.displayName ?: "Player" }

        val createdBooking = Booking(
            id = generatedBookingId,
            turfId = turf.id,
            turfName = turf.name,
            date = date,
            time = time,
            durationHours = duration,
            amount = total,
            status = BookingStatus.CONFIRMED,
            customer = customerName,
            paymentMethod = "DEMO_UPI",
            paymentStatus = "DEMO_PAID",
            transactionId = effectiveTxId
        )

        // One atomic commit prevents partial bookings and double reservation races.
        db.runTransaction { tx ->
            val bookingMap = mapOf(
                "id" to generatedBookingId,
                "userId" to uid,
                "turfId" to turf.id,
                "turfName" to turf.name,
                "date" to date,
                "day" to dayStr,
                "time" to time,
                "startHour" to startHour,
                "durationHours" to duration,
                "amount" to total,
                "status" to "CONFIRMED",
                "customer" to customerName,
                "paymentMethod" to "DEMO_UPI",
                "paymentStatus" to "DEMO_PAID",
                "transactionId" to effectiveTxId,
                "slotIds" to slotIds,
                "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )
            val turfDoc = tx.get(db.collection("turfs").document(turf.id))
            check(turfDoc.exists() && turfDoc.getBoolean("active") == true) { "This turf is not available." }
            val settingsDoc = tx.get(db.document("config/public"))
            check(!settingsDoc.exists() || settingsDoc.getBoolean("acceptBookings") != false) { "New bookings are currently paused." }
            val reservationDocs = slotIds.map { tx.get(db.collection("reservations").document(it)) }
            check(reservationDocs.none { it.exists() }) { "Sorry, this slot was just booked by another user." }
            val slotDocs = slotIds.map { tx.get(db.collection("slots").document(it)) }
            check(slotDocs.none { it.exists() && it.getString("state") == "BLOCKED" }) { "One of these slots is blocked." }

            tx.set(db.collection("bookings").document(generatedBookingId), bookingMap)

            for ((i, key) in slotIds.withIndex()) {
                val hourTime = java.time.LocalTime.of(hours[i], 0).format(slotTimeFormat)
                tx.set(db.collection("reservations").document(key),
                    mapOf(
                        "bookingId" to generatedBookingId,
                        "turfId" to turf.id,
                        "day" to dayStr,
                        "date" to date,
                        "time" to hourTime,
                        "price" to (slotDocs[i].getLong("price")?.toInt() ?: turf.pricePerHour)
                    )
                )
            }

            tx.set(db.collection("notifications").document(generatedBookingId),
                mapOf(
                    "userId" to uid,
                    "title" to "Booking confirmed",
                    "message" to "${turf.name} · $date · $time",
                    "time" to "Just now",
                    "unread" to true,
                    "type" to "booking"
                )
            )
            Unit
        }.await()

        // Always update local memory state so booking appears immediately in UI
        _bookings.value = (listOf(createdBooking) + _bookings.value).distinctBy { it.id }

        // Also register booked slots locally so the UI marks them as booked immediately
        val dayKey = "${turf.id}|$date"
        val newBookedSlots = hours.map { h ->
            val hourTime = java.time.LocalTime.of(h, 0).format(slotTimeFormat)
            TurfSlot(hourTime, turf.pricePerHour, SlotState.BOOKED)
        }
        val existingForDay = reservations[dayKey].orEmpty()
        reservations = reservations + (dayKey to (existingForDay + newBookedSlots).distinctBy { it.time })
        publishSlots()

        // Also add confirmed notification locally
        val notification = AppNotification(
            id = generatedBookingId,
            title = "Booking confirmed",
            message = "${turf.name} · $date · $time",
            time = "Just now",
            unread = true,
            type = "booking"
        )
        _notifications.value = listOf(notification) + _notifications.value

        pendingBooking = null
        return createdBooking
    }

    override suspend fun cancelBooking(bookingId: String) {
        val uid = auth.currentUser?.uid ?: error("Sign in first.")
        try {
            db.runTransaction { tx ->
                val ref = db.collection("bookings").document(bookingId)
                val doc = tx.get(ref)
                if (!doc.exists()) return@runTransaction
                val owner = doc.getString("userId")
                if (owner != uid && !isAdmin) return@runTransaction
                if (doc.getString("status") == "CANCELLED") return@runTransaction
                tx.update(
                    ref,
                    mapOf(
                        "status" to "CANCELLED",
                        "cancelledAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                    )
                )
                @Suppress("UNCHECKED_CAST")
                val slotIds = doc.get("slotIds") as? List<String> ?: emptyList()
                slotIds.forEach { key ->
                    tx.delete(db.collection("reservations").document(key))
                }
            }.await()
        } catch (e: Exception) {
            android.util.Log.w("BookMyTurf", "Firestore cancel sync: ${e.message}")
        }
        _bookings.value = _bookings.value.map {
            if (it.id == bookingId) it.copy(status = BookingStatus.CANCELLED) else it
        }
    }

    override suspend fun setTurfActive(turfId: String, active: Boolean) {
        requireAdmin()
        db.collection("turfs").document(turfId).update("active", active).await()
    }

    override suspend fun deleteTurf(turfId: String) {
        requireAdmin()
        db.collection("turfs").document(turfId).delete().await()
    }

    override suspend fun upsertTurf(turf: Turf) {
        requireAdmin()
        val photos = turf.photoUris.ifEmpty { turf.imageUrls }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .take(4)

        db.collection("turfs").document(turf.id).set(mapOf(
            "name" to turf.name, "area" to turf.area, "city" to turf.city, "sports" to turf.sports.map { it.name },
            "pricePerHour" to turf.pricePerHour, "rating" to turf.rating, "distanceKm" to turf.distanceKm,
            "description" to turf.description, "facilities" to turf.facilities, "isIndoor" to turf.isIndoor,
            "active" to turf.active, "visualSeed" to turf.visualSeed,
            "photoUris" to photos,
            "imageUrls" to photos,
            "openingHour" to turf.openingHour, "closingHour" to turf.closingHour
        )).await()
    }

    override suspend fun updateOperations(change: (OperationsState) -> OperationsState) {
        requireAdmin()
        val before = _operations.value
        val after = change(before)
        val batch = db.batch()
        after.offers.filter { it !in before.offers }.forEach { batch.set(db.collection("offers").document(it.id), mapOf("code" to it.code, "detail" to it.detail, "active" to it.active)) }
        after.reviews.filter { it !in before.reviews }.forEach { batch.set(db.collection("reviews").document(it.id), mapOf("turfId" to it.turfId, "author" to it.author, "stars" to it.stars, "text" to it.text, "published" to it.published)) }
        if (after.settings != before.settings) batch.set(db.document("config/public"), after.settings, SetOptions.merge())
        (after.disabledEmails + before.disabledEmails).filter { (it in after.disabledEmails) != (it in before.disabledEmails) }.forEach { email ->
            val uid = usersByEmail[email] ?: error("User not found. Refresh and try again.")
            batch.update(db.collection("users").document(uid), "disabled", email in after.disabledEmails)
        }
        after.slotOverrides.filter { (key, value) -> before.slotOverrides[key] != value }.forEach { (key, slots) ->
            val turfId = key.substringBefore('|'); val date = key.substringAfter('|'); val day = bookingDate(date).toString()
            slots.filter { it.state != SlotState.BOOKED }.forEach { slot ->
                val hour = bookingStart(slot.time).hour
                batch.set(db.collection("slots").document("${turfId}_${day}_$hour"), mapOf("turfId" to turfId, "date" to date, "day" to day, "time" to slot.time, "price" to slot.price, "state" to slot.state.name))
            }
        }
        batch.commit().await()
    }

    override suspend fun upsertTournament(tournament: Tournament) {
        requireAdmin()
        validateTournament(tournament)
        val id = tournament.id.ifBlank { UUID.randomUUID().toString() }
        val ref = db.collection("tournaments").document(id)
        val previous = ref.get().await()
        val registeredTeams = previous.getLong("registeredTeams") ?: 0L
        require(tournament.maxTeams >= registeredTeams) { "Maximum teams cannot be below existing registrations." }
        ref.set(mapOf(
            "name" to tournament.name.trim(), "sport" to tournament.sport.name,
            "city" to tournament.city.trim(), "venue" to tournament.venue.trim(),
            "date" to tournament.date, "registrationDeadline" to tournament.registrationDeadline,
            "teamSize" to tournament.teamSize, "maxTeams" to tournament.maxTeams,
            "entryFee" to tournament.entryFee, "prizePool" to tournament.prizePool,
            "description" to tournament.description.trim(), "rules" to tournament.rules.trim(),
            "active" to tournament.active, "registeredTeams" to registeredTeams,
            "imageUrls" to tournament.imageUrls
        )).await()
    }

    override suspend fun registerTournament(tournament: Tournament, teamName: String, captainName: String, captainPhone: String, transactionId: String): TournamentRegistration {
        val uid = auth.currentUser?.uid ?: error("Sign in first.")
        validateTournamentRegistration(tournament, teamName, captainName, captainPhone)
        require(transactionId.matches(Regex("BMT-DEMO-[A-Z0-9]{8}"))) { "Complete the demo payment first." }
        val tournamentRef = db.collection("tournaments").document(tournament.id)
        val registrationRef = db.collection("tournamentRegistrations").document("${uid}_${tournament.id}")
        val registration = db.runTransaction { tx ->
            val current = tx.get(tournamentRef)
            val latest = tournamentFrom(current.id, current.data ?: error("Tournament unavailable."))
            validateTournamentRegistration(latest, teamName, captainName, captainPhone)
            val registered = current.getLong("registeredTeams") ?: 0L
            check(registered < latest.maxTeams) { "This tournament is full." }
            val saved = TournamentRegistration(registrationRef.id, latest.id, latest.name, latest.sport, teamName.trim(), captainName.trim(), captainPhone.trim(), latest.entryFee, transactionId, userId = uid)
            tx.update(tournamentRef, "registeredTeams", registered + 1)
            tx.set(registrationRef, mapOf(
                "userId" to uid, "tournamentId" to latest.id, "tournamentName" to latest.name,
                "sport" to latest.sport.name, "teamName" to saved.teamName,
                "captainName" to saved.captainName, "captainPhone" to saved.captainPhone,
                "amount" to saved.amount,
                "transactionId" to transactionId, "paymentStatus" to "DEMO_PAID",
                "status" to "CONFIRMED", "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            ))
            saved
        }.await()
        return registration
    }

    override suspend fun setTournamentActive(id: String, active: Boolean) {
        requireAdmin()
        db.collection("tournaments").document(id).update("active", active).await()
    }

    override suspend fun setTournamentRegistrationStatus(id: String, status: String) {
        requireAdmin()
        require(status in listOf("CONFIRMED", "CANCELLED"))
        val registrationRef = db.collection("tournamentRegistrations").document(id)
        db.runTransaction { tx ->
            val registration = tx.get(registrationRef)
            check(registration.exists()) { "Registration not found." }
            val oldStatus = registration.getString("status") ?: "CONFIRMED"
            if (oldStatus != status) {
                val tournamentRef = db.collection("tournaments").document(registration.getString("tournamentId") ?: error("Tournament missing."))
                val tournament = tx.get(tournamentRef)
                val current = tournament.getLong("registeredTeams") ?: 0L
                val next = current + if (status == "CONFIRMED") 1 else -1
                check(next >= 0 && next <= (tournament.getLong("maxTeams") ?: 0L)) { "Tournament capacity has changed." }
                tx.update(tournamentRef, "registeredTeams", next)
                tx.update(registrationRef, "status", status)
            }
        }.await()
    }

    private fun requireAdmin() { check(isAdmin && auth.currentUser != null) { "Administrator access is required." } }

    companion object {
        fun emptyOperations() = OperationsState(offers = emptyList(), reviews = emptyList(), profiles = emptyList(), settings = mapOf("acceptBookings" to true))
        private fun Map<String, Any>.text(key: String) = this[key] as? String ?: ""
        private fun Map<String, Any>.number(key: String) = this[key] as? Number ?: 0
        private fun Map<String, Any>.strings(key: String) = (this[key] as? List<*>)?.filterIsInstance<String>().orEmpty()
        private fun profileFrom(d: Map<String, Any>) = UserProfile(d.text("name"), d.text("phone"), d.text("email"), d.text("location"))
        private fun turfFrom(id: String, d: Map<String, Any>): Turf {
            val urls = d.strings("imageUrls").ifEmpty { d.strings("photoUris") }
            return Turf(
                id, d.text("name"), d.text("city"), d.text("area"),
                d.strings("sports").mapNotNull { runCatching { Sport.valueOf(it) }.getOrNull() },
                d.number("pricePerHour").toInt(), d.number("rating").toDouble(), d.number("distanceKm").toDouble(),
                d.text("description"), d.strings("facilities"),
                isIndoor = d["isIndoor"] == true, active = d["active"] == true, visualSeed = d.number("visualSeed").toInt(),
                photoUris = urls,
                openingHour = d.number("openingHour").toInt(), closingHour = d.number("closingHour").toInt()
            )
        }
        private fun bookingFrom(id: String, d: Map<String, Any>) = Booking(
            id, d.text("turfId"), d.text("turfName"), d.text("date"), d.text("time"),
            d.number("durationHours").toInt(), d.number("amount").toDouble(),
            runCatching { BookingStatus.valueOf(d.text("status")) }.getOrDefault(BookingStatus.CONFIRMED),
            d.text("customer").ifBlank { "Player" },
            paymentMethod = d.text("paymentMethod").ifBlank { "DEMO_UPI" },
            paymentStatus = d.text("paymentStatus").ifBlank { "DEMO_PAID" },
            transactionId = d.text("transactionId")
        )
        private fun tournamentFrom(id: String, d: Map<String, Any>) = Tournament(
            id, d.text("name"), runCatching { Sport.valueOf(d.text("sport")) }.getOrDefault(Sport.FOOTBALL),
            d.text("city"), d.text("venue"), d.text("date"), d.text("registrationDeadline"),
            d.number("teamSize").toInt(), d.number("maxTeams").toInt(), d.number("entryFee").toInt(),
            d.number("prizePool").toInt(), d.text("description"), d.text("rules"), d["active"] == true,
            d.strings("imageUrls")
        )
        private fun tournamentRegistrationFrom(id: String, d: Map<String, Any>) = TournamentRegistration(
            id, d.text("tournamentId"), d.text("tournamentName"),
            runCatching { Sport.valueOf(d.text("sport")) }.getOrDefault(Sport.FOOTBALL),
            d.text("teamName"), d.text("captainName"), d.text("captainPhone"),
            d.number("amount").toInt(), d.text("transactionId"), d.text("paymentStatus"),
            d.text("status"), d.text("userId")
        )
    }
}
