@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bookmyturf.app.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bookmyturf.app.data.*
import com.bookmyturf.app.ui.AppViewModel
import com.bookmyturf.app.ui.components.*
import com.bookmyturf.app.ui.theme.*
import kotlin.math.max

private enum class AdminSection(val label: String, val icon: ImageVector) {
    DASHBOARD("Dashboard", Icons.Default.Dashboard), TURFS("Turfs", Icons.Default.Stadium),
    TOURNAMENTS("Tournaments", Icons.Default.EmojiEvents),
    BOOKINGS("Bookings", Icons.Default.ConfirmationNumber), USERS("Users", Icons.Default.Groups),
    PAYMENTS("Payments", Icons.Default.Payments), OFFERS("Offers", Icons.Default.LocalOffer),
    REVIEWS("Reviews", Icons.Default.Star), SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun AdminDashboardScreen(vm: AppViewModel, nav: NavController, onLogout: () -> Unit) {
    if (vm.authRole.value != com.bookmyturf.app.ui.AuthRole.ADMIN) { ErrorState("Admin sign-in required", "Sign in with your admin account.", onLogout); return }
    var section by remember { mutableStateOf(AdminSection.DASHBOARD) }
    BoxWithConstraints(Modifier.fillMaxSize().background(Ink)) {
        val tablet = maxWidth >= 720.dp
        Row {
            if (tablet) AdminRail(section, { section = it }, onLogout)
            Column(Modifier.weight(1f)) {
                AdminHeader(section, onLogout)
                if (!tablet) LazyRow(Modifier.fillMaxWidth().background(Charcoal).padding(vertical = 8.dp), contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) { items(AdminSection.entries) { item -> Tag(item.label, section == item) { section = item } } }
                when (section) {
                    AdminSection.DASHBOARD -> AdminHome(vm)
                    AdminSection.TURFS -> TurfManagement(vm)
                    AdminSection.TOURNAMENTS -> TournamentManagement(vm)
                    AdminSection.BOOKINGS -> BookingManagement(vm)
                    AdminSection.USERS -> UserManagement(vm)
                    AdminSection.PAYMENTS -> PaymentsDashboard(vm)
                    AdminSection.OFFERS -> OffersManagement(vm)
                    AdminSection.REVIEWS -> ReviewsManagement(vm)
                    AdminSection.SETTINGS -> AdminSettings(vm, onLogout)
                }
            }
        }
    }
}

@Composable private fun AdminRail(selected: AdminSection, onSelect: (AdminSection) -> Unit, logout: () -> Unit) {
    Column(Modifier.width(220.dp).fillMaxHeight().background(Charcoal).padding(14.dp)) {
        BrandMark(); Spacer(Modifier.height(8.dp)); Text("ADMIN COMMAND", color = Lime, style = MaterialTheme.typography.labelMedium); Spacer(Modifier.height(28.dp))
        AdminSection.entries.forEach { item -> NavigationDrawerItem(label = { Text(item.label.uppercase(), style = MaterialTheme.typography.labelMedium) }, icon = { Icon(item.icon, null) }, selected = selected == item, onClick = { onSelect(item) }, modifier = Modifier.padding(vertical = 3.dp), shape = RoundedCornerShape(6.dp), colors = NavigationDrawerItemDefaults.colors(selectedContainerColor = Lime, selectedIconColor = Ink, selectedTextColor = Ink, unselectedContainerColor = Color.Transparent, unselectedIconColor = TextMuted, unselectedTextColor = TextMuted)) }
        Spacer(Modifier.weight(1f)); TextButton(onClick = logout) { Icon(Icons.Default.Logout, null, tint = Danger); Spacer(Modifier.width(8.dp)); Text("LOGOUT", color = Danger) }
    }
}

@Composable private fun AdminHeader(section: AdminSection, logout: () -> Unit) { Row(Modifier.fillMaxWidth().statusBarsPadding().height(70.dp).background(Ink).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(section.label.uppercase(), style = MaterialTheme.typography.headlineMedium); Text("BOOKMYTURF OPERATIONS", color = TextMuted, style = MaterialTheme.typography.labelMedium) }; Surface(color = Lime.copy(.12f), shape = RoundedCornerShape(6.dp)) { Text("FIREBASE", Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = Lime, style = MaterialTheme.typography.labelMedium) }; IconButton(logout) { Icon(Icons.Default.Logout, "Logout", tint = TextMuted) } } }

@Composable private fun AdminContent(content: LazyListScope.() -> Unit) { LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }

@Composable private fun AdminHome(vm: AppViewModel) {
    val bookings by vm.bookings.collectAsStateWithLifecycle()
    val turfs by vm.turfs.collectAsStateWithLifecycle()
    val operations by vm.operations.collectAsStateWithLifecycle()
    val revenue = bookings.filter { it.status != BookingStatus.CANCELLED }.sumOf { it.amount }
    val days = (6L downTo 0L).map { java.time.LocalDate.now().minusDays(it) }
    val daily = days.map { day -> bookings.filter { it.status != BookingStatus.CANCELLED && runCatching { bookingDate(it.date) == day }.getOrDefault(false) }.sumOf { it.amount }.toInt() }
    AdminContent {
        item { Text("SAVED APP DATA", color = TextMuted, style = MaterialTheme.typography.labelMedium) }
        item {
            val metrics = listOf("Booking value" to formatInr(revenue), "Bookings" to bookings.size.toString(), "Active turfs" to turfs.count { it.active }.toString(), "Registered users" to operations.profiles.size.toString())
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { metrics.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { MetricCard(it.first, it.second, "All saved records", Modifier.weight(1f)) } } } }
        }
        item { Surface(color = Charcoal, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Line)) {
            Column(Modifier.padding(16.dp)) {
                SectionTitle("Last 7 days")
                Text(formatInr(daily.sum().toDouble()), color = Lime)
                RevenueChart(daily, Modifier.fillMaxWidth().height(180.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { days.forEach { Text(it.dayOfWeek.name.take(3), color = TextMuted, style = MaterialTheme.typography.labelSmall) } }
            }
        } }
        item { SectionTitle("Recent bookings") }
        if (bookings.isEmpty()) item { Text("No bookings yet.", color = TextMuted) }
        items(bookings.take(10), key = { it.id }) { CompactAdminBooking(it) }
    }
}

@Composable private fun MetricCard(label: String, value: String, delta: String, modifier: Modifier = Modifier) { Surface(modifier, color = Charcoal, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Line)) { Column(Modifier.padding(14.dp)) { Text(label.uppercase(), color = TextMuted, style = MaterialTheme.typography.labelMedium, maxLines = 1); Spacer(Modifier.height(8.dp)); Text(value, style = MaterialTheme.typography.headlineMedium, maxLines = 1); Spacer(Modifier.height(5.dp)); Text(delta, color = Lime, style = MaterialTheme.typography.labelMedium) } } }

@Composable private fun RevenueChart(values: List<Int>, modifier: Modifier) { val progress by animateFloatAsState(1f, tween(900), label = "chart"); Canvas(modifier) { val maxV = (values.maxOrNull() ?: 0).coerceAtLeast(1).toFloat(); val step = size.width / (values.size - 1); val points = values.mapIndexed { i, v -> Offset(i * step, size.height - (v / maxV * size.height * .82f) - 10f) }; points.zipWithNext().forEach { (a,b) -> drawLine(Lime, a, Offset(a.x + (b.x-a.x)*progress, a.y+(b.y-a.y)*progress), 5f) }; points.forEach { drawCircle(Lime, 7f, it); drawCircle(Ink, 3f, it) }; repeat(4) { y -> drawLine(Line.copy(.7f), Offset(0f, size.height * y/4), Offset(size.width, size.height*y/4), 1f) } } }


@Composable private fun TurfManagement(vm: AppViewModel) {
    val turfs by vm.turfs.collectAsStateWithLifecycle(); var form by remember { mutableStateOf<Turf?>(null) }; var manageSlots by remember { mutableStateOf<Turf?>(null) }
    AdminContent { item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("${turfs.size} VENUES", color = TextMuted, style = MaterialTheme.typography.labelMedium); Text("Manage turf inventory and availability.") }; Button({ form = Turf("", "", "Vadodara", "", listOf(Sport.FOOTBALL), 800, 4.5, 1.0, "", listOf("Parking")) }, shape = RoundedCornerShape(6.dp), colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) { Icon(Icons.Default.Add, null); Text(" ADD TURF") } } }; items(turfs, key = { it.id }) { turf -> AdminTurfCard(turf, vm, { form = turf }, { manageSlots = turf }) } }
    if (form != null) TurfEditor(form!!, vm) { form = null }
    if (manageSlots != null) SlotManager(manageSlots!!, vm) { manageSlots = null }
}

@Composable private fun AdminTurfCard(turf: Turf, vm: AppViewModel, edit: () -> Unit, slots: () -> Unit) { Surface(color = Charcoal, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, Line)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { VenuePhoto(turf, Modifier.size(96.dp, 78.dp).clip(RoundedCornerShape(6.dp))); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Row { Text(turf.name, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); StatusBadge(if (turf.active) "Active" else "Inactive", if (turf.active) Lime else TextMuted) }; Text("${turf.area}, ${turf.city} • ₹${turf.pricePerHour}/hr", color = TextMuted); Row { TextButton(slots) { Text("SLOTS", color = Lime) }; TextButton(edit) { Text("EDIT") }; Switch(turf.active, { vm.setTurfActive(turf.id, it) }, colors = SwitchDefaults.colors(checkedThumbColor = Ink, checkedTrackColor = Lime)) } } } } }

@Composable private fun BookingManagement(vm: AppViewModel) {
    val bookings by vm.bookings.collectAsStateWithLifecycle()
    var filter by remember { mutableStateOf<BookingStatus?>(null) }
    var cancel by remember { mutableStateOf<Booking?>(null) }
    AdminContent {
        item { LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            item { Tag("All", filter == null) { filter = null } }
            items(BookingStatus.entries) { status -> Tag(status.name, filter == status) { filter = status } }
        } }
        items(bookings.filter { filter == null || it.status == filter }, key = { it.id }) { booking ->
            CompactAdminBooking(booking, booking.status == BookingStatus.CONFIRMED) { cancel = booking }
        }
    }
    cancel?.let { booking ->
        AlertDialog(onDismissRequest = { cancel = null }, containerColor = Charcoal,
            title = { Text("Cancel ${booking.id}?") }, text = { Text("The reserved hours will become available again unless blocked. The booking record will be retained.") },
            confirmButton = { TextButton(onClick = { vm.cancelBooking(booking.id); cancel = null }) { Text("CANCEL BOOKING", color = Danger) } },
            dismissButton = { TextButton(onClick = { cancel = null }) { Text("KEEP BOOKING") } })
    }
}

@Composable private fun CompactAdminBooking(b: Booking, action: Boolean = false, onAction: () -> Unit = {}) {
    Surface(color = Charcoal, shape = RoundedCornerShape(7.dp), border = BorderStroke(1.dp, Line)) {
        Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(b.id, color = Lime, style = MaterialTheme.typography.labelMedium)
                    if (b.transactionId.isNotBlank()) {
                        Text(" · Tx: ${b.transactionId}", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                    }
                }
                Text("${b.customer} • ${b.turfName}", fontWeight = FontWeight.Bold)
                Text("${b.date} • ${b.time}", color = TextMuted)
                Text("${b.paymentStatus} (${b.paymentMethod})", color = Lime.copy(alpha = 0.8f), style = MaterialTheme.typography.labelSmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatInr(b.amount), fontWeight = FontWeight.Black)
                StatusBadge(b.status.name, when(b.status){ BookingStatus.CONFIRMED->Lime; BookingStatus.COMPLETED->Color(0xFF55B8FF); BookingStatus.CANCELLED->Danger })
                if(action) TextButton(onAction) { Text(if(b.status==BookingStatus.CONFIRMED) "CANCEL" else "VIEW", color = if(b.status==BookingStatus.CONFIRMED) Danger else Lime) }
            }
        }
    }
}

@Composable private fun UserManagement(vm: AppViewModel) {
    val operations by vm.operations.collectAsStateWithLifecycle()
    val bookings by vm.bookings.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    AdminContent {
        item { OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text("Search name, email or phone") }) }
        items(operations.profiles.filter { (it.name + it.email + it.phone).contains(query, true) }, key = { it.email }) { user ->
            val userBookings = bookings.filter { it.customer == user.name }
            Surface(color = Charcoal, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Line)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text(user.name, fontWeight = FontWeight.Bold); Text(user.email, color = TextMuted); Text(user.phone, color = TextMuted)
                    Text("${userBookings.size} bookings · ${formatInr(userBookings.filter { it.status != BookingStatus.CANCELLED }.sumOf { it.amount })}", color = Lime)
                    val disabled = user.email.lowercase() in operations.disabledEmails
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (disabled) "Sign-in disabled" else "Sign-in enabled", Modifier.weight(1f))
                        Switch(!disabled, { enabled -> vm.updateOperations { it.copy(disabledEmails = if (enabled) it.disabledEmails - user.email.lowercase() else it.disabledEmails + user.email.lowercase()) } })
                    }
                }
            }
        }
    }
}

@Composable private fun PaymentsDashboard(vm: AppViewModel) {
    val bookings by vm.bookings.collectAsStateWithLifecycle()
    var cancelled by remember { mutableStateOf(false) }
    AdminContent {
        item { Text("Simulated payments · no real funds are collected or refunded.", color = TextMuted) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Booking value", formatInr(bookings.filter { it.status != BookingStatus.CANCELLED }.sumOf { it.amount }), "Simulated", Modifier.weight(1f))
            MetricCard("Cancelled value", formatInr(bookings.filter { it.status == BookingStatus.CANCELLED }.sumOf { it.amount }), "No refund transaction", Modifier.weight(1f))
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Tag("Confirmed / completed", !cancelled) { cancelled = false }; Tag("Cancelled", cancelled) { cancelled = true } } }
        items(bookings.filter { (it.status == BookingStatus.CANCELLED) == cancelled }, key = { it.id }) { CompactAdminBooking(it) }
    }
}

@Composable private fun OffersManagement(vm: AppViewModel) {
    val operations by vm.operations.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<VenueOffer?>(null) }
    AdminContent {
        item { Button(onClick = { editing = VenueOffer(java.util.UUID.randomUUID().toString(), "", "") }) { Text("Create offer") } }
        item { Text("These banners appear on Home. Discounts are informational; checkout does not redeem coupons.", color = TextMuted) }
        items(operations.offers, key = { it.id }) { offer ->
            OfferCard(offer.code, "", offer.detail, offer.active)
            Row {
                TextButton(onClick = { editing = offer }) { Text("EDIT") }
                TextButton(onClick = { vm.updateOperations { state -> state.copy(offers = state.offers.map { if (it.id == offer.id) it.copy(active = !it.active) else it }) } }) { Text(if (offer.active) "HIDE" else "PUBLISH") }
            }
        }
    }
    editing?.let { initial ->
        var code by remember(initial.id) { mutableStateOf(initial.code) }
        var detail by remember(initial.id) { mutableStateOf(initial.detail) }
        AlertDialog(onDismissRequest = { editing = null }, containerColor = Charcoal, title = { Text("Offer banner") },
            text = { Column { OutlinedTextField(code, { code = it }, label = { Text("Code") }); OutlinedTextField(detail, { detail = it }, label = { Text("Offer description") }) } },
            confirmButton = { TextButton(enabled = code.isNotBlank() && detail.isNotBlank(), onClick = {
                val saved = initial.copy(code = code.trim().uppercase(), detail = detail.trim())
                vm.updateOperations { it.copy(offers = it.offers.filterNot { o -> o.id == saved.id } + saved) }; editing = null
            }) { Text("SAVE") } }, dismissButton = { TextButton(onClick = { editing = null }) { Text("CANCEL") } })
    }
}

@Composable private fun OfferCard(code:String,discount:String,detail:String,active:Boolean){ Surface(color=Charcoal,shape=RoundedCornerShape(8.dp),border=BorderStroke(1.dp,if(active)Lime.copy(.3f) else Line)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.LocalOffer,null,tint=if(active)Lime else TextMuted);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(code,fontWeight=FontWeight.Black,letterSpacing=1.sp);Text(detail,color=TextMuted)};Column(horizontalAlignment=Alignment.End){Text(discount,color=if(active)Lime else TextMuted,fontWeight=FontWeight.Black);StatusBadge(if(active)"Active" else "Expired",if(active)Lime else TextMuted)}}} }

@Composable private fun ReviewsManagement(vm: AppViewModel) {
    val operations by vm.operations.collectAsStateWithLifecycle()
    val turfs by vm.turfs.collectAsStateWithLifecycle()
    AdminContent {
        if (operations.reviews.isEmpty()) item { Text("No reviews to moderate.", color = TextMuted) }
        items(operations.reviews, key = { it.id }) { review ->
            Surface(color = Charcoal, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Line)) {
                Column(Modifier.fillMaxWidth().padding(14.dp)) {
                    Text("${review.author} · ${turfs.firstOrNull { it.id == review.turfId }?.name ?: "Removed venue"}", fontWeight = FontWeight.Bold)
                    Text("${review.stars}/5", color = Orange); Text(review.text, color = TextMuted)
                    TextButton(onClick = { vm.updateOperations { state -> state.copy(reviews = state.reviews.map { if (it.id == review.id) it.copy(published = !it.published) else it }) } }) { Text(if (review.published) "HIDE REVIEW" else "PUBLISH REVIEW") }
                }
            }
        }
    }
}

@Composable private fun AdminSettings(vm: AppViewModel, logout: () -> Unit) {
    val operations by vm.operations.collectAsStateWithLifecycle()
    AdminContent {
        item { SectionTitle("Operations settings") }
        item { Surface(color = Charcoal, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Line)) {
            Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("Accept new bookings", fontWeight = FontWeight.Bold); Text("Pause or resume availability in the customer app.", color = TextMuted) }
                Switch(operations.settings["acceptBookings"] != false, { enabled -> vm.updateOperations { it.copy(settings = it.settings + ("acceptBookings" to enabled)) } })
            }
        } }
        item { Text("Account and venue data use Firebase. Billing remains disabled: photo uploads and online booking are paused. Existing local demo data has not been uploaded.", color = TextMuted) }
        item { OutlinedButton(logout, Modifier.fillMaxWidth()) { Text("LOGOUT ADMIN", color = Danger) } }
    }
}
