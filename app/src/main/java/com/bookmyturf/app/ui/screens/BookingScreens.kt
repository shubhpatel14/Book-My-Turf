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
    val turfs by vm.turfs.collectAsStateWithLifecycle(); val turf = turfs.firstOrNull { it.id == id }
    if (turf == null) { ErrorState("TURF NOT FOUND", "This arena may no longer be available.") { nav.popBackStack() }; return }
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
                Text("6:00 AM – 11:00 PM", color = PulseLavender)
            } }
            item { BookingPanel {
                BookingSectionTitle("Find the arena")
                Text("${turf.area}, ${turf.city}", color = TextMuted)
                Spacer(Modifier.height(10.dp))
                MapPlaceholder(turf.area)
            } }
            item { BookingSectionTitle("From the players")
                ReviewCard("Rohan Mehta", 5, "Top-quality turf and bright floodlights. The staff had everything ready on time.")
                Spacer(Modifier.height(10.dp))
                ReviewCard("Kabir Shah", 4, "Great surface for box cricket. Parking gets busy after 7 PM.")
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
                Image(painterResource(photos[page]), "${turf.name}, photo ${page + 1} of 4", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
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
    if (turf == null) { ErrorState("SLOT UNAVAILABLE", "This turf is currently unavailable.") { nav.popBackStack() }; return }
    LaunchedEffect(turf.id) { if (vm.bookingDraft.value.turf?.id != turf.id) vm.beginBooking(turf) }
    val slots = remember { DemoBookMyTurfRepository.slots() }; val draft by vm.bookingDraft
    val dates = listOf("10 September 2026" to "TODAY\n10 SEP", "11 September 2026" to "TOMORROW\n11 SEP", "12 September 2026" to "SAT\n12 SEP", "13 September 2026" to "SUN\n13 SEP")
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
    val draft by vm.bookingDraft; val state by vm.paymentState; var method by remember { mutableStateOf("Google Pay") }
    val methods = listOf("Google Pay", "PhonePe", "Paytm", "BHIM", "Other UPI")
    Scaffold(topBar = { BookingTopBar("UPI payment", nav, true) }, bottomBar = { Surface(color = Ink) { BookingButton(if (state == PaymentState.PROCESSING) "Processing…" else "Pay Now • ${formatInr(draft.total)}", { vm.processDemoPayment { nav.navigate(Routes.CONFIRMATION) { popUpTo(Routes.PAYMENT) { inclusive = true } } } }, Modifier.navigationBarsPadding().padding(16.dp), enabled = state != PaymentState.PROCESSING) } }, containerColor = Ink) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(color = Orange.copy(.12f), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Orange.copy(.4f))) { Text("SIMULATED PAYMENT • DEMO MODE", Modifier.padding(10.dp), color = Orange, style = MaterialTheme.typography.labelMedium) }
            Spacer(Modifier.height(20.dp)); Text(formatInr(draft.total), fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineLarge); Text("AMOUNT PAYABLE", color = TextMuted, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(24.dp)); DemoQrCode(Modifier.size(210.dp)); Spacer(Modifier.height(12.dp)); Text("SCAN TO TEST PAYMENT", style = MaterialTheme.typography.labelLarge); Text("DEMO ONLY — NO REAL PAYMENT", color = Danger, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp)); Text("UPI ID  bookmyturf@upi", color = TextMuted)
            Spacer(Modifier.height(24.dp)); Text("CHOOSE A DEMO UPI APP", Modifier.fillMaxWidth(), color = TextMuted, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(10.dp)); Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { methods.forEach { item -> Surface(onClick = { method = item }, color = if (method == item) PulseLavender.copy(.12f) else Charcoal, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, if (method == item) PulseLavender else Line)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.AccountBalanceWallet, null, tint = if (method == item) PulseLavender else TextMuted); Spacer(Modifier.width(12.dp)); Text(item, Modifier.weight(1f), fontWeight = FontWeight.Bold); RadioButton(method == item, { method = item }, colors = RadioButtonDefaults.colors(selectedColor = PulseLavender)) } } } }
        }
        if (state == PaymentState.PROCESSING) Box(Modifier.fillMaxSize().background(Ink.copy(.86f)), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(color = PulseLavender); Spacer(Modifier.height(18.dp)); Text("VERIFYING DEMO PAYMENT", style = MaterialTheme.typography.labelLarge); Text("No real transaction is being made", color = TextMuted) } }
    }
}

@Composable private fun DemoQrCode(modifier: Modifier = Modifier) {
    val pattern = remember { List(25) { y -> List(25) { x -> ((x * 13 + y * 7 + x * y) % 5 < 2) || (x < 7 && y < 7) || (x > 17 && y < 7) || (x < 7 && y > 17) } } }
    Box(modifier.background(Color.White).padding(10.dp)) { Canvas(Modifier.fillMaxSize()) { val cell = size.width / 25f; pattern.forEachIndexed { y, row -> row.forEachIndexed { x, on -> if (on) drawRect(Ink, androidx.compose.ui.geometry.Offset(x * cell, y * cell), androidx.compose.ui.geometry.Size(cell + .5f, cell + .5f)) } } } }
}

@Composable
fun ConfirmationScreen(vm: AppViewModel, nav: NavController) {
    val booking by vm.lastBooking; val b = booking ?: Booking("BMT-20260910-4821", "strike", "Strike Arena", "10 September 2026", "07:00 PM – 08:00 PM", 1, 967.60, BookingStatus.CONFIRMED)
    var appeared by remember { mutableStateOf(false) }; LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(if (appeared) 1f else .2f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "success"); val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(Ink).verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp)); Box(Modifier.size(92.dp).scale(scale).background(PulseLavender.copy(.12f), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.CheckCircle, null, Modifier.size(68.dp), tint = PulseLavender) }
        Spacer(Modifier.height(18.dp)); Text("Booking confirmed", fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center); Text("Your arena is locked in.", color = TextMuted)
        Spacer(Modifier.height(22.dp)); StatusBadge(b.id)
        Spacer(Modifier.height(20.dp)); Surface(Modifier.fillMaxWidth(), color = Charcoal, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Line)) { Column(Modifier.padding(18.dp)) { SummaryRow("Turf", b.turfName); SummaryRow("Date", b.date); SummaryRow("Time", b.time); SummaryRow("Duration", "${b.durationHours} hour"); SummaryRow("Amount paid", formatInr(b.amount), true); Spacer(Modifier.height(12.dp)); Box(Modifier.align(Alignment.CenterHorizontally)) { DemoQrCode(Modifier.size(130.dp)) }; Text("SHOW AT VENUE", Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp), style = MaterialTheme.typography.labelMedium, color = TextMuted) } }
        Spacer(Modifier.height(18.dp)); BookingButton("View Booking", { nav.navigate(Routes.BOOKINGS) { popUpTo(Routes.USER) } })
        Spacer(Modifier.height(8.dp)); OutlinedButton(onClick = { runCatching { context.startActivity(Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI).putExtra(CalendarContract.Events.TITLE, "Game at ${b.turfName}").putExtra(CalendarContract.Events.DESCRIPTION, "BookMyTurf ${b.id}")) } }, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Line)) { Icon(Icons.Default.CalendarMonth, null); Spacer(Modifier.width(8.dp)); Text("ADD TO CALENDAR") }
        TextButton(onClick = { vm.showMessage("Map directions preview opened in demo mode.") }) { Icon(Icons.Default.Directions, null, tint = PulseLavender); Spacer(Modifier.width(8.dp)); Text("GET DIRECTIONS", color = PulseLavender) }
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
