package com.bookmyturf.app.data

import java.nio.file.Files
import java.time.LocalDate
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class PersistentOperationsTest {
    private val tomorrow get() = LocalDate.now().plusDays(1).format(bookingDateFormat)

    @Test fun editsPhotosFavoritesAndOperationsSurviveRepositoryRestart() = runBlocking {
        val directory = Files.createTempDirectory("bmt-persistence-test").toFile()
        try {
            val file = java.io.File(directory, "store.bin")
            val repo = DemoBookMyTurfRepository(file)
            val turf = repo.turfs.first().first().copy(name = "Updated Arena", pricePerHour = 1250,
                photoUris = listOf("file:///saved/photo1.jpg", "file:///saved/photo2.jpg"), openingHour = 8, closingHour = 21)
            repo.upsertTurf(turf)
            repo.toggleFavorite(turf.id)
            repo.updateOperations { it.copy(slotOverrides = mapOf("${turf.id}|$tomorrow" to listOf(TurfSlot("09:00 AM", 1250, SlotState.BLOCKED))),
                offers = listOf(VenueOffer("test", "NEW", "Updated offer", false)), reviews = it.reviews.map { r -> r.copy(published = false) }) }
            repo.markNotificationsRead()
            val loaded = DemoBookMyTurfRepository(file)
            val saved = loaded.turfs.first().first { it.id == turf.id }
            assertEquals(turf.copy(isFavorite = true), saved)
            assertFalse(loaded.notifications.first().any { it.unread })
            assertFalse(loaded.operations.first().offers.first().active)
            assertFalse(loaded.operations.first().reviews.any { it.published })
            assertEquals(SlotState.BLOCKED, venueSlots(saved, tomorrow, loaded.operations.first(), loaded.bookings.first()).first { it.time == "09:00 AM" }.state)
        } finally { directory.deleteRecursively() }
    }

    @Test fun concurrentBookingsCannotClaimTheSameSlotAndCancellationReleasesIt() = runBlocking {
        val repo = DemoBookMyTurfRepository()
        val turf = repo.turfs.first().first()
        val results = (1..2).map { async { runCatching { repo.createBooking(turf, tomorrow, "07:00 PM - 08:00 PM", 1, 967.6) } } }.awaitAll()
        assertEquals(1, results.count { it.isSuccess })
        assertEquals(SlotState.BOOKED, venueSlots(turf, tomorrow, repo.operations.first(), repo.bookings.first()).first { it.time == "07:00 PM" }.state)
        repo.cancelBooking(results.first { it.isSuccess }.getOrThrow().id)
        assertEquals(SlotState.AVAILABLE, venueSlots(turf, tomorrow, repo.operations.first(), repo.bookings.first()).first { it.time == "07:00 PM" }.state)
    }

    @Test fun inactiveBlockedAndRepricedVenuesRejectStaleCheckout() = runBlocking {
        val repo = DemoBookMyTurfRepository()
        val turf = repo.turfs.first().first()
        repo.setTurfActive(turf.id, false)
        assertTrue(runCatching { repo.createBooking(turf, tomorrow, "07:00 PM - 08:00 PM", 1, 967.6) }.isFailure)
        repo.setTurfActive(turf.id, true)
        repo.updateOperations { it.copy(slotOverrides = mapOf("${turf.id}|$tomorrow" to listOf(TurfSlot("07:00 PM", 800, SlotState.BLOCKED)))) }
        assertTrue(runCatching { repo.createBooking(turf, tomorrow, "07:00 PM - 08:00 PM", 1, 967.6) }.isFailure)
        repo.updateOperations { it.copy(slotOverrides = emptyMap()) }
        repo.upsertTurf(turf.copy(pricePerHour = 1200))
        assertTrue(runCatching { repo.createBooking(turf, tomorrow, "07:00 PM - 08:00 PM", 1, 967.6) }.isFailure)
    }

    @Test fun writeFailureDoesNotPublishUnsavedChanges() = runBlocking {
        val directory = Files.createTempDirectory("bmt-failure-test").toFile()
        try {
            val parent = java.io.File(directory, "parent")
            val repo = DemoBookMyTurfRepository(java.io.File(parent, "store.bin"))
            parent.writeText("not a directory")
            val original = repo.turfs.first().first()
            assertTrue(runCatching { repo.upsertTurf(original.copy(name = "Unsaved")) }.isFailure)
            assertEquals(original, repo.turfs.first().first())
        } finally { directory.deleteRecursively() }
    }

    @Test fun demoPaymentServiceGeneratesTransactionIdAndRespectsSimulationModes() = runBlocking {
        val paymentService = DemoPaymentService()
        val request = PaymentRequest(800.0, "Strike Arena", "Rohan")

        // Test SUCCESS mode
        paymentService.mode = DemoPaymentMode.SUCCESS
        val successResult = paymentService.processPayment(request)
        assertTrue(successResult is PaymentResult.Success)
        val success = successResult as PaymentResult.Success
        assertTrue(success.transactionId.startsWith("BMT-DEMO-"))
        assertEquals(8, success.transactionId.removePrefix("BMT-DEMO-").length)
        assertEquals(800.0, success.amount, 0.01)

        // Test FAILED mode
        paymentService.mode = DemoPaymentMode.FAILED
        val failedResult = paymentService.processPayment(request)
        assertTrue(failedResult is PaymentResult.Failed)

        // Test CANCELLED mode
        paymentService.mode = DemoPaymentMode.CANCELLED
        val cancelledResult = paymentService.processPayment(request)
        assertTrue(cancelledResult is PaymentResult.Cancelled)
    }

    @Test fun bookingSavesDemoPaymentDetailsAndTurfImageUrls() = runBlocking {
        val repo = DemoBookMyTurfRepository()
        val turf = repo.turfs.first().first().copy(
            photoUris = listOf(
                "https://example.com/turf1.jpg",
                "https://example.com/turf2.jpg",
                "https://example.com/turf3.jpg",
                "https://example.com/turf4.jpg"
            )
        )
        repo.upsertTurf(turf)
        val savedTurf = repo.turfs.first().first { it.id == turf.id }
        assertEquals(4, savedTurf.imageUrls.size)
        assertEquals("https://example.com/turf1.jpg", savedTurf.imageUrls[0])

        val booking = repo.createBooking(
            savedTurf,
            tomorrow,
            "07:00 PM - 08:00 PM",
            1,
            967.6,
            customer = "Rohan",
            transactionId = "BMT-DEMO-A1B2C3D4"
        )
        assertEquals("BMT-DEMO-A1B2C3D4", booking.transactionId)
        assertEquals("DEMO_UPI", booking.paymentMethod)
        assertEquals("DEMO_PAID", booking.paymentStatus)
    }
}
