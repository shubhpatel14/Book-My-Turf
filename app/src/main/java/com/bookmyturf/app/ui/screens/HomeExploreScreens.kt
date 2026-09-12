@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bookmyturf.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bookmyturf.app.Routes
import com.bookmyturf.app.R
import com.bookmyturf.app.data.Sport
import com.bookmyturf.app.data.UserProfile
import com.bookmyturf.app.ui.AppViewModel
import com.bookmyturf.app.ui.components.*
import com.bookmyturf.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale

private val heroHorizontalOverlay = Brush.horizontalGradient(
    listOf(Ink.copy(.97f), Ink.copy(.7f), Ink.copy(.08f), Color.Transparent)
)
private val heroVerticalOverlay = Brush.verticalGradient(
    listOf(Color.Transparent, Ink.copy(.1f), Ink.copy(.5f))
)

private data class HeroMessage(val badge: String, val title: String, val subtitle: String)
private val heroMessages = listOf(
    HeroMessage("Tonight is yours", "OWN THE\nNEXT GAME.", "Premium arenas. Zero hassle."),
    HeroMessage("Prime time live", "PLAY UNDER\nTHE LIGHTS.", "Top-rated evening slots near you."),
    HeroMessage("Built for teams", "YOUR CITY.\nYOUR TURF.", "Book together. Compete harder.")
)

private data class UserNav(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val route: String)
private val userNav = listOf(
    UserNav("Home", Icons.Default.Home, Routes.USER), UserNav("Explore", Icons.Default.Search, Routes.EXPLORE),
    UserNav("Bookings", Icons.Default.ConfirmationNumber, Routes.BOOKINGS), UserNav("Profile", Icons.Default.Person, Routes.PROFILE)
)

@Composable
fun UserBottomBar(current: String, nav: NavController) {
    NavigationBar(containerColor = Charcoal, tonalElevation = 0.dp) {
        userNav.forEach { item ->
            NavigationBarItem(
                selected = current == item.route, onClick = { if (current != item.route) nav.navigate(item.route) { launchSingleTop = true } },
                icon = { Icon(item.icon, item.label) }, label = { Text(item.label.uppercase(), style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = Ink, selectedTextColor = Lime, indicatorColor = Lime, unselectedIconColor = TextMuted, unselectedTextColor = TextMuted)
            )
        }
    }
}

@Composable
fun UserTopBar(title: String? = null, nav: NavController, showBack: Boolean = false, animateBrand: Boolean = true) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().height(72.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showBack) IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") } else BrandMark(compact = true, animatedCrest = animateBrand)
        if (title != null) Text(title.uppercase(), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(start = if (showBack) 8.dp else 16.dp)) else Spacer(Modifier.weight(1f))
        IconButton(onClick = { nav.navigate(Routes.FAVORITES) }) { Icon(Icons.Outlined.FavoriteBorder, "Favorites") }
        BadgedBox(badge = { Badge(containerColor = Lime, contentColor = Ink) { Text("2") } }) {
            IconButton(onClick = { nav.navigate(Routes.NOTIFICATIONS) }) { Icon(Icons.Outlined.Notifications, "Notifications") }
        }
    }
}

@Composable
fun UserScaffold(current: String, nav: NavController, title: String? = null, showBack: Boolean = false, content: @Composable (PaddingValues) -> Unit) {
    Scaffold(topBar = { UserTopBar(title, nav, showBack, animateBrand = true) }, bottomBar = { if (!showBack) UserBottomBar(current, nav) }, containerColor = Ink, content = content)
}

@Composable
fun HomeHero(onBook: () -> Unit) {
    var messageIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (isActive) {
            delay(4_200)
            messageIndex = (messageIndex + 1) % heroMessages.size
        }
    }
    Box(Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, Color.White.copy(.1f), RoundedCornerShape(14.dp))) {
        Image(painterResource(R.drawable.hero_football), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(heroHorizontalOverlay))
        Box(Modifier.fillMaxSize().background(heroVerticalOverlay))
        Column(Modifier.align(Alignment.CenterStart).padding(22.dp).width(215.dp)) {
            AnimatedContent(
                targetState = heroMessages[messageIndex],
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 6 }) togetherWith fadeOut(tween(150))
                },
                contentKey = { it.title },
                label = "heroCopy"
            ) { message ->
                Column(Modifier.height(124.dp)) {
                    StatusBadge(message.badge)
                    Spacer(Modifier.height(12.dp))
                    Text(message.title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black)
                    Spacer(Modifier.height(7.dp))
                    Text(message.subtitle, color = Color.White.copy(.75f), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                }
            }
            Spacer(Modifier.height(16.dp)); Button(onClick = onBook, shape = RoundedCornerShape(7.dp), colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink), contentPadding = PaddingValues(horizontal = 17.dp, vertical = 12.dp)) { Text("FIND A TURF", style = MaterialTheme.typography.labelLarge); Spacer(Modifier.width(8.dp)); Icon(Icons.Default.ArrowForward, null, Modifier.size(17.dp)) }
        }
        Row(Modifier.align(Alignment.BottomEnd).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            heroMessages.indices.forEach { index ->
                val width by animateDpAsState(if (messageIndex == index) 18.dp else 6.dp, tween(180), label = "heroIndicator")
                Box(Modifier.width(width).height(4.dp).background(if (messageIndex == index) Lime else Color.White.copy(.42f), CircleShape))
            }
        }
    }
}

@Composable
private fun PlayerGameProfile(nav: NavController, player: UserProfile, games: Int) {
    var started by remember { mutableStateOf(false) }
    val levelProgress by animateFloatAsState(if (started) .72f else 0f, tween(720, easing = FastOutSlowInEasing), label = "playerLevel")
    val gameCount by animateIntAsState(if (started) games else 0, tween(650, easing = FastOutSlowInEasing), label = "gameCount")
    LaunchedEffect(Unit) { started = true }
    Surface(onClick = { nav.navigate(Routes.PROFILE) }, color = Charcoal, shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Lime.copy(.2f))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp)) {
                Image(painterResource(R.drawable.player_arjun), "Arjun Patel", Modifier.fillMaxSize().clip(CircleShape).border(2.dp, Lime, CircleShape), contentScale = ContentScale.Crop)
                Box(Modifier.size(14.dp).align(Alignment.BottomEnd).background(Lime, CircleShape).border(3.dp, Charcoal, CircleShape))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text(player.name.uppercase(), style = MaterialTheme.typography.titleMedium, maxLines = 1); Spacer(Modifier.width(7.dp)); Icon(Icons.Default.Verified, null, Modifier.size(15.dp), tint = Lime) }
                Text("STRIKER • LEVEL 12", color = Lime, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(7.dp)); LinearProgressIndicator({ levelProgress }, Modifier.fillMaxWidth().height(5.dp), color = Lime, trackColor = Line)
            }
            Spacer(Modifier.width(12.dp)); Column(horizontalAlignment = Alignment.End) { Text(gameCount.toString().padStart(2, '0'), style = MaterialTheme.typography.headlineMedium); Text("GAMES", color = TextMuted, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun NextGameCard(onOpen: () -> Unit) {
    var remainingSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(Unit) {
        while (isActive) {
            val now = LocalDateTime.now()
            var kickoff = now.withHour(19).withMinute(0).withSecond(0).withNano(0)
            if (!kickoff.isAfter(now)) kickoff = kickoff.plusDays(1)
            remainingSeconds = Duration.between(now, kickoff).seconds.coerceAtLeast(0)
            delay(1_000)
        }
    }
    val countdown = String.format(Locale.US, "%02d:%02d:%02d", remainingSeconds / 3600, (remainingSeconds % 3600) / 60, remainingSeconds % 60)
    Surface(onClick = onOpen, color = Steel, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Line)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(Lime, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = Ink) }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("NEXT GAME", color = Lime, style = MaterialTheme.typography.labelMedium); Text("Strike Arena • Today, 7:00 PM", fontWeight = FontWeight.Bold); Text("Gotri • 2.1 km away", color = TextMuted, style = MaterialTheme.typography.bodyMedium) }
            Column(horizontalAlignment = Alignment.End) { Text(countdown, style = MaterialTheme.typography.titleMedium); Text("TO KICKOFF", color = TextMuted, style = MaterialTheme.typography.labelMedium) }
        }
    }
}

@Composable
private fun SportTile(sport: Sport, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressScale by animateFloatAsState(if (pressed) .94f else 1f, tween(110), label = "sportPress")
    Surface(
        modifier = Modifier.width(92.dp).scale(pressScale).clickable(interactionSource = interaction, indication = LocalIndication.current, onClick = onClick),
        color = if (pressed) Lime.copy(.1f) else Steel,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (pressed) Lime else Line)
    ) {
        Column(Modifier.padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            SportIcon(sport, Modifier.size(26.dp))
            Spacer(Modifier.height(8.dp))
            Text(sport.label.uppercase(), style = MaterialTheme.typography.labelMedium, color = if (pressed) Lime else LocalContentColor.current)
        }
    }
}

@Composable
fun UserHomeScreen(vm: AppViewModel, nav: NavController) {
    val turfs by vm.turfs.collectAsStateWithLifecycle()
    val bookings by vm.bookings.collectAsStateWithLifecycle()
    val profile by vm.profile
    val greeting by produceState(initialValue = "") {
        while (true) {
            value = when (LocalTime.now().hour) {
                in 5..11 -> "GOOD MORNING,"
                in 12..16 -> "GOOD AFTERNOON,"
                else -> "GOOD EVENING,"
            }
            delay(60_000)
        }
    }
    val firstName = profile.name.substringBefore(' ').uppercase()
    UserScaffold(Routes.USER, nav) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item { StaggeredReveal(0) { Column { Spacer(Modifier.height(4.dp)); Text(greeting, color = TextMuted, style = MaterialTheme.typography.labelLarge); Text("READY, $firstName?", style = MaterialTheme.typography.headlineLarge); Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.LocationOn, null, Modifier.size(16.dp), tint = Lime); Text(" ${profile.location}", color = TextMuted) } } } }
            item { StaggeredReveal(1) { PlayerGameProfile(nav, profile, (bookings.size + 5).coerceAtLeast(8)) } }
            item {
                StaggeredReveal(2) { Surface(onClick = { nav.navigate(Routes.EXPLORE) }, color = Charcoal, shape = RoundedCornerShape(9.dp), border = BorderStroke(1.dp, Line)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Default.Search, null, tint = TextMuted); Spacer(Modifier.width(12.dp)); Text("Search turfs, sports or locations", color = TextMuted); Spacer(Modifier.weight(1f)); Icon(Icons.Default.Tune, null, tint = Lime) }
                } }
            }
            item { StaggeredReveal(3) { HomeHero { nav.navigate(Routes.EXPLORE) } } }
            item { StaggeredReveal(4) { NextGameCard { nav.navigate(Routes.BOOKINGS) } } }
            item { SectionTitle("Choose your sport") }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(Sport.entries) { sport ->
                        SportTile(sport) { nav.navigate(Routes.EXPLORE) }
                    }
                }
            }
            item { SectionTitle("Nearby turfs", "See all") { nav.navigate(Routes.EXPLORE) } }
            itemsIndexed(turfs.take(3), key = { _, item -> item.id }) { index, turf ->
                StaggeredReveal(index + 5) { TurfCard(turf, { nav.navigate(Routes.turf(turf.id)) }, { vm.toggleFavorite(turf.id) }) }
            }
        }
    }
}

@Composable
fun ExploreScreen(vm: AppViewModel, nav: NavController) {
    val turfs by vm.turfs.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }; var sport by remember { mutableStateOf<Sport?>(null) }; var indoor by remember { mutableStateOf(false) }; var sort by remember { mutableStateOf("Nearest") }; var showFilters by remember { mutableStateOf(false) }
    val filtered = remember(turfs, query, sport, indoor, sort) {
        turfs.filter { t -> (query.isBlank() || t.name.contains(query, true) || t.city.contains(query, true) || t.area.contains(query, true) || t.sports.any { it.label.contains(query, true) }) && (sport == null || sport in t.sports) && (!indoor || t.isIndoor) }
            .let { list -> when (sort) { "Cheapest" -> list.sortedBy { it.pricePerHour }; "Highest Rated" -> list.sortedByDescending { it.rating }; else -> list.sortedBy { it.distanceKm } } }
    }
    UserScaffold(Routes.EXPLORE, nav, "Explore") { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp), placeholder = { Text("Turf, location or sport") }, leadingIcon = { Icon(Icons.Default.Search, null) }, trailingIcon = { IconButton(onClick = { showFilters = true }) { Icon(Icons.Default.Tune, "Filters", tint = Lime) } }, shape = RoundedCornerShape(8.dp), singleLine = true)
            LazyRow(Modifier.padding(vertical = 12.dp), contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { Tag("All", sport == null) { sport = null } }; items(Sport.entries) { s -> Tag(s.label, sport == s) { sport = if (sport == s) null else s } }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) { Text("${filtered.size} ARENAS", color = TextMuted, style = MaterialTheme.typography.labelMedium); Spacer(Modifier.weight(1f)); Text("$sort ", color = Lime, style = MaterialTheme.typography.labelMedium); Icon(Icons.Default.SwapVert, null, tint = Lime, modifier = Modifier.size(16.dp).clickable { sort = when (sort) { "Nearest" -> "Cheapest"; "Cheapest" -> "Highest Rated"; else -> "Nearest" } }) }
            if (filtered.isEmpty()) {
                Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Icon(Icons.Default.SearchOff, null, Modifier.size(54.dp), tint = TextMuted); Spacer(Modifier.height(16.dp)); Text("NO TURFS FOUND", style = MaterialTheme.typography.headlineMedium); Text("Try changing your search or filters.", color = TextMuted); Spacer(Modifier.height(18.dp)); OutlinedButton(onClick = { query = ""; sport = null; indoor = false }) { Text("CLEAR FILTERS") } }
            } else LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                items(filtered, key = { it.id }) { turf -> TurfCard(turf, { nav.navigate(Routes.turf(turf.id)) }, { vm.toggleFavorite(turf.id) }) }
            }
        }
    }
    if (showFilters) ModalBottomSheet(onDismissRequest = { showFilters = false }, containerColor = Charcoal) {
        Column(Modifier.padding(20.dp).navigationBarsPadding()) {
            SectionTitle("Filters"); Text("VENUE TYPE", color = TextMuted, style = MaterialTheme.typography.labelMedium); Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Tag("Indoor", indoor) { indoor = !indoor }; Tag("Outdoor", !indoor) { indoor = false } }
            Spacer(Modifier.height(20.dp)); Text("SORT BY", color = TextMuted, style = MaterialTheme.typography.labelMedium); Spacer(Modifier.height(10.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Nearest", "Cheapest", "Highest Rated").forEach { Tag(it, sort == it) { sort = it } } }
            Spacer(Modifier.height(24.dp)); PrimaryButton("Show ${filtered.size} Turfs", { showFilters = false }); Spacer(Modifier.height(14.dp))
        }
    }
}
