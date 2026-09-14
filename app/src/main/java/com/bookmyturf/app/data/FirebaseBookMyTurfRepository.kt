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
    override val turfs = _turfs.asStateFlow()
    override val bookings = _bookings.asStateFlow()
    override val notifications = _notifications.asStateFlow()
    override val operations = _operations.asStateFlow()
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
        if (isAdmin) listenCollection(db.collection("users"), receiveProfiles)
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
    private fun listenCollection(query: com.google.firebase.firestore.Query, onData: (List<Pair<String, Map<String, Any>>>) -> Unit) {
        val uid = auth.currentUser?.uid
        listeners += query.addSnapshotListener { snapshot, error ->
            if (uid != auth.currentUser?.uid) return@addSnapshotListener
            if (error != null) report(error)
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
    override suspend fun createBooking(turf: Turf, date: String, time: String, duration: Int, total: Double, customer: String): Booking {
        check(CloudFeatures.bookingsEnabled) { CloudFeatures.bookingUnavailable }
        val key = "${turf.id}|$date|$time|$duration|$total"
        val requestId = pendingBooking?.takeIf { it.first == key }?.second ?: UUID.randomUUID().toString().also { pendingBooking = key to it }
        val result = functions.getHttpsCallable("reserveDemoBooking").call(mapOf("requestId" to requestId, "turfId" to turf.id, "day" to bookingDate(date).toString(), "startHour" to bookingStart(time).hour, "duration" to duration, "expectedTotal" to total)).await()
        @Suppress("UNCHECKED_CAST") val data = result.data as? Map<String, Any> ?: error("Invalid booking response.")
        val booking = bookingFrom(data.text("id"), data)
        pendingBooking = null
        return booking
    }
    override suspend fun cancelBooking(bookingId: String) {
        check(CloudFeatures.bookingsEnabled) { CloudFeatures.bookingUnavailable }
        functions.getHttpsCallable("cancelBooking").call(mapOf("bookingId" to bookingId)).await()
    }
    override suspend fun setTurfActive(turfId: String, active: Boolean) { requireAdmin(); db.collection("turfs").document(turfId).update("active", active).await() }
    override suspend fun deleteTurf(turfId: String) { requireAdmin(); db.collection("turfs").document(turfId).delete().await() }
    override suspend fun upsertTurf(turf: Turf) {
        requireAdmin()
        val photos = turf.photoUris.map { source ->
            if (source.startsWith("gs://")) source else {
                check(CloudFeatures.photoUploadsEnabled) { CloudFeatures.photosUnavailable }
                check(source.startsWith("file://") || source.startsWith("content://")) { "Choose a photo from the photo picker." }
                val ref = storage.reference.child("turf-images/${turf.id}/${UUID.randomUUID()}.jpg")
                ref.putFile(Uri.parse(source), com.google.firebase.storage.StorageMetadata.Builder().setContentType("image/jpeg").build()).await()
                "gs://${ref.bucket}/${ref.path}"
            }
        }
        db.collection("turfs").document(turf.id).set(mapOf(
            "name" to turf.name, "area" to turf.area, "city" to turf.city, "sports" to turf.sports.map { it.name },
            "pricePerHour" to turf.pricePerHour, "rating" to turf.rating, "distanceKm" to turf.distanceKm,
            "description" to turf.description, "facilities" to turf.facilities, "isIndoor" to turf.isIndoor,
            "active" to turf.active, "visualSeed" to turf.visualSeed, "photoUris" to photos,
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
    private fun requireAdmin() { check(isAdmin && auth.currentUser != null) { "Administrator access is required." } }

    companion object {
        fun emptyOperations() = OperationsState(offers = emptyList(), reviews = emptyList(), profiles = emptyList(), settings = mapOf("acceptBookings" to false))
        private fun Map<String, Any>.text(key: String) = this[key] as? String ?: ""
        private fun Map<String, Any>.number(key: String) = this[key] as? Number ?: 0
        private fun Map<String, Any>.strings(key: String) = (this[key] as? List<*>)?.filterIsInstance<String>().orEmpty()
        private fun profileFrom(d: Map<String, Any>) = UserProfile(d.text("name"), d.text("phone"), d.text("email"), d.text("location"))
        private fun turfFrom(id: String, d: Map<String, Any>) = Turf(id, d.text("name"), d.text("city"), d.text("area"), d.strings("sports").mapNotNull { runCatching { Sport.valueOf(it) }.getOrNull() }, d.number("pricePerHour").toInt(), d.number("rating").toDouble(), d.number("distanceKm").toDouble(), d.text("description"), d.strings("facilities"), isIndoor = d["isIndoor"] == true, active = d["active"] == true, visualSeed = d.number("visualSeed").toInt(), photoUris = d.strings("photoUris"), openingHour = d.number("openingHour").toInt(), closingHour = d.number("closingHour").toInt())
        private fun bookingFrom(id: String, d: Map<String, Any>) = Booking(id, d.text("turfId"), d.text("turfName"), d.text("date"), d.text("time"), d.number("durationHours").toInt(), d.number("amount").toDouble(), BookingStatus.valueOf(d.text("status")), d.text("customer"))
    }
}
