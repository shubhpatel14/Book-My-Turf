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
    val date: String = "10 September 2026",
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
    val turfs: StateFlow<List<Turf>> = repository.turfs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val bookings: StateFlow<List<Booking>> = repository.bookings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val notifications: StateFlow<List<AppNotification>> = repository.notifications.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

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

    fun login(identifier: String, password: String): Boolean {
        val admin = identifier.trim().equals("admin@bookmyturf.demo", true) && password == "Admin@12345"
        val user = identifier.isNotBlank() && password.length >= 6
        authRole.value = when { admin -> AuthRole.ADMIN; user -> AuthRole.USER; else -> null }
        return authRole.value != null
    }

    fun beginBooking(turf: Turf) {
        val fee = turf.pricePerHour.toDouble()
        bookingDraft.value = BookingDraft(turf = turf, turfFee = fee, gst = (fee + 20.0) * .18)
        paymentState.value = PaymentState.IDLE
    }

    fun setBookingDate(date: String) {
        bookingDraft.value = bookingDraft.value.copy(date = date, slots = emptyList())
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

    fun processDemoPayment(onSuccess: () -> Unit) {
        val draft = bookingDraft.value
        val turf = draft.turf ?: return
        paymentState.value = PaymentState.PROCESSING
        viewModelScope.launch {
            delay(1500)
            lastBooking.value = repository.createBooking(turf, draft.date, draft.timeLabel, draft.slots.size.coerceAtLeast(1), draft.total)
            paymentState.value = PaymentState.SUCCESS
            onSuccess()
        }
    }

    fun register(name: String, phone: String, email: String, password: String, confirm: String): String? {
        val error = when {
            name.trim().length < 3 -> "Enter your full name"
            !phone.filter(Char::isDigit).let { it.length == 10 || it.length == 12 } -> "Enter a valid Indian mobile number"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Enter a valid email address"
            password.length < 8 -> "Password must be at least 8 characters"
            password != confirm -> "Passwords do not match"
            else -> null
        }
        if (error == null) {
            profile.value = UserProfile(name.trim(), phone.trim(), email.trim(), "Vadodara, Gujarat")
            authRole.value = AuthRole.USER
        }
        return error
    }

    fun toggleFavorite(id: String) = viewModelScope.launch { repository.toggleFavorite(id) }
    fun cancelBooking(id: String) = viewModelScope.launch { repository.cancelBooking(id); snackbar.value = "Booking cancelled in demo mode." }
    fun setTurfActive(id: String, active: Boolean) = viewModelScope.launch { repository.setTurfActive(id, active) }
    fun deleteTurf(id: String) = viewModelScope.launch { repository.deleteTurf(id); snackbar.value = "Turf removed from demo data." }
    fun upsertTurf(turf: Turf) = viewModelScope.launch { repository.upsertTurf(turf); snackbar.value = "Turf saved to demo data." }
    fun showMessage(message: String) { snackbar.value = message }
    fun clearSnackbar() { snackbar.value = null }
}

class AppViewModelFactory(private val repository: BookMyTurfRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(repository) as T
}
