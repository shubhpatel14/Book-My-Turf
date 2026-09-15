package com.bookmyturf.app.data

/** Keep paid-service integrations off until explicitly approved, deployed and tested. */
object CloudFeatures {
    const val bookingsEnabled = true
    const val photoUploadsEnabled = true
    const val bookingUnavailable = "Select an available slot to proceed."
    const val photosUnavailable = "Enter up to 4 image URLs for this venue."
}
