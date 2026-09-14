package com.bookmyturf.app.ui.screens

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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
import com.bookmyturf.app.ui.AppViewModel
import com.bookmyturf.app.ui.components.*
import com.bookmyturf.app.ui.theme.*

@Composable
fun MyBookingsScreen(vm: AppViewModel, nav: NavController) {
    val bookings by vm.bookings.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val statuses = listOf(BookingStatus.CONFIRMED, BookingStatus.COMPLETED, BookingStatus.CANCELLED)
    val labels = listOf("Upcoming", "Completed", "Cancelled")
    val filtered = bookings.filter { it.status == statuses[tab] }
    LazyColumn(
        Modifier.fillMaxSize().background(Ink).statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { AccountHeading("Your games", "All your plans, in one place.") }
        item {
            Row(Modifier.fillMaxWidth().background(Steel, RoundedCornerShape(18.dp)).padding(5.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                labels.forEachIndexed { index, label ->
                    Surface(onClick = { tab = index }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp),
                        color = if (tab == index) PulsePurple.copy(.3f) else Color.Transparent) {
                        Text(label, Modifier.padding(vertical = 14.dp), textAlign = TextAlign.Center,
                            color = if (tab == index) PulseLavender else TextMuted, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { Text("${filtered.size} ${labels[tab].lowercase()} bookings", color = TextMuted, style = MaterialTheme.typography.bodySmall) }
        if (filtered.isEmpty()) item {
            Surface(color = Charcoal, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Line)) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(Icons.Outlined.ConfirmationNumber, null, Modifier.size(44.dp), tint = PulseLavender)
                    Text("No ${labels[tab].lowercase()} games", fontWeight = FontWeight.ExtraBold)
                    Text("Your next game starts with finding an arena.", color = TextMuted, textAlign = TextAlign.Center)
                    Button(onClick = { nav.navigate(Routes.EXPLORE) }, colors = ButtonDefaults.buttonColors(containerColor = PulsePurple, contentColor = Color.White), shape = RoundedCornerShape(14.dp)) { Text("Explore turfs") }
                }
            }
        }
        items(filtered, key = { it.id }) { BookingCard(it, vm) }
    }
}

@Composable private fun BookingCard(booking: Booking, vm: AppViewModel) {
    var confirmCancel by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    val statusColor = when (booking.status) { BookingStatus.CONFIRMED -> PulseLavender; BookingStatus.COMPLETED -> Color(0xFF55B8FF); BookingStatus.CANCELLED -> Danger }
    Surface(color = Charcoal, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Line)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(PulsePurple.copy(.2f), RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SportsSoccer, null, tint = PulseLavender)
                }
                Column(Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(booking.turfName, fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    Text(booking.id, color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Surface(color = statusColor.copy(.12f), shape = RoundedCornerShape(50)) {
                Text(booking.status.name.lowercase().replaceFirstChar { it.uppercase() }, Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = statusColor, style = MaterialTheme.typography.labelSmall)
            }
            HorizontalDivider(color = Line)
            AccountInfo(Icons.Default.CalendarMonth, booking.date)
            AccountInfo(Icons.Default.Schedule, booking.time)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("${booking.durationHours} hour${if (booking.durationHours == 1) "" else "s"}", Modifier.weight(1f), color = TextMuted, style = MaterialTheme.typography.bodySmall)
                Text(formatInr(booking.amount), fontWeight = FontWeight.ExtraBold, color = TextPrimary)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = { showDetails = true }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = PulsePurple.copy(.25f), contentColor = PulseLavender)) { Text("Details") }
                if (booking.status == BookingStatus.CONFIRMED) OutlinedButton(onClick = { confirmCancel = true }, Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger), border = BorderStroke(1.dp, Danger.copy(.3f))) { Text("Cancel") }
            }
        }
    }
    if (showDetails) AlertDialog(
        onDismissRequest = { showDetails = false }, containerColor = Charcoal, shape = RoundedCornerShape(24.dp),
        title = { Text(booking.turfName, fontFamily = ManropeUi, fontWeight = FontWeight.Bold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(booking.id, color = PulseLavender)
            Text(booking.date); Text(booking.time)
            Text("${booking.durationHours} hour(s) · ${formatInr(booking.amount)}")
            Text("Status: ${booking.status.name.lowercase()}")
        } }, confirmButton = { TextButton(onClick = { showDetails = false }) { Text("Done", color = PulseLavender) } }
    )
    if (confirmCancel) AlertDialog(onDismissRequest = { confirmCancel = false }, containerColor = Charcoal, shape = RoundedCornerShape(24.dp), title = { Text("Cancel booking?") }, text = { Text("Free cancellation is available until 6 hours before the slot. This demo will mark the booking cancelled.") }, confirmButton = { TextButton({ vm.cancelBooking(booking.id); confirmCancel = false }) { Text("Cancel booking", color = Danger) } }, dismissButton = { TextButton({ confirmCancel = false }) { Text("Keep booking", color = PulseLavender) } })
}

@Composable
fun FavoritesScreen(vm: AppViewModel, nav: NavController) {
    val turfs by vm.turfs.collectAsStateWithLifecycle(); val favorites = remember(turfs) { turfs.filter { it.isFavorite } }
    Scaffold(topBar = { UserTopBar("My turfs", nav, true) }, containerColor = Ink) { padding ->
        if (favorites.isEmpty()) Column(Modifier.fillMaxSize().padding(padding).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Outlined.FavoriteBorder, null, Modifier.size(60.dp), tint = TextMuted); Text("NO FAVORITES", style = MaterialTheme.typography.headlineMedium); Text("Tap the heart on a turf to keep it here.", color = TextMuted, textAlign = TextAlign.Center) }
        else LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { item { Text("YOUR GO-TO ARENAS", color = TextMuted, style = MaterialTheme.typography.labelMedium) }; items(favorites, key = { it.id }) { turf -> TurfCard(turf, { nav.navigate(Routes.turf(turf.id)) }, { vm.toggleFavorite(turf.id) }) } }
    }
}

@Composable
fun NotificationsScreen(vm: AppViewModel, nav: NavController) {
    val notifications by vm.notifications.collectAsStateWithLifecycle()
    Scaffold(topBar = { UserTopBar("Notifications", nav, true) }, containerColor = Ink) { padding -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        item { Row(Modifier.fillMaxWidth().padding(bottom = 14.dp)) { Text("GAME UPDATES", color = TextMuted, style = MaterialTheme.typography.labelMedium); Spacer(Modifier.weight(1f)); Text("MARK ALL READ", color = Lime, style = MaterialTheme.typography.labelMedium) } }
        items(notifications, key = { it.id }) { n -> NotificationItem(n) }
    } }
}

@Composable private fun NotificationItem(item: AppNotification) { val icon = when (item.type) { "booking" -> Icons.Default.CheckCircle; "reminder" -> Icons.Default.Alarm; "payment" -> Icons.Default.Payments; else -> Icons.Default.SportsSoccer }; Surface(color = if (item.unread) Lime.copy(.055f) else Ink, border = BorderStroke(0.5.dp, Line)) { Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) { Box(Modifier.size(42.dp).background(if (item.unread) Lime else Steel, CircleShape), contentAlignment = Alignment.Center) { Icon(icon, null, tint = if (item.unread) Ink else TextMuted, modifier = Modifier.size(21.dp)) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(item.title, fontWeight = FontWeight.Bold); Text(item.message, color = TextMuted); Text(item.time.uppercase(), color = if (item.unread) Lime else TextMuted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 6.dp)) }; if (item.unread) Box(Modifier.size(7.dp).background(Lime, CircleShape)) } } }

@Composable
fun ProfileScreen(vm: AppViewModel, nav: NavController, onLogout: () -> Unit) {
    val p by vm.profile
    val bookings by vm.bookings.collectAsStateWithLifecycle()
    val turfs by vm.turfs.collectAsStateWithLifecycle()
    var confirmLogout by remember { mutableStateOf(false) }
    LazyColumn(
        Modifier.fillMaxSize().background(Ink).statusBarsPadding(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item { AccountHeading("Your profile", "A little about you. Everything for your game.") }
        item { PlayerProfileHeader(p) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfileStat(bookings.count { it.status == BookingStatus.CONFIRMED }.toString(), "Upcoming", Modifier.weight(1f))
            ProfileStat(bookings.count { it.status == BookingStatus.COMPLETED }.toString(), "Played", Modifier.weight(1f))
            ProfileStat(turfs.count { it.isFavorite }.toString(), "Saved turfs", Modifier.weight(1f))
        } }
        item { Text("Your activity", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium) }
        item { ProfileGroup(listOf(
            ProfileItem("My bookings", Icons.Outlined.ConfirmationNumber) { nav.navigate(Routes.BOOKINGS) },
            ProfileItem("Saved turfs", Icons.Outlined.FavoriteBorder) { nav.navigate(Routes.FAVORITES) },
            ProfileItem("Payment history", Icons.Outlined.ReceiptLong) { vm.showMessage("Demo payment history is available in bookings.") }
        )) }
        item { Text("Support & preferences", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium) }
        item { ProfileGroup(listOf(
            ProfileItem("Notifications", Icons.Outlined.Notifications) { nav.navigate(Routes.NOTIFICATIONS) },
            ProfileItem("Help & support", Icons.Outlined.HelpOutline) { vm.showMessage("Support chat is simulated in this demo.") },
            ProfileItem("Terms & conditions", Icons.Outlined.Description) { vm.showMessage("Terms & conditions are not available in this demo.") },
            ProfileItem("Privacy policy", Icons.Outlined.Shield) { vm.showMessage("Privacy policy is not available in this demo.") }
        )) }
        item { OutlinedButton(onClick = { confirmLogout = true }, Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger), border = BorderStroke(1.dp, Danger.copy(.3f))) {
            Icon(Icons.Default.Logout, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Log out")
        } }
        item { Text("BookMyTurf · Demo", Modifier.fillMaxWidth(), color = TextMuted.copy(.6f), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center) }
    }
    if (confirmLogout) AlertDialog(onDismissRequest = { confirmLogout = false }, containerColor = Charcoal, shape = RoundedCornerShape(24.dp), title = { Text("Log out?") }, text = { Text("You can sign back in anytime.") }, confirmButton = { TextButton(onClick = onLogout) { Text("Log out", color = Danger) } }, dismissButton = { TextButton({ confirmLogout = false }) { Text("Stay", color = PulseLavender) } })
}

@Composable private fun PlayerProfileHeader(p: UserProfile) {
    Surface(color = Charcoal, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Line)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).background(Brush.linearGradient(listOf(PulsePurple, PulsePurpleDark)), RoundedCornerShape(22.dp)), contentAlignment = Alignment.Center) {
                    Text(p.name.split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.take(1).uppercase() }, color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
                }
                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(p.name, fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    Text("Your player account", color = PulseLavender, style = MaterialTheme.typography.bodySmall)
                }
            }
            HorizontalDivider(color = Line)
            AccountInfo(Icons.Default.LocationOn, p.location)
            AccountInfo(Icons.Default.Call, p.phone)
            AccountInfo(Icons.Default.Email, p.email)
        }
    }
}

@Composable private fun ProfileStat(value: String, label: String, modifier: Modifier = Modifier) {
    Surface(modifier, color = Charcoal, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Line)) {
        Column(Modifier.padding(vertical = 18.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall, color = PulseLavender)
            Text(label, color = TextMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}

@Composable private fun AccountHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(title, fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
        Text(subtitle, color = TextMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable private fun AccountInfo(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = PulseLavender)
        Text(text, Modifier.padding(start = 10.dp), color = TextMuted, style = MaterialTheme.typography.bodyMedium)
    }
}

private data class ProfileItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val action: () -> Unit)
@Composable private fun ProfileGroup(items: List<ProfileItem>) {
    Surface(color = Charcoal, shape = RoundedCornerShape(22.dp), border = BorderStroke(1.dp, Line)) {
        Column {
            items.forEachIndexed { index, item ->
                Row(Modifier.fillMaxWidth().clickable(onClick = item.action).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).background(PulsePurple.copy(.16f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Icon(item.icon, null, Modifier.size(20.dp), tint = PulseLavender)
                    }
                    Text(item.label, Modifier.weight(1f).padding(horizontal = 12.dp), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(20.dp))
                }
                if (index < items.lastIndex) HorizontalDivider(Modifier.padding(start = 64.dp, end = 16.dp), color = Line)
            }
        }
    }
}
