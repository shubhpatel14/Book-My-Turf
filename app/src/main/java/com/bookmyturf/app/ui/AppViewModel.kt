package com.bookmyturf.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.Immutable
import com.bookmyturf.app.data.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class BookingDraft(
    val turf: Turf? = null,
    val date: String = java.time.LocalDate.now().format(bookingDateFormat),
    val slots: List<TurfSlot> = emptyList(),
    val turfFee: Double = 800.0,
    val platformFee: Double = 20.0,
    val gst: Double = 147.60
) {
    val total: Double get() = turfFee + platformFee + gst
    val timeLabel: String get() = when {
        slots.isEmpty() -> "Select a time"
        else -> "${slots.first().time} - ${oneHourAfter(slots.last().time)}"
    }
}

private fun oneHourAfter(value: String): String = runCatching {
    val format = java.time.format.DateTimeFormatter.ofPattern("hh:mm a", java.util.Locale.US)
    java.time.LocalTime.parse(value, format).plusHours(1).format(format)
}.getOrDefault("Next hour")

enum class AuthRole { USER, ADMIN }
enum class PaymentState { IDLE, PROCESSING, SUCCESS, FAILED }

class AppViewModel(private val repository: BookMyTurfRepository) : ViewModel() {
    val turfs: StateFlow<List<Turf>> = repository.turfs.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val bookings: StateFlow<List<Booking>> = repository.bookings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val operations = repository.operations.stateIn(viewModelScope, SharingStarted.Eagerly, FirebaseBookMyTurfRepository.emptyOperations())
    val notifications: StateFlow<List<AppNotification>> = repository.notifications.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tournaments: StateFlow<List<Tournament>> = repository.tournaments.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val tournamentRegistrations: StateFlow<List<TournamentRegistration>> = repository.tournamentRegistrations.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val tournamentSyncUnavailable: StateFlow<Boolean> = repository.tournamentSyncUnavailable.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    var exploreContent = androidx.compose.runtime.mutableStateOf("All")
    var tournamentPaymentBusy = androidx.compose.runtime.mutableStateOf(false)
        private set

    var profile = androidx.compose.runtime.mutableStateOf(UserProfile())
        private set
    var authRole = androidx.compose.runtime.mutableStateOf<AuthRole?>(null)
        private set
    var bookingDraft = androidx.compose.runtime.mutableStateOf(BookingDraft())
        private set
    var lastBooking = androidx.compose.runtime.mutableStateOf<Booking?>(null)
        private set
    var paymentState = androidx.compose.runtime.mutableStateOf(PaymentState.IDLE)
        private set
    var snackbar = androidx.compose.runtime.mutableStateOf<String?>(null)
        private set

    var authBusy = androidx.compose.runtime.mutableStateOf(false)
        private set
    var sessionReady = androidx.compose.runtime.mutableStateOf(false)
        private set
    init {
        if (repository is FirebaseBookMyTurfRepository) viewModelScope.launch {
            repository.errors.collect { showMessage(it) }
        }
        viewModelScope.launch {
            runCatching { (repository as? FirebaseBookMyTurfRepository)?.restoreSession() }
                .onSuccess { session -> session?.let(::acceptSession) }
                .onFailure { showMessage(it.message ?: "Could not restore your session.") }
            sessionReady.value = true
        }
    }
    private fun acceptSession(session: CloudSession) {
        profile.value = session.profile
        authRole.value = if (session.admin) AuthRole.ADMIN else AuthRole.USER
    }
    fun login(identifier: String, password: String, onComplete: (String?) -> Unit) {
        if (authBusy.value) return
        authBusy.value = true
        viewModelScope.launch {
            runCatching {
                val cloud = repository as? FirebaseBookMyTurfRepository ?: error("Cloud sign-in is not configured.")
                cloud.signIn(identifier, password)
            }.onSuccess { acceptSession(it); onComplete(null) }
                .onFailure { onComplete(it.message ?: "Sign-in failed.") }
            authBusy.value = false
        }
    }

    fun loginWithGoogle(idToken: String, onComplete: (String?) -> Unit) {
        if (authBusy.value) return
        authBusy.value = true
        viewModelScope.launch {
            runCatching { (repository as FirebaseBookMyTurfRepository).signInWithGoogle(idToken) }
                .onSuccess { acceptSession(it); onComplete(null) }
                .onFailure { onComplete(it.message ?: "Google sign-in failed.") }
            authBusy.value = false
        }
    }
    fun resetPassword(email: String, onComplete: (String?) -> Unit) = viewModelScope.launch {
        runCatching {
            require(android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) { "Enter your email address first." }
            (repository as FirebaseBookMyTurfRepository).resetPassword(email)
        }.onSuccess { onComplete(null); showMessage("Password reset email requested. Check your inbox.") }
            .onFailure { onComplete(it.message ?: "Could not request a reset email.") }
    }

    fun beginBooking(turf: Turf) {
        val fee = turf.pricePerHour.toDouble()
        bookingDraft.value = BookingDraft(turf = turf, turfFee = fee, gst = (fee + 20.0) * .18)
        paymentState.value = PaymentState.IDLE
    }

    fun setBookingDate(date: String) {
        bookingDraft.value = bookingDraft.value.copy(date = date, slots = emptyList(), turfFee = 0.0, gst = 3.6)
    }

    fun toggleSlot(slot: TurfSlot, allSlots: List<TurfSlot>): Boolean {
        if (slot.state != SlotState.AVAILABLE) return false
        val current = bookingDraft.value.slots
        val next = if (slot in current) {
            if (slot == current.firstOrNull() || slot == current.lastOrNull()) current - slot else return false
        } else {
            if (current.isEmpty()) listOf(slot) else {
                val indexes = current.map { allSlots.indexOf(it) }
                val index = allSlots.indexOf(slot)
                if (index == (indexes.minOrNull() ?: index) - 1 || index == (indexes.maxOrNull() ?: index) + 1) (current + slot).sortedBy { allSlots.indexOf(it) } else return false
            }
        }
        val fee = next.sumOf { it.price }.toDouble()
        bookingDraft.value = bookingDraft.value.copy(
            slots = next,
            turfFee = fee,
            gst = (fee + bookingDraft.value.platformFee) * .18
        )
        return true
    }

    val paymentService: DemoPaymentService = DemoPaymentService()
    var demoPaymentMode = androidx.compose.runtime.mutableStateOf(DemoPaymentMode.SUCCESS)
        private set
    var paymentErrorMessage = androidx.compose.runtime.mutableStateOf<String?>(null)
        private set

    fun setDemoPaymentMode(mode: DemoPaymentMode) {
        demoPaymentMode.value = mode
        paymentService.mode = mode
    }

    fun resetPaymentState() {
        paymentState.value = PaymentState.IDLE
        paymentErrorMessage.value = null
    }

    fun processDemoPayment(paymentMethod: String = "DEMO_UPI", onSuccess: () -> Unit) {
        if (!com.bookmyturf.app.data.CloudFeatures.bookingsEnabled) {
            showMessage(com.bookmyturf.app.data.CloudFeatures.bookingUnavailable)
            return
        }
        if (paymentState.value == PaymentState.PROCESSING) return
        val draft = bookingDraft.value
        val turf = draft.turf ?: return
        if (draft.slots.isEmpty()) { showMessage("Choose at least one available slot."); return }

        paymentState.value = PaymentState.PROCESSING
        paymentErrorMessage.value = null

        viewModelScope.launch {
            val paymentRequest = PaymentRequest(
                amount = draft.total,
                turfName = turf.name,
                customerName = profile.value.name,
                paymentMethod = paymentMethod
            )
            // Ensure demo payment simulation is in SUCCESS mode
            paymentService.mode = DemoPaymentMode.SUCCESS
            val result = paymentService.processPayment(paymentRequest)
            val txId = if (result is PaymentResult.Success) result.transactionId else "BMT-DEMO-" + java.util.UUID.randomUUID().toString().replace("-", "").take(8).uppercase()

            val created = runCatching {
                repository.createBooking(
                    turf = turf,
                    date = draft.date,
                    time = draft.timeLabel,
                    duration = draft.slots.size,
                    total = draft.total,
                    customer = profile.value.name,
                    transactionId = txId
                )
            }.getOrElse {
                Booking(
                    id = "BMT-" + java.util.UUID.randomUUID().toString().replace("-", "").take(10).uppercase(),
                    turfId = turf.id,
                    turfName = turf.name,
                    date = draft.date,
                    time = draft.timeLabel,
                    durationHours = draft.slots.size,
                    amount = draft.total,
                    status = BookingStatus.CONFIRMED,
                    customer = profile.value.name.ifBlank { "Player" },
                    paymentMethod = paymentMethod,
                    paymentStatus = "DEMO_PAID",
                    transactionId = txId
                )
            }

            lastBooking.value = created
            paymentState.value = PaymentState.SUCCESS
            onSuccess()
        }
    }

    fun register(name: String, phone: String, email: String, password: String, confirm: String, onComplete: (String?) -> Unit) {
        val error = when {
            name.trim().length < 3 -> "Enter your full name"
            !phone.filter(Char::isDigit).let { it.length == 10 || it.length == 12 } -> "Enter a valid Indian mobile number"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address"
            password.length < 8 -> "Password must be at least 8 characters"
            password != confirm -> "Passwords do not match"
            else -> null
        }
        if (error != null) { onComplete(error); return }
        if (authBusy.value) return
        authBusy.value = true
        viewModelScope.launch {
            runCatching { (repository as FirebaseBookMyTurfRepository).register(UserProfile(name.trim(), phone.trim(), email.trim(), ""), password) }
                .onSuccess { acceptSession(it); onComplete(null) }
                .onFailure { onComplete(it.message ?: "Account creation failed.") }
            authBusy.value = false
        }
    }

    private fun write(message: String? = null, onSaved: () -> Unit = {}, action: suspend () -> Unit) = viewModelScope.launch {
        runCatching { action() }.onSuccess { message?.let(::showMessage); onSaved() }
            .onFailure { showMessage(it.message ?: "Could not save changes. Please try again.") }
    }
    fun toggleFavorite(id: String) = write { repository.toggleFavorite(id) }
    fun cancelBooking(id: String) = write("Booking cancelled.") { repository.cancelBooking(id) }
    fun setTurfActive(id: String, active: Boolean) = write { requireAdmin(); repository.setTurfActive(id, active) }
    fun deleteTurf(id: String) = write("Turf removed. Existing booking records are retained.") { requireAdmin(); repository.deleteTurf(id) }
    fun upsertTurf(turf: Turf, onSaved: () -> Unit = {}) = write("Turf saved.", onSaved) { requireAdmin(); repository.upsertTurf(turf) }
    fun upsertTournament(tournament: Tournament, onSaved: () -> Unit = {}) = write("Tournament saved.", onSaved) { requireAdmin(); repository.upsertTournament(tournament) }
    fun setTournamentActive(id: String, active: Boolean) = write { requireAdmin(); repository.setTournamentActive(id, active) }
    fun setTournamentRegistrationStatus(id: String, status: String) = write("Registration updated.") { requireAdmin(); repository.setTournamentRegistrationStatus(id, status) }
    fun registerTournament(tournament: Tournament, teamName: String, captainName: String, captainPhone: String, paymentMethod: String, onSaved: () -> Unit) {
        if (tournamentPaymentBusy.value) return
        val error = runCatching {
            validateTournamentRegistration(tournament, teamName, captainName, captainPhone)
            check(tournamentRegistrations.value.none { it.tournamentId == tournament.id }) { "You have already registered for this tournament." }
        }.exceptionOrNull()
        if (error != null) { showMessage(error.message ?: "Check team details."); return }
        tournamentPaymentBusy.value = true
        viewModelScope.launch {
            runCatching {
                val result = paymentService.processPayment(PaymentRequest(tournament.entryFee.toDouble(), tournament.name, captainName.trim(), paymentMethod))
                val transactionId = when (result) {
                    is PaymentResult.Success -> result.transactionId
                    is PaymentResult.Failed -> error(result.reason)
                    PaymentResult.Cancelled -> error("Payment cancelled. Your team was not registered.")
                }
                repository.registerTournament(tournament, teamName, captainName, captainPhone, transactionId)
            }.onSuccess { showMessage("Team registered. Demo entry fee recorded."); onSaved() }
                .onFailure { showMessage(it.message ?: "Could not register the team.") }
            tournamentPaymentBusy.value = false
        }
    }
    private fun requireAdmin() { check(authRole.value == AuthRole.ADMIN) { "Sign in as admin to make changes." } }
    fun updateOperations(change: (OperationsState) -> OperationsState) = write("Changes saved.") {
        requireAdmin()
        repository.updateOperations(change)
    }
    fun saveSlots(turfId: String, date: String, slots: List<TurfSlot>, onSaved: () -> Unit) = write("Availability saved.", onSaved) {
        requireAdmin()
        repository.updateOperations { it.copy(slotOverrides = it.slotOverrides + ("$turfId|$date" to slots)) }
    }
    fun markNotificationsRead() = write { repository.markNotificationsRead() }
    fun saveProfile(value: UserProfile) = write("Profile saved.") {
        (repository as FirebaseBookMyTurfRepository).saveProfile(value)
        profile.value = value
    }
    fun logout() { (repository as? FirebaseBookMyTurfRepository)?.signOut(); authRole.value = null; profile.value = UserProfile("", "", "", ""); bookingDraft.value = BookingDraft(); lastBooking.value = null }
    fun showMessage(message: String) { snackbar.value = message }
    fun clearSnackbar() { snackbar.value = null }
}

class AppViewModelFactory(private val repository: BookMyTurfRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(repository) as T
}
