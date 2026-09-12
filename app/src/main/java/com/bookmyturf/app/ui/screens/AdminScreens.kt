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
    BOOKINGS("Bookings", Icons.Default.ConfirmationNumber), USERS("Users", Icons.Default.Groups),
    PAYMENTS("Payments", Icons.Default.Payments), OFFERS("Offers", Icons.Default.LocalOffer),
    REVIEWS("Reviews", Icons.Default.Star), SETTINGS("Settings", Icons.Default.Settings)
}

@Composable
fun AdminDashboardScreen(vm: AppViewModel, nav: NavController, onLogout: () -> Unit) {
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
                    AdminSection.BOOKINGS -> BookingManagement(vm)
                    AdminSection.USERS -> UserManagement()
                    AdminSection.PAYMENTS -> PaymentsDashboard(vm)
                    AdminSection.OFFERS -> OffersManagement(vm)
                    AdminSection.REVIEWS -> ReviewsManagement(vm)
                    AdminSection.SETTINGS -> AdminSettings(onLogout)
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

@Composable private fun AdminHeader(section: AdminSection, logout: () -> Unit) { Row(Modifier.fillMaxWidth().statusBarsPadding().height(70.dp).background(Ink).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(section.label.uppercase(), style = MaterialTheme.typography.headlineMedium); Text("BOOKMYTURF OPERATIONS", color = TextMuted, style = MaterialTheme.typography.labelMedium) }; Surface(color = Lime.copy(.12f), shape = RoundedCornerShape(6.dp)) { Text("LIVE DEMO", Modifier.padding(horizontal = 9.dp, vertical = 6.dp), color = Lime, style = MaterialTheme.typography.labelMedium) }; IconButton(logout) { Icon(Icons.Default.Logout, "Logout", tint = TextMuted) } } }

@Composable private fun AdminContent(content: LazyListScope.() -> Unit) { LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(14.dp), content = content) }

@Composable private fun AdminHome(vm: AppViewModel) {
    val bookings by vm.bookings.collectAsStateWithLifecycle(); val turfs by vm.turfs.collectAsStateWithLifecycle(); var range by remember { mutableStateOf("7 Days") }
    val confirmedRevenue = bookings.filter { it.status != BookingStatus.CANCELLED }.sumOf { it.amount }
    AdminContent {
        item { Text("TODAY AT A GLANCE", color = TextMuted, style = MaterialTheme.typography.labelMedium) }
        item { BoxWithConstraints { val columns = if (maxWidth > 680.dp) 3 else 2; val cards = listOf(Triple("Today's Revenue", "₹${48_500 + confirmedRevenue.toInt()}", "+12.4%"), Triple("Today's Bookings", "${64 + bookings.count { it.status == BookingStatus.CONFIRMED }}", "+8.1%"), Triple("Active Turfs", "${12 + turfs.count { it.active }}", "+2"), Triple("Registered Users", "2,481", "+46"), Triple("Occupancy", "78%", "+5.2%")); Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { cards.chunked(columns).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) { row.forEach { metric -> MetricCard(metric.first, metric.second, metric.third, Modifier.weight(1f)) }; repeat(columns - row.size) { Spacer(Modifier.weight(1f)) } } } } } }
        item { Surface(color = Charcoal, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Line)) { Column(Modifier.padding(16.dp)) { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("REVENUE", style = MaterialTheme.typography.titleLarge); Text("₹3.89L THIS WEEK", color = Lime, style = MaterialTheme.typography.labelLarge) }; Tag(range, true) { range = when (range) { "Today" -> "7 Days"; "7 Days" -> "30 Days"; "30 Days" -> "12 Months"; else -> "Today" } } }; Spacer(Modifier.height(16.dp)); RevenueChart(listOf(32, 41, 38, 52, 61, 89, 76), Modifier.fillMaxWidth().height(190.dp)); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN").forEach { Text(it, color = TextMuted, style = MaterialTheme.typography.labelMedium) } } } } }
        item { BoxWithConstraints { val wide = maxWidth > 640.dp; if (wide) Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { OccupancyCard(Modifier.weight(1f)); PeakHoursCard(Modifier.weight(1f)) } else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { OccupancyCard(); PeakHoursCard() } } }
        item { SectionTitle("Recent bookings") }
        items(bookings.take(4), key = { it.id }) { b -> CompactAdminBooking(b) }
    }
}

@Composable private fun MetricCard(label: String, value: String, delta: String, modifier: Modifier = Modifier) { Surface(modifier, color = Charcoal, shape = RoundedCornerShape(8.dp), border = BorderStroke(1.dp, Line)) { Column(Modifier.padding(14.dp)) { Text(label.uppercase(), color = TextMuted, style = MaterialTheme.typography.labelMedium, maxLines = 1); Spacer(Modifier.height(8.dp)); Text(value, style = MaterialTheme.typography.headlineMedium, maxLines = 1); Spacer(Modifier.height(5.dp)); Text("▲ $delta", color = Lime, style = MaterialTheme.typography.labelMedium) } } }

@Composable private fun RevenueChart(values: List<Int>, modifier: Modifier) { val progress by animateFloatAsState(1f, tween(900), label = "chart"); Canvas(modifier) { val maxV = values.maxOrNull()!!.toFloat(); val step = size.width / (values.size - 1); val points = values.mapIndexed { i, v -> Offset(i * step, size.height - (v / maxV * size.height * .82f) - 10f) }; points.zipWithNext().forEach { (a,b) -> drawLine(Lime, a, Offset(a.x + (b.x-a.x)*progress, a.y+(b.y-a.y)*progress), 5f) }; points.forEach { drawCircle(Lime, 7f, it); drawCircle(Ink, 3f, it) }; repeat(4) { y -> drawLine(Line.copy(.7f), Offset(0f, size.height * y/4), Offset(size.width, size.height*y/4), 1f) } } }
@Composable private fun OccupancyCard(modifier: Modifier = Modifier) { Surface(modifier, color = Charcoal, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Line)) { Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) { Canvas(Modifier.fillMaxSize()) { drawArc(Line, -90f, 360f, false, style = Stroke(11f)); drawArc(Lime, -90f, 280.8f, false, style = Stroke(11f)) }; Text("78%", fontWeight = FontWeight.Black, fontSize = 20.sp) }; Spacer(Modifier.width(16.dp)); Column { Text("TURF OCCUPANCY", style = MaterialTheme.typography.titleMedium); Text("Prime time is 6–9 PM", color = TextMuted); Text("+5.2% vs last week", color = Lime, style = MaterialTheme.typography.labelMedium) } } } }
@Composable private fun PeakHoursCard(modifier: Modifier = Modifier) { Surface(modifier, color = Charcoal, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Line)) { Column(Modifier.padding(18.dp)) { Text("POPULAR SPORTS", style = MaterialTheme.typography.titleMedium); Spacer(Modifier.height(10.dp)); listOf("Football" to .86f, "Cricket" to .72f, "Badminton" to .48f).forEach { (name, value) -> Row { Text(name, Modifier.width(90.dp), color = TextMuted); LinearProgressIndicator({ value }, Modifier.weight(1f).height(8.dp), color = Lime, trackColor = Line); Text(" ${(value*100).toInt()}%", style = MaterialTheme.typography.labelMedium) }; Spacer(Modifier.height(9.dp)) } } } }

@Composable private fun TurfManagement(vm: AppViewModel) {
    val turfs by vm.turfs.collectAsStateWithLifecycle(); var form by remember { mutableStateOf<Turf?>(null) }; var manageSlots by remember { mutableStateOf<Turf?>(null) }
    AdminContent { item { Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("${turfs.size} VENUES", color = TextMuted, style = MaterialTheme.typography.labelMedium); Text("Manage turf inventory and availability.") }; Button({ form = Turf("", "", "Vadodara", "", listOf(Sport.FOOTBALL), 800, 4.5, 1.0, "", listOf("Parking")) }, shape = RoundedCornerShape(6.dp), colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) { Icon(Icons.Default.Add, null); Text(" ADD TURF") } } }; items(turfs, key = { it.id }) { turf -> AdminTurfCard(turf, vm, { form = turf }, { manageSlots = turf }) } }
    if (form != null) TurfEditor(form!!, { form = null }) { vm.upsertTurf(it); form = null }
    if (manageSlots != null) SlotManager(manageSlots!!) { manageSlots = null }
}

@Composable private fun AdminTurfCard(turf: Turf, vm: AppViewModel, edit: () -> Unit, slots: () -> Unit) { Surface(color = Charcoal, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, Line)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { TurfVisual(turf.visualSeed, Modifier.size(96.dp, 78.dp).clip(RoundedCornerShape(6.dp))); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Row { Text(turf.name, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis); StatusBadge(if (turf.active) "Active" else "Inactive", if (turf.active) Lime else TextMuted) }; Text("${turf.area}, ${turf.city} • ₹${turf.pricePerHour}/hr", color = TextMuted); Row { TextButton(slots) { Text("SLOTS", color = Lime) }; TextButton(edit) { Text("EDIT") }; Switch(turf.active, { vm.setTurfActive(turf.id, it) }, colors = SwitchDefaults.colors(checkedThumbColor = Ink, checkedTrackColor = Lime)) } } } } }

@Composable private fun TurfEditor(initial: Turf, close: () -> Unit, save: (Turf) -> Unit) { var name by remember { mutableStateOf(initial.name) }; var area by remember { mutableStateOf(initial.area) }; var city by remember { mutableStateOf(initial.city) }; var price by remember { mutableStateOf(initial.pricePerHour.toString()) }; var desc by remember { mutableStateOf(initial.description) }; ModalBottomSheet(close, containerColor = Charcoal) { Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp).navigationBarsPadding()) { SectionTitle(if (initial.id.isBlank()) "Add turf" else "Edit turf"); listOf(Triple("Turf name", name, { v: String -> name = v }), Triple("Area / address", area, { v: String -> area = v }), Triple("City", city, { v: String -> city = v }), Triple("Price per hour", price, { v: String -> price = v.filter(Char::isDigit) }), Triple("Description", desc, { v: String -> desc = v })).forEach { (label, value, change) -> OutlinedTextField(value, change, Modifier.fillMaxWidth().padding(vertical = 6.dp), label = { Text(label) }, shape = RoundedCornerShape(7.dp)) }; Text("Sports • Facilities • Opening 06:00 • Closing 23:00 • Latitude/Longitude • Images", color = TextMuted, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 12.dp)); PrimaryButton("Save Turf", { if (name.isNotBlank()) save(initial.copy(id = initial.id.ifBlank { name.lowercase().replace(" ", "-") }, name = name, area = area, city = city, pricePerHour = price.toIntOrNull() ?: 800, description = desc)) }) } } }

@Composable private fun SlotManager(turf: Turf, close: () -> Unit) { val slots = remember { DemoBookMyTurfRepository.slots().toMutableStateList() }; ModalBottomSheet(close, containerColor = Charcoal) { Column(Modifier.padding(18.dp).navigationBarsPadding()) { SectionTitle("${turf.name} slots"); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Tag("10 Sep", true); Tag("11 Sep"); Tag("12 Sep") }; Spacer(Modifier.height(14.dp)); LazyColumn(Modifier.heightIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { itemsIndexed(slots) { i, slot -> Surface(color = Steel, shape = RoundedCornerShape(6.dp)) { Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Text(slot.time, Modifier.weight(1f), fontWeight = FontWeight.Bold); Text("₹${slot.price}", color = Lime); Spacer(Modifier.width(12.dp)); Tag(slot.state.name) { slots[i] = slot.copy(state = when(slot.state) { SlotState.AVAILABLE -> SlotState.BLOCKED; SlotState.BLOCKED -> SlotState.BOOKED; SlotState.BOOKED -> SlotState.AVAILABLE }) } } } } }; Spacer(Modifier.height(12.dp)); PrimaryButton("Save Slot Changes", close) } } }

@Composable private fun BookingManagement(vm: AppViewModel) { val bookings by vm.bookings.collectAsState(); var filter by remember { mutableStateOf<BookingStatus?>(null) }; val filtered = bookings.filter { filter == null || it.status == filter }; AdminContent { item { LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) { item { Tag("All", filter == null) { filter = null } }; items(BookingStatus.entries) { s -> Tag(s.name, filter == s) { filter = s } } } }; items(filtered) { b -> CompactAdminBooking(b, true) { if (b.status == BookingStatus.CONFIRMED) vm.cancelBooking(b.id) else vm.showMessage("Booking ${b.id} reviewed.") } } } }
@Composable private fun CompactAdminBooking(b: Booking, action: Boolean = false, onAction: () -> Unit = {}) { Surface(color = Charcoal, shape = RoundedCornerShape(7.dp), border = BorderStroke(1.dp, Line)) { Row(Modifier.fillMaxWidth().padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(b.id, color = Lime, style = MaterialTheme.typography.labelMedium); Text("${b.customer} • ${b.turfName}", fontWeight = FontWeight.Bold); Text("${b.date} • ${b.time}", color = TextMuted) }; Column(horizontalAlignment = Alignment.End) { Text(formatInr(b.amount), fontWeight = FontWeight.Black); StatusBadge(b.status.name, when(b.status){ BookingStatus.CONFIRMED->Lime; BookingStatus.COMPLETED->Color(0xFF55B8FF); BookingStatus.CANCELLED->Danger }); if(action) TextButton(onAction) { Text(if(b.status==BookingStatus.CONFIRMED) "CANCEL" else "VIEW", color = if(b.status==BookingStatus.CONFIRMED) Danger else Lime) } } } } }

@Composable private fun UserManagement() { val users = remember { listOf(AdminUser("Arjun Patel", "arjun.patel@example.com", "+91 98765 43210", 12, 14860, true), AdminUser("Neel Shah", "neel.shah@example.com", "+91 98250 11882", 8, 9120, true), AdminUser("Riya Desai", "riya.desai@example.com", "+91 99099 67420", 5, 6800, true), AdminUser("Kabir Singh", "kabir.singh@example.com", "+91 97122 44210", 2, 1900, false)) }; AdminContent { item { OutlinedTextField("", {}, Modifier.fillMaxWidth(), placeholder = { Text("Search user, email or phone") }, leadingIcon = { Icon(Icons.Default.Search, null) }, shape = RoundedCornerShape(7.dp)) }; items(users) { u -> Surface(color = Charcoal, border = BorderStroke(1.dp, Line), shape = RoundedCornerShape(8.dp)) { Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(42.dp).background(if(u.active) Lime else TextMuted, CircleShape), contentAlignment = Alignment.Center) { Text(u.name.take(1), color = Ink, fontWeight = FontWeight.Black) }; Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(u.name, fontWeight = FontWeight.Bold); Text(u.email, color = TextMuted); Text("${u.bookings} bookings • ₹${u.spend} spend", color = Lime, style = MaterialTheme.typography.labelMedium) }; StatusBadge(if(u.active) "Active" else "Disabled", if(u.active) Lime else Danger) } } } } }

@Composable private fun PaymentsDashboard(vm: AppViewModel) { val b by vm.bookings.collectAsState(); val payments = b.mapIndexed { i, it -> PaymentRecord("TXN-BMT-${90210+i}", it.id, it.customer, it.amount, listOf("Google Pay","PhonePe","Paytm")[i%3], if(it.status==BookingStatus.CANCELLED) "Refunded" else "Success", it.date) }; AdminContent { item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { MetricCard("Total revenue", "₹4.82L", "+14%", Modifier.weight(1f)); MetricCard("Refunds", "₹18.4K", "-2.1%", Modifier.weight(1f)) } }; item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Tag("Successful", true); Tag("Pending"); Tag("Failed") } }; items(payments) { p -> Surface(color = Charcoal, border = BorderStroke(1.dp, Line), shape = RoundedCornerShape(7.dp)) { Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Payments, null, tint = Lime); Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text(p.transactionId, fontWeight = FontWeight.Bold); Text("${p.user} • ${p.method}", color = TextMuted); Text(p.bookingId, color = TextMuted, style = MaterialTheme.typography.labelMedium) }; Column(horizontalAlignment = Alignment.End) { Text(formatInr(p.amount), fontWeight = FontWeight.Black); StatusBadge(p.status, if(p.status=="Success") Lime else Orange) } } } } } }

@Composable private fun OffersManagement(vm: AppViewModel) { var show by remember { mutableStateOf(false) }; AdminContent { item { Row(verticalAlignment = Alignment.CenterVertically) { Text("ACTIVE CAMPAIGNS", Modifier.weight(1f), color = TextMuted, style = MaterialTheme.typography.labelMedium); Button({ show = true }, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink), shape = RoundedCornerShape(6.dp)) { Icon(Icons.Default.Add, null); Text(" CREATE") } } }; item { OfferCard("FIRSTGAME", "20% OFF", "Max ₹200 • First booking", true) }; item { OfferCard("WEEKDAY10", "₹100 OFF", "Mon–Thu • Min ₹800", true) }; item { OfferCard("MONSOON", "15% OFF", "Expired 31 Aug", false) } }; if(show) AlertDialog({ show=false }, containerColor=Charcoal, title={Text("Create offer")}, text={Column{OutlinedTextField("",{},label={Text("Coupon code")}); OutlinedTextField("",{},label={Text("Discount / validity")})}}, confirmButton={TextButton({show=false;vm.showMessage("Demo offer created.")}){Text("CREATE",color=Lime)}}, dismissButton={TextButton({show=false}){Text("CANCEL")}}) }
@Composable private fun OfferCard(code:String,discount:String,detail:String,active:Boolean){ Surface(color=Charcoal,shape=RoundedCornerShape(8.dp),border=BorderStroke(1.dp,if(active)Lime.copy(.3f) else Line)){Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.LocalOffer,null,tint=if(active)Lime else TextMuted);Spacer(Modifier.width(12.dp));Column(Modifier.weight(1f)){Text(code,fontWeight=FontWeight.Black,letterSpacing=1.sp);Text(detail,color=TextMuted)};Column(horizontalAlignment=Alignment.End){Text(discount,color=if(active)Lime else TextMuted,fontWeight=FontWeight.Black);StatusBadge(if(active)"Active" else "Expired",if(active)Lime else TextMuted)}}} }

@Composable private fun ReviewsManagement(vm:AppViewModel){ val reviews=listOf(Triple("Rohan Mehta • Strike Arena",5,"Top-quality turf and bright floodlights. Great staff."),Triple("Kabir Shah • TurfX",4,"Very good pitch. Parking gets busy after 7 PM."),Triple("Anaya Rao • Urban Sports",5,"Clean courts and an excellent booking experience.")); AdminContent{items(reviews){r->Surface(color=Charcoal,border=BorderStroke(1.dp,Line),shape=RoundedCornerShape(8.dp)){Column(Modifier.padding(14.dp)){Row{Text(r.first,Modifier.weight(1f),fontWeight=FontWeight.Bold);repeat(r.second){Icon(Icons.Default.Star,null,Modifier.size(15.dp),tint=Orange)}};Text(r.third,color=TextMuted,modifier=Modifier.padding(vertical=9.dp));Row{StatusBadge("Published");Spacer(Modifier.weight(1f));TextButton({vm.showMessage("Review hidden in demo moderation.")}){Text("HIDE",color=Danger)}}}}}} }
@Composable private fun AdminSettings(logout:()->Unit){AdminContent{item{SectionTitle("Operations settings")};item{SettingRow("Booking reminders","FCM reminder 2 hours before play",true)};item{SettingRow("Automatic confirmations","Confirm verified payments",true)};item{SettingRow("Peak pricing","Apply configured peak-hour prices",true)};item{Surface(color=Orange.copy(.1f),shape=RoundedCornerShape(8.dp),border=BorderStroke(1.dp,Orange.copy(.4f))){Column(Modifier.padding(16.dp)){Text("DEMO ENVIRONMENT",color=Orange,style=MaterialTheme.typography.labelLarge);Text("Production admin access must use Firebase Authentication and backend custom claims. Never ship hardcoded credentials or payment secrets.",color=TextMuted)}}};item{OutlinedButton(logout,Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=Danger),border=BorderStroke(1.dp,Danger)){Text("LOGOUT ADMIN")}}} }
@Composable private fun SettingRow(title:String,detail:String,checked:Boolean){var value by remember{mutableStateOf(checked)};Surface(color=Charcoal,shape=RoundedCornerShape(8.dp),border=BorderStroke(1.dp,Line)){Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(title,fontWeight=FontWeight.Bold);Text(detail,color=TextMuted)};Switch(value,{value=it},colors=SwitchDefaults.colors(checkedTrackColor=Lime,checkedThumbColor=Ink))}} }
