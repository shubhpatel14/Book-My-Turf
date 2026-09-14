package com.bookmyturf.app.data

/** Keep paid-service integrations off until explicitly approved, deployed and tested. */
object CloudFeatures {
    const val bookingsEnabled = false
    const val photoUploadsEnabled = false
    const val bookingUnavailable = "Online booking is not enabled yet. You can browse venues; no payment will be taken."
    const val photosUnavailable = "Photo uploads are paused while billing is disabled. Venue details can still be saved."
}
