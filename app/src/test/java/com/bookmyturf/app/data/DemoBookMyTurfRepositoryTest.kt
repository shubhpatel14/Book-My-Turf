package com.bookmyturf.app.data

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DemoBookMyTurfRepositoryTest {
    @Test
    fun bookingFlowAddsConfirmedBooking() = runBlocking {
        val repository = DemoBookMyTurfRepository()
        val turf = repository.turfs.first().first()
        val before = repository.bookings.first().size

        val result = repository.createBooking(turf, java.time.LocalDate.now().plusDays(1).format(bookingDateFormat), "07:00 PM - 08:00 PM", 1, 967.60)

        assertEquals(BookingStatus.CONFIRMED, result.status)
        assertEquals(before + 1, repository.bookings.first().size)
        assertEquals(967.60, result.amount, 0.001)
    }

    @Test
    fun favoriteToggleUpdatesObservableTurf() = runBlocking {
        val repository = DemoBookMyTurfRepository()
        val turf = repository.turfs.first().first()
        repository.toggleFavorite(turf.id)
        assertTrue(repository.turfs.first().first { it.id == turf.id }.isFavorite)
    }
}
