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
    val bookings by vm.bookings.collectAsStateWithLifecycle(); var tab by remember { mutableIntStateOf(0) }
    val statuses = listOf(BookingStatus.CONFIRMED, BookingStatus.COMPLETED, BookingStatus.CANCELLED); val list = bookings.filter { it.status == statuses[tab] }
    UserScaffold(Routes.BOOKINGS, nav, "My bookings") { padding -> Column(Modifier.fillMaxSize().padding(padding)) {
        TabRow(tab, containerColor = Ink, contentColor = Lime) { listOf("Upcoming", "Completed", "Cancelled").forEachIndexed { index, label -> Tab(tab == index, { tab = index }, text = { Text(label.uppercase(), style = MaterialTheme.typography.labelMedium) }) } }
        if (list.isEmpty()) EmptyBookings { nav.navigate(Routes.EXPLORE) } else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { items(list, key = { it.id }) { BookingCard(it, vm) } }
    } }
}

@Composable private fun BookingCard(booking: Booking, vm: AppViewModel) {
    var confirmCancel by remember { mutableStateOf(false) }
    val statusColor = when (booking.status) { BookingStatus.CONFIRMED -> Lime; BookingStatus.COMPLETED -> Color(0xFF55B8FF); BookingStatus.CANCELLED -> Danger }
    Card(colors = CardDefaults.cardColors(containerColor = Charcoal), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Line)) { Column(Modifier.padding(16.dp)) {
        Row { Column(Modifier.weight(1f)) { Text(booking.turfName.uppercase(), style = MaterialTheme.typography.titleLarge); Text(booking.id, color = TextMuted, style = MaterialTheme.typography.labelMedium) }; StatusBadge(booking.status.name, statusColor) }
        HorizontalDivider(Modifier.padding(vertical = 14.dp), color = Line)
        Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.CalendarMonth, null, Modifier.size(18.dp), tint = Lime); Text(" ${booking.date}  •  ${booking.time}", color = TextMuted, modifier = Modifier.weight(1f)); Text(formatInr(booking.amount), fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(14.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton({ vm.showMessage("Booking ${booking.id} is shown above.") }, Modifier.weight(1f), shape = RoundedCornerShape(6.dp), border = BorderStroke(1.dp, Line)) { Text("VIEW DETAILS", style = MaterialTheme.typography.labelMedium) }; if (booking.status == BookingStatus.CONFIRMED) OutlinedButton({ confirmCancel = true }, Modifier.weight(1f), shape = RoundedCornerShape(6.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger), border = BorderStroke(1.dp, Danger.copy(.5f))) { Text("CANCEL", style = MaterialTheme.typography.labelMedium) } }
    } }
    if (confirmCancel) AlertDialog(onDismissRequest = { confirmCancel = false }, containerColor = Charcoal, title = { Text("Cancel booking?") }, text = { Text("Free cancellation is available until 6 hours before the slot. This demo will mark the booking cancelled.") }, confirmButton = { TextButton({ vm.cancelBooking(booking.id); confirmCancel = false }) { Text("CANCEL BOOKING", color = Danger) } }, dismissButton = { TextButton({ confirmCancel = false }) { Text("KEEP BOOKING") } })
}

@Composable private fun EmptyBookings(action: () -> Unit) { Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Outlined.ConfirmationNumber, null, Modifier.size(58.dp), tint = TextMuted); Spacer(Modifier.height(16.dp)); Text("NO BOOKINGS YET", style = MaterialTheme.typography.headlineMedium); Text("Your next game starts with finding an arena.", color = TextMuted, textAlign = TextAlign.Center); Spacer(Modifier.height(20.dp)); Button(action, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) { Text("EXPLORE TURFS") } } }

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
    val p by vm.profile; var confirmLogout by remember { mutableStateOf(false) }
    UserScaffold(Routes.PROFILE, nav, "Profile") { padding -> LazyColumn(Modifier.padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { PlayerProfileHeader(p) }
        item { Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) { ProfileStat("08", "GAMES", Modifier.weight(1f)); ProfileStat("05", "WINS", Modifier.weight(1f)); ProfileStat("4.8", "RATING", Modifier.weight(1f)) } }
        item { Surface(color = Lime.copy(.08f), shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Lime.copy(.28f))) { Column(Modifier.padding(15.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.EmojiEvents, null, tint = Lime); Spacer(Modifier.width(9.dp)); Text("GAME PROFILE", color = Lime, style = MaterialTheme.typography.labelLarge) }; Spacer(Modifier.height(10.dp)); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Tag("Football"); Tag("Striker"); Tag("Evening") }; Spacer(Modifier.height(12.dp)); Text("72% TO LEVEL 13", color = TextMuted, style = MaterialTheme.typography.labelMedium); LinearProgressIndicator({ .72f }, Modifier.fillMaxWidth().padding(top = 6.dp).height(6.dp), color = Lime, trackColor = Line) } } }
        item { ProfileGroup(listOf(ProfileItem("My Bookings", Icons.Outlined.ConfirmationNumber) { nav.navigate(Routes.BOOKINGS) }, ProfileItem("Favorite Turfs", Icons.Outlined.FavoriteBorder) { nav.navigate(Routes.FAVORITES) }, ProfileItem("Payment History", Icons.Outlined.ReceiptLong) { vm.showMessage("Demo payment history is available in bookings.") })) }
        item { ProfileGroup(listOf(ProfileItem("Notifications", Icons.Outlined.Notifications) { nav.navigate(Routes.NOTIFICATIONS) }, ProfileItem("Help & Support", Icons.Outlined.HelpOutline) { vm.showMessage("Support chat is simulated in this demo.") }, ProfileItem("Terms & Conditions", Icons.Outlined.Description) {}, ProfileItem("Privacy Policy", Icons.Outlined.Shield) {})) }
        item { OutlinedButton({ confirmLogout = true }, Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(8.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Danger), border = BorderStroke(1.dp, Danger.copy(.5f))) { Icon(Icons.Default.Logout, null); Spacer(Modifier.width(8.dp)); Text("LOGOUT") } }
        item { Text("BOOKMYTURF • DEMO BUILD 1.0", Modifier.fillMaxWidth().padding(16.dp), color = TextMuted.copy(.6f), style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center) }
    } }
    if (confirmLogout) AlertDialog(onDismissRequest = { confirmLogout = false }, containerColor = Charcoal, title = { Text("Leave the game?") }, text = { Text("You can sign back in anytime.") }, confirmButton = { TextButton(onClick = onLogout) { Text("LOGOUT", color = Danger) } }, dismissButton = { TextButton({ confirmLogout = false }) { Text("STAY") } })
}

@Composable private fun PlayerProfileHeader(p: UserProfile) {
    Box(Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(14.dp))) {
        Image(painterResource(R.drawable.player_arjun), p.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Ink.copy(.16f), Ink.copy(.96f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text(p.name.uppercase(), style = MaterialTheme.typography.headlineLarge); Spacer(Modifier.width(8.dp)); Icon(Icons.Default.Verified, null, tint = Lime) }
            Text("STRIKER • LEVEL 12 • VADODARA", color = Lime, style = MaterialTheme.typography.labelMedium)
            Text("${p.phone}  •  ${p.email}", color = Color.White.copy(.72f), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable private fun ProfileStat(value: String, label: String, modifier: Modifier = Modifier) { Surface(modifier, color = Charcoal, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Line)) { Column(Modifier.padding(vertical = 13.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(value, style = MaterialTheme.typography.headlineMedium, color = Lime); Text(label, color = TextMuted, style = MaterialTheme.typography.labelMedium) } } }

private data class ProfileItem(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val action: () -> Unit)
@Composable private fun ProfileGroup(items: List<ProfileItem>) { Surface(color = Charcoal, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Line)) { Column { items.forEachIndexed { index, item -> Row(Modifier.fillMaxWidth().clickable(onClick = item.action).padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(item.icon, null, tint = Lime); Spacer(Modifier.width(14.dp)); Text(item.label, Modifier.weight(1f), fontWeight = FontWeight.Bold); Icon(Icons.Default.ChevronRight, null, tint = TextMuted) }; if (index < items.lastIndex) HorizontalDivider(color = Line) } } } }
