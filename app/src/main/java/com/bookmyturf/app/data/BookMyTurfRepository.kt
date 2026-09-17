package com.bookmyturf.app.data

import kotlinx.coroutines.flow.Flow

interface BookMyTurfRepository {
    val turfs: Flow<List<Turf>>
    val bookings: Flow<List<Booking>>
    val notifications: Flow<List<AppNotification>>
    val operations: Flow<OperationsState>
    val tournaments: Flow<List<Tournament>>
    val tournamentRegistrations: Flow<List<TournamentRegistration>>
    val tournamentSyncUnavailable: Flow<Boolean>
    suspend fun upsertTournament(tournament: Tournament)
    suspend fun registerTournament(tournament: Tournament, teamName: String, captainName: String, captainPhone: String, transactionId: String): TournamentRegistration
    suspend fun setTournamentActive(id: String, active: Boolean)
    suspend fun setTournamentRegistrationStatus(id: String, status: String)
    suspend fun updateOperations(change: (OperationsState) -> OperationsState)
    suspend fun markNotificationsRead()
    suspend fun toggleFavorite(turfId: String)
    suspend fun createBooking(turf: Turf, date: String, time: String, duration: Int, total: Double, customer: String = "Arjun Patel", transactionId: String = ""): Booking
    suspend fun cancelBooking(bookingId: String)
    suspend fun setTurfActive(turfId: String, active: Boolean)
    suspend fun deleteTurf(turfId: String)
    suspend fun upsertTurf(turf: Turf)
}

/**
 * Production implementations plug in here. FirebaseAuth owns identity; Firestore
 * backs users/turfs/slots/bookings/payments/reviews/favorites/coupons/notifications/admins;
 * Storage owns venue media; FCM delivers reminders. The UI never receives secrets.
 */
interface AuthGateway {
    suspend fun signIn(emailOrMobile: String, password: String): Result<UserProfile>
    suspend fun register(name: String, mobile: String, email: String, password: String): Result<UserProfile>
    suspend fun signOut()
}

interface PaymentGateway {
    suspend fun createOrder(bookingId: String, amountPaise: Long): Result<String>
    suspend fun verifyPayment(orderId: String, providerPayload: String): Result<String>
}
