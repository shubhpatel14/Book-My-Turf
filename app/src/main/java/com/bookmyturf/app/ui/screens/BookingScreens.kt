@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.bookmyturf.app.ui.screens

import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bookmyturf.app.Routes
import com.bookmyturf.app.R
import com.bookmyturf.app.data.*
import com.bookmyturf.app.ui.*
import com.bookmyturf.app.ui.components.*
import com.bookmyturf.app.ui.theme.*
import java.util.Locale

@Composable
fun TurfDetailsScreen(vm: AppViewModel, id: String, nav: NavController) {
    val operations by vm.operations.collectAsStateWithLifecycle()
    val turfs by vm.turfs.collectAsStateWithLifecycle(); val turf = turfs.firstOrNull { it.id == id }
    if (turf == null || !turf.active) { ErrorState("TURF NOT FOUND", "This arena may no longer be available.") { nav.popBackStack() }; return }
    Scaffold(
        topBar = { BookingTopBar("The arena", nav, true) },
        bottomBar = {
            Surface(color = Charcoal, shadowElevation = 8.dp) {
                Row(Modifier.navigationBarsPadding().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("₹${turf.pricePerHour}", color = TextPrimary, style = MaterialTheme.typography.titleMedium)
                        Text("per hour", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    BookingButton("Choose a slot", { vm.beginBooking(turf); nav.navigate(Routes.slots(turf.id)) }, Modifier.weight(1.3f))
                }
            }
        }, containerColor = Ink
    ) { padding ->
        LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item { Box(Modifier.clip(RoundedCornerShape(24.dp))) { VenuePhotoCarousel(turf) } }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(turf.name, Modifier.weight(1f), fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
                        IconButton(onClick = { vm.toggleFavorite(turf.id) }) {
                            Icon(if (turf.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Save arena", tint = PulseLavender)
                        }
                    }
                    Text("${turf.area}, ${turf.city} · ${turf.distanceKm} km away", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        turf.sports.forEach { BookingChip(it.label) }
                        BookingChip(if (turf.isIndoor) "Indoor" else "Outdoor")
                    }
                }
            }
            item { BookingPanel {
                BookingSectionTitle("About the arena")
                Text(turf.description, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
            } }
            item { BookingPanel {
                BookingSectionTitle("Everything you need")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { turf.facilities.forEach { FacilityChip(it) } }
            } }
            item { BookingPanel {
                BookingSectionTitle("Opening hours")
                Text("Monday – Sunday", fontWeight = FontWeight.Bold)
                Text("${java.time.LocalTime.of(turf.openingHour, 0).format(slotTimeFormat)} – ${java.time.LocalTime.of(turf.closingHour, 0).format(slotTimeFormat)}", color = PulseLavender)
            } }
            item { BookingPanel {
                BookingSectionTitle("Find the arena")
                Text("${turf.area}, ${turf.city}", color = TextMuted)
                Spacer(Modifier.height(10.dp))
                MapPlaceholder(turf.area)
            } }
            item {
                BookingSectionTitle("From the players")
                val reviews = operations.reviews.filter { it.turfId == turf.id && it.published }
                if (reviews.isEmpty()) Text("No reviews yet.", color = TextMuted)
                reviews.forEach { review -> ReviewCard(review.author, review.stars, review.text); Spacer(Modifier.height(10.dp)) }
            }
        }
    }
}

@Composable private fun VenuePhotoCarousel(turf: Turf) {
    val first = when { Sport.BADMINTON in turf.sports -> R.drawable.venue_badminton; Sport.CRICKET in turf.sports && Sport.FOOTBALL !in turf.sports -> R.drawable.venue_cricket; else -> R.drawable.venue_football }
    val photos = remember(turf.photoResIds, first) { List(4) { turf.photoResIds.getOrNull(it) ?: first } }
    val pager = rememberPagerState { photos.size }
    Box(Modifier.fillMaxWidth().height(275.dp)) {
        HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
            Box(Modifier.fillMaxSize()) {
                VenuePhoto(turf, Modifier.fillMaxSize(), page)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(.1f), Ink.copy(.78f)))))
            }
        }
        Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(photos.size) { i -> Box(Modifier.width(if (pager.currentPage == i) 24.dp else 6.dp).height(6.dp).background(if (pager.currentPage == i) PulseLavender else Color.White.copy(.5f), CircleShape)) }
        }
        Surface(Modifier.align(Alignment.TopStart).padding(14.dp), color = Ink.copy(.82f), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Star, null, Modifier.size(15.dp), tint = Orange); Text(" ${turf.rating}  •  ${turf.distanceKm} KM", style = MaterialTheme.typography.labelMedium) } }
        Surface(Modifier.align(Alignment.TopEnd).padding(14.dp), color = Ink.copy(.82f), shape = RoundedCornerShape(18.dp)) { Text("${pager.currentPage + 1}/${photos.size}", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable private fun FacilityChip(name: String) {
    val icon = when { name.contains("Parking") -> Icons.Default.LocalParking; name.contains("light", true) -> Icons.Default.LightMode; name.contains("Water", true) -> Icons.Default.WaterDrop; name.contains("Canteen") -> Icons.Default.Restaurant; name.contains("Equipment") -> Icons.Default.SportsSoccer; else -> Icons.Default.CheckCircle }
    Surface(color = Steel, border = BorderStroke(1.dp, Line), shape = RoundedCornerShape(18.dp)) { Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, Modifier.size(16.dp), tint = PulseLavender); Spacer(Modifier.width(6.dp)); Text(name, style = MaterialTheme.typography.bodyMedium) } }
}

@Composable private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Surface(color = Charcoal, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Line)) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(icon, null, tint = PulseLavender); Spacer(Modifier.width(14.dp)); Text(label, Modifier.weight(.8f), fontWeight = FontWeight.Bold); Text(value, color = TextMuted) } }
}

@Composable private fun MapPlaceholder(area: String) {
    Box(Modifier.fillMaxWidth().height(170.dp).background(Steel, RoundedCornerShape(18.dp)).border(1.dp, Line, RoundedCornerShape(18.dp))) {
        Canvas(Modifier.fillMaxSize()) { repeat(7) { i -> drawLine(Line, androidx.compose.ui.geometry.Offset(0f, size.height * i / 6), androidx.compose.ui.geometry.Offset(size.width, size.height * (i + 2) / 8), 3f) }; repeat(5) { i -> drawLine(Line, androidx.compose.ui.geometry.Offset(size.width * i / 4, 0f), androidx.compose.ui.geometry.Offset(size.width * (i + 1) / 5, size.height), 2f) } }
        Icon(Icons.Default.LocationOn, null, Modifier.align(Alignment.Center).size(42.dp), tint = PulseLavender)
        Text("$area • MAP PREVIEW", Modifier.align(Alignment.BottomStart).padding(12.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable private fun ReviewCard(name: String, stars: Int, text: String) {
    Surface(color = Charcoal, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Line)) { Column(Modifier.padding(14.dp)) { Row { Text(name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); repeat(stars) { Icon(Icons.Default.Star, null, Modifier.size(15.dp), tint = Orange) } }; Spacer(Modifier.height(7.dp)); Text(text, color = TextMuted) } }
}

@Composable
fun SlotBookingScreen(vm: AppViewModel, id: String, nav: NavController) {
    val turfs by vm.turfs.collectAsStateWithLifecycle(); val turf = turfs.firstOrNull { it.id == id }
    if (turf == null || !turf.active) { ErrorState("SLOT UNAVAILABLE", "This turf is currently unavailable.") { nav.popBackStack() }; return }
    LaunchedEffect(turf.id) { if (vm.bookingDraft.value.turf?.id != turf.id) vm.beginBooking(turf) }
    val draft by vm.bookingDraft
    val operations by vm.operations.collectAsStateWithLifecycle()
    val bookings by vm.bookings.collectAsStateWithLifecycle()
    val slots = remember(turf, draft.date, operations, bookings) { venueSlots(turf, draft.date, operations, bookings) }
    LaunchedEffect(slots) {
        if (draft.slots.any { selected -> slots.none { it.time == selected.time && it.price == selected.price && it.state == SlotState.AVAILABLE } }) {
            vm.setBookingDate(draft.date)
            vm.showMessage("Availability changed. Please choose your slots again.")
        }
    }
    val dates = (0L..6L).map { offset ->
        val day = java.time.LocalDate.now().plusDays(offset)
        day.format(bookingDateFormat) to (day.format(java.time.format.DateTimeFormatter.ofPattern("EEE", Locale.US)) + "\n" + day.format(java.time.format.DateTimeFormatter.ofPattern("d MMM", Locale.US)))
    }
    Scaffold(topBar = { BookingTopBar("Pick your slot", nav, true) }, bottomBar = {
        Surface(color = Charcoal, border = BorderStroke(1.dp, Line)) { Column(Modifier.navigationBarsPadding().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("${draft.slots.size} HOUR${if (draft.slots.size == 1) "" else "S"}", color = TextMuted, style = MaterialTheme.typography.labelMedium); Text(if (draft.slots.isEmpty()) "Select a slot" else formatInr(draft.turfFee), fontFamily = ManropeUi, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = if (draft.slots.isEmpty()) TextPrimary else PulseLavender) }; Text(draft.timeLabel, color = TextMuted, style = MaterialTheme.typography.bodyMedium) }
            Spacer(Modifier.height(12.dp)); BookingButton("Review booking", { nav.navigate(Routes.SUMMARY) }, enabled = draft.slots.isNotEmpty())
        } }
    }, containerColor = Ink) { padding ->
        LazyColumn(Modifier.padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
            item { Text(turf.name, fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall); Text("Choose a date", color = TextMuted, style = MaterialTheme.typography.labelMedium); Spacer(Modifier.height(10.dp)); LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(dates) { d -> BookingChip(d.second, draft.date == d.first) { vm.setBookingDate(d.first) } } } }
            item { Row(verticalAlignment = Alignment.CenterVertically) { Text("Choose your time", fontFamily = ManropeUi, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium); Spacer(Modifier.weight(1f)); Box(Modifier.size(8.dp).background(PulseLavender, CircleShape)); Text(" Available", color = TextMuted, style = MaterialTheme.typography.bodyMedium); Spacer(Modifier.width(10.dp)); Box(Modifier.size(8.dp).background(Line, CircleShape)); Text(" Unavailable", color = TextMuted, style = MaterialTheme.typography.bodyMedium) } }
            items(slots.chunked(2)) { row -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { slot -> SlotCard(slot, slot in draft.slots, Modifier.weight(1f)) { if (!vm.toggleSlot(slot, slots)) vm.showMessage("Choose consecutive available slots.") } }; if (row.size == 1) Spacer(Modifier.weight(1f)) } }
            item { Surface(color = PulseLavender.copy(.08f), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, PulseLavender.copy(.3f))) { Row(Modifier.padding(14.dp)) { Icon(Icons.Default.Info, null, tint = PulseLavender); Spacer(Modifier.width(10.dp)); Text("Select adjacent available slots for a longer game. Booked and blocked slots cannot be selected.", color = TextMuted) } } }
        }
    }
}

@Composable private fun SlotCard(slot: TurfSlot, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val available = slot.state == SlotState.AVAILABLE
    val scale by animateFloatAsState(if (selected) 1.018f else 1f, tween(140), label = "slotScale")
    val background by androidx.compose.animation.animateColorAsState(if (selected) PulsePurple.copy(.3f) else if (available) Charcoal else Steel.copy(.55f), tween(140), label = "slotColor")
    Surface(onClick = onClick, enabled = available, modifier = modifier.heightIn(min = 88.dp).scale(scale), color = background, contentColor = if (selected) PulseLavender else if (available) TextPrimary else TextMuted.copy(.5f), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, if (selected) PulseLavender else Line)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(slot.time, fontWeight = FontWeight.ExtraBold); Text(if (available) "₹${slot.price}" else slot.state.name, style = MaterialTheme.typography.labelMedium) }
    }
}

@Composable
fun BookingSummaryScreen(vm: AppViewModel, nav: NavController) {
    val d by vm.bookingDraft; val turf = d.turf ?: return ErrorState("NO BOOKING", "Choose a turf and slot first.") { nav.navigate(Routes.EXPLORE) }
    Scaffold(topBar = { BookingTopBar("Booking summary", nav, true) }, bottomBar = { Surface(color = Ink) { BookingButton("Pay ${formatInr(d.total)}", { nav.navigate(Routes.PAYMENT) }, Modifier.navigationBarsPadding().padding(16.dp), icon = Icons.Default.Lock) } }, containerColor = Ink) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(18.dp)) {
            Box(Modifier.clip(RoundedCornerShape(22.dp))) { VenuePhotoCarousel(turf) }
            Spacer(Modifier.height(20.dp)); BookingSectionTitle("Game details")
            SummaryRow("Turf", turf.name); SummaryRow("Date", d.date); SummaryRow("Time", d.timeLabel); SummaryRow("Duration", "${d.slots.size} hour${if (d.slots.size == 1) "" else "s"}")
            Spacer(Modifier.height(24.dp)); BookingSectionTitle("Price breakdown")
            SummaryRow("Turf fee", formatInr(d.turfFee)); SummaryRow("Platform fee", formatInr(d.platformFee)); SummaryRow("GST (18%)", formatInr(d.gst)); HorizontalDivider(Modifier.padding(vertical = 12.dp), color = Line); SummaryRow("TOTAL", formatInr(d.total), true)
            Spacer(Modifier.height(18.dp)); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.VerifiedUser, null, tint = PulseLavender); Spacer(Modifier.width(10.dp)); Text("Your booking is protected by BookMyTurf's cancellation policy.", color = TextMuted, style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable private fun SummaryRow(label: String, value: String, total: Boolean = false) { Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) { Text(label, Modifier.weight(1f), color = if (total) TextPrimary else TextMuted, fontWeight = if (total) FontWeight.Black else FontWeight.Normal); Text(value, Modifier.weight(1.2f), textAlign = TextAlign.End, color = if (total) PulseLavender else TextPrimary, fontFamily = ManropeUi, style = if (total) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold) } }

@Composable
fun PaymentScreen(vm: AppViewModel, nav: NavController) {
    val draft by vm.bookingDraft
    val state by vm.paymentState
    val currentMode by vm.demoPaymentMode
    val errorMsg by vm.paymentErrorMessage
    val turf = draft.turf

    var paymentTab by remember { mutableStateOf("UPI_APPS") } // "UPI_APPS" or "QR_CODE"
    var selectedUpiApp by remember { mutableStateOf("Google Pay") }
    val upiApps = listOf("Google Pay", "PhonePe", "Paytm", "BHIM UPI", "CRED")
    var showDevControls by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { BookingTopBar("Secure Checkout", nav, true) },
        bottomBar = {
            Surface(color = Ink, shadowElevation = 12.dp) {
                Column(Modifier.navigationBarsPadding().padding(16.dp)) {
                    if (state == PaymentState.FAILED) {
                        Button(
                            onClick = { vm.resetPaymentState() },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Danger, contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Try Again")
                        }
                    } else {
                        BookingButton(
                            text = if (state == PaymentState.PROCESSING) "Authorizing Payment…" else "Proceed to Pay • ${formatInr(draft.total)}",
                            onClick = {
                                vm.processDemoPayment(selectedUpiApp) {
                                    nav.navigate(Routes.CONFIRMATION) {
                                        popUpTo(Routes.PAYMENT) { inclusive = true }
                                    }
                                }
                            },
                            enabled = state != PaymentState.PROCESSING,
                            icon = Icons.Default.Lock
                        )
                    }
                }
            }
        },
        containerColor = Ink
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Realistic Gateway Header Banner
            Surface(
                color = Charcoal,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, Line)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(40.dp).background(PulseLavender.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.VerifiedUser, null, tint = PulseLavender, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("256-Bit Encrypted Demo Gateway", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Simulated transaction · No real money debited", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Order & Turf Summary
            Surface(
                color = Charcoal,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Line)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(turf?.name ?: "Sports Arena", fontWeight = FontWeight.ExtraBold, fontFamily = ManropeUi, style = MaterialTheme.typography.titleMedium)
                            Text("${draft.date} • ${draft.timeLabel}", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                            Text("${draft.slots.size} hour duration", color = PulseLavender, style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("AMOUNT DUE", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                            Text(formatInr(draft.total), color = PulseLavender, fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }

            // Failure State Banner if payment failed
            if (state == PaymentState.FAILED) {
                Surface(
                    color = Danger.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Danger.copy(alpha = 0.4f))
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ErrorOutline, null, tint = Danger, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Payment Failed", fontWeight = FontWeight.Bold, color = Danger)
                            Text(errorMsg ?: "Your demo payment could not be processed.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Payment Mode Selector: UPI Apps vs QR Code
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    onClick = { paymentTab = "UPI_APPS" },
                    modifier = Modifier.weight(1f),
                    color = if (paymentTab == "UPI_APPS") PulsePurple.copy(alpha = 0.25f) else Charcoal,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (paymentTab == "UPI_APPS") PulseLavender else Line)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.PhoneAndroid, null, tint = if (paymentTab == "UPI_APPS") PulseLavender else TextMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("UPI Apps", fontWeight = FontWeight.Bold, color = if (paymentTab == "UPI_APPS") PulseLavender else TextPrimary)
                    }
                }
                Surface(
                    onClick = { paymentTab = "QR_CODE" },
                    modifier = Modifier.weight(1f),
                    color = if (paymentTab == "QR_CODE") PulsePurple.copy(alpha = 0.25f) else Charcoal,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, if (paymentTab == "QR_CODE") PulseLavender else Line)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Default.QrCode, null, tint = if (paymentTab == "QR_CODE") PulseLavender else TextMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Scan UPI QR", fontWeight = FontWeight.Bold, color = if (paymentTab == "QR_CODE") PulseLavender else TextPrimary)
                    }
                }
            }

            if (paymentTab == "UPI_APPS") {
                Text("SELECT UPI PAYMENT APP", color = TextMuted, style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    upiApps.forEach { app ->
                        Surface(
                            onClick = { selectedUpiApp = app },
                            color = if (selectedUpiApp == app) PulseLavender.copy(alpha = 0.12f) else Charcoal,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, if (selectedUpiApp == app) PulseLavender else Line)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountBalanceWallet, null, tint = if (selectedUpiApp == app) PulseLavender else TextMuted)
                                Spacer(Modifier.width(12.dp))
                                Text(app, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                                RadioButton(
                                    selected = selectedUpiApp == app,
                                    onClick = { selectedUpiApp = app },
                                    colors = RadioButtonDefaults.colors(selectedColor = PulseLavender)
                                )
                            }
                        }
                    }
                }
            } else {
                Surface(
                    color = Charcoal,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Scan using any UPI App", fontWeight = FontWeight.Bold)
                        Text("GPay, PhonePe, Paytm, BHIM or Mobile Banking", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        DemoQrCode(Modifier.size(200.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("UPI ID: bookmyturf@demo", color = PulseLavender, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            // Developer Testing Simulation Options (subtle expandable card)
            Surface(
                color = Steel.copy(alpha = 0.35f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Developer Simulation Mode", color = TextMuted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { showDevControls = !showDevControls },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(if (showDevControls) "Hide" else "Configure", style = MaterialTheme.typography.labelSmall, color = PulseLavender)
                        }
                    }
                    if (showDevControls) {
                        Text("Choose simulated payment outcome for testing:", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            DemoPaymentMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = currentMode == mode,
                                    onClick = { vm.setDemoPaymentMode(mode) },
                                    label = { Text(mode.name, style = MaterialTheme.typography.labelSmall) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = if (mode == DemoPaymentMode.SUCCESS) PulsePurple else Danger.copy(alpha = 0.5f),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Realistic Gateway Processing Screen Overlay (2.5s simulated transition)
        if (state == PaymentState.PROCESSING) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Ink.copy(alpha = 0.94f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        color = PulseLavender,
                        strokeWidth = 4.dp
                    )
                    Text("Processing Payment…", fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
                    Text("Communicating with UPI Banking Server", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Text("Please do not press back or close the app.", style = MaterialTheme.typography.bodySmall, color = TextMuted, textAlign = TextAlign.Center)
                    Surface(
                        color = Charcoal,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Line)
                    ) {
                        Text("DEMO SIMULATION · 256-BIT ENCRYPTION", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), color = PulseLavender, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable private fun DemoQrCode(modifier: Modifier = Modifier) {
    val pattern = remember { List(25) { y -> List(25) { x -> ((x * 13 + y * 7 + x * y) % 5 < 2) || (x < 7 && y < 7) || (x > 17 && y < 7) || (x < 7 && y > 17) } } }
    Box(modifier.background(Color.White).padding(10.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val cell = size.width / 25f
            pattern.forEachIndexed { y, row ->
                row.forEachIndexed { x, on ->
                    if (on) drawRect(Ink, androidx.compose.ui.geometry.Offset(x * cell, y * cell), androidx.compose.ui.geometry.Size(cell + .5f, cell + .5f))
                }
            }
        }
    }
}

@Composable
fun ConfirmationScreen(vm: AppViewModel, nav: NavController) {
    val booking by vm.lastBooking
    val b = booking ?: Booking("BMT-20260910-4821", "strike", "Strike Arena", "10 September 2026", "07:00 PM – 08:00 PM", 1, 967.60, BookingStatus.CONFIRMED, transactionId = "BMT-DEMO-8F72A91C")
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(if (appeared) 1f else .2f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "success")
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier.size(92.dp).scale(scale).background(PulseLavender.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CheckCircle, null, Modifier.size(64.dp), tint = PulseLavender)
        }
        Spacer(Modifier.height(16.dp))
        Text("Payment Successful!", fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center)
        Text("Your turf slot is locked in and confirmed.", color = TextMuted)
        Spacer(Modifier.height(18.dp))

        // Demo Transaction & Booking Identification Card
        Surface(
            Modifier.fillMaxWidth(),
            color = Charcoal,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, PulseLavender.copy(alpha = 0.4f))
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SummaryRow("Booking ID", b.id)
                if (b.transactionId.isNotBlank()) {
                    SummaryRow("Transaction ID", b.transactionId)
                }
                SummaryRow("Payment Status", "${b.paymentStatus} (${b.paymentMethod})")
                HorizontalDivider(Modifier.padding(vertical = 4.dp), color = Line)
                SummaryRow("Turf", b.turfName)
                SummaryRow("Date", b.date)
                SummaryRow("Time", b.time)
                SummaryRow("Duration", "${b.durationHours} hour${if (b.durationHours == 1) "" else "s"}")
                SummaryRow("Amount paid", formatInr(b.amount), true)
                Spacer(Modifier.height(10.dp))
                Box(Modifier.align(Alignment.CenterHorizontally)) { DemoQrCode(Modifier.size(120.dp)) }
                Text("SHOW AT VENUE ENTRY", Modifier.align(Alignment.CenterHorizontally).padding(top = 6.dp), style = MaterialTheme.typography.labelMedium, color = TextMuted)
            }
        }

        Spacer(Modifier.height(18.dp))
        BookingButton("View In My Bookings", { nav.navigate(Routes.BOOKINGS) { popUpTo(Routes.USER) } })
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_INSERT)
                            .setData(CalendarContract.Events.CONTENT_URI)
                            .putExtra(CalendarContract.Events.TITLE, "Game at ${b.turfName}")
                            .putExtra(CalendarContract.Events.DESCRIPTION, "BookMyTurf Booking ${b.id}\nTx ID: ${b.transactionId}")
                    )
                }
            },
            Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Line)
        ) {
            Icon(Icons.Default.CalendarMonth, null)
            Spacer(Modifier.width(8.dp))
            Text("ADD TO CALENDAR")
        }
        TextButton(onClick = { vm.showMessage("Map directions preview opened.") }) {
            Icon(Icons.Default.Directions, null, tint = PulseLavender)
            Spacer(Modifier.width(8.dp))
            Text("GET DIRECTIONS", color = PulseLavender)
        }
    }
}

@Composable
fun ErrorState(title: String, message: String, action: () -> Unit) { Column(Modifier.fillMaxSize().background(Ink).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Default.ErrorOutline, null, Modifier.size(58.dp), tint = Danger); Spacer(Modifier.height(18.dp)); Text(title, fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall); Spacer(Modifier.height(8.dp)); Text(message, color = TextMuted, textAlign = TextAlign.Center); Spacer(Modifier.height(20.dp)); Button(onClick = action, colors = ButtonDefaults.buttonColors(containerColor = PulseLavender, contentColor = Ink)) { Text("TRY AGAIN") } } }



@Composable
private fun BookingTopBar(title: String, nav: NavController, showBack: Boolean = true) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showBack) IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.background(Steel, CircleShape)) {
            Icon(Icons.Default.ChevronLeft, "Back", tint = TextPrimary)
        }
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            Text("Make time for your game", color = TextMuted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun BookingButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
    Button(onClick = onClick, enabled = enabled, modifier = modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PulsePurple, contentColor = Color.White, disabledContainerColor = Steel, disabledContentColor = TextMuted)) {
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BookingSectionTitle(title: String) {
    Text(title, Modifier.padding(bottom = 12.dp), fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun BookingChip(text: String, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    Surface(
        modifier = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick),
        color = if (selected) PulsePurple.copy(.28f) else Steel,
        shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, if (selected) PulseLavender else Line)
    ) {
        Text(text, Modifier.padding(horizontal = 16.dp, vertical = 13.dp), color = if (selected) PulseLavender else TextPrimary,
            style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
    }
}

@Composable
private fun BookingPanel(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), color = Charcoal, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Line)) {
        Column(Modifier.padding(18.dp), content = content)
    }
}
