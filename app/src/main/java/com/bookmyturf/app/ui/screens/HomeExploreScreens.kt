@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bookmyturf.app.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import kotlin.math.sin
import kotlin.math.cos
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.selectable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bookmyturf.app.Routes
import com.bookmyturf.app.R
import com.bookmyturf.app.data.Sport
import com.bookmyturf.app.data.Turf
import com.bookmyturf.app.data.UserProfile
import com.bookmyturf.app.ui.AppViewModel
import com.bookmyturf.app.ui.*
import com.bookmyturf.app.ui.components.*
import com.bookmyturf.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.Locale
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalConfiguration

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
private val homeSportOrder = listOf(
    Sport.CRICKET,
    Sport.FOOTBALL,
    Sport.BASKETBALL,
    Sport.TENNIS,
    Sport.BADMINTON,
    Sport.VOLLEYBALL
)
@Stable
class BottomBarScrollState {
    var isCompact by mutableStateOf(false)
        private set

    private var accumulatedDelta = 0f
    private val collapseThreshold = 45f
    private val expandThreshold = 25f

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(
            available: Offset,
            source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
        ): Offset {
            if (source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) {
                val delta = available.y
                if (delta < -1f) {
                    if (accumulatedDelta > 0f) accumulatedDelta = 0f
                    accumulatedDelta += -delta
                    if (accumulatedDelta >= collapseThreshold) {
                        isCompact = true
                        accumulatedDelta = 0f
                    }
                } else if (delta > 1f) {
                    if (accumulatedDelta < 0f) accumulatedDelta = 0f
                    accumulatedDelta += delta
                    if (accumulatedDelta >= expandThreshold) {
                        isCompact = false
                        accumulatedDelta = 0f
                    }
                }
            }
            return Offset.Zero
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: androidx.compose.ui.input.nestedscroll.NestedScrollSource
        ): Offset {
            if (available.y > 20f && source == androidx.compose.ui.input.nestedscroll.NestedScrollSource.UserInput) {
                isCompact = false
                accumulatedDelta = 0f
            }
            return Offset.Zero
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            if (available.y < -400f) {
                isCompact = true
            } else if (available.y > 400f) {
                isCompact = false
            }
            return Velocity.Zero
        }
    }

    fun expand() {
        isCompact = false
        accumulatedDelta = 0f
    }

    fun collapse() {
        isCompact = true
        accumulatedDelta = 0f
    }
}

@Composable
fun rememberBottomBarScrollState(): BottomBarScrollState {
    return remember { BottomBarScrollState() }
}

@Composable
fun UserBottomBar(
    current: String,
    nav: NavController,
    modifier: Modifier = Modifier,
    scrollState: BottomBarScrollState = rememberBottomBarScrollState()
) {
    var requestedRoute by remember { mutableStateOf<String?>(null) }
    val selectedRoute = requestedRoute ?: current
    val selectedIndex = userNav.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)
    val tracker = remember { Animatable(selectedIndex.toFloat()) }
    val itemMotions = remember {
        List(userNav.size) { index -> Animatable(if (index == selectedIndex) 1f else 0f) }
    }

    LaunchedEffect(selectedIndex, current) {
        val destination = requestedRoute
        coroutineScope {
            launch {
                tracker.animateTo(
                    selectedIndex.toFloat(),
                    tween(durationMillis = 240, easing = FastOutSlowInEasing)
                )
            }
            itemMotions.forEachIndexed { index, motion ->
                launch {
                    motion.animateTo(
                        if (index == selectedIndex) 1f else 0f,
                        tween(durationMillis = 180, easing = FastOutSlowInEasing)
                    )
                }
            }
        }
        if (destination != null && destination == requestedRoute) {
            if (destination != current) {
                nav.navigate(destination) {
                    popUpTo(Routes.USER) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                scrollState.expand()
            }
            requestedRoute = null
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val fullBarWidth = (screenWidth - 28.dp).coerceAtMost(360.dp)
    val compactBarWidth = 216.dp.coerceAtMost(screenWidth - 32.dp)

    val isCompact = scrollState.isCompact
    val animSpecDp = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 650f
    )
    val animSpecFloat = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 650f
    )

    val barWidth by animateDpAsState(if (isCompact) compactBarWidth else fullBarWidth, animSpecDp, label = "dockW")
    val barHeight by animateDpAsState(if (isCompact) 52.dp else 68.dp, animSpecDp, label = "dockH")
    val barCornerRadius by animateDpAsState(if (isCompact) 26.dp else 34.dp, animSpecDp, label = "dockR")
    val progress by animateFloatAsState(if (isCompact) 1f else 0f, animSpecFloat, label = "dockP")

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(86.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .width(barWidth)
                .height(barHeight)
                .shadow(
                    elevation = 14.dp,
                    shape = RoundedCornerShape(barCornerRadius),
                    spotColor = Color.Black.copy(alpha = 0.55f),
                    ambientColor = Color.Black.copy(alpha = 0.35f)
                ),
            color = Color(0xDC202228),
            shape = RoundedCornerShape(barCornerRadius),
            border = BorderStroke(1.dp, Color.White.copy(0.12f))
        ) {
            Box(Modifier.fillMaxSize()) {
                val itemWidth = barWidth / userNav.size
                val trackerWidth = itemWidth - lerp(8.dp, 6.dp, progress)
                val trackerHeight = lerp(60.dp, 44.dp, progress)
                val trackerRadius = lerp(30.dp, 22.dp, progress)
                val trackerX = lerp(4.dp, 3.dp, progress)
                val trackerY = lerp(4.dp, 4.dp, progress)

                // Active tab sliding indicator
                Box(
                    Modifier
                        .offset(x = trackerX, y = trackerY)
                        .width(trackerWidth)
                        .height(trackerHeight)
                        .graphicsLayer {
                            translationX = itemWidth.toPx() * tracker.value
                        }
                        .background(Color.White.copy(0.12f), RoundedCornerShape(trackerRadius))
                        .border(1.dp, Color.White.copy(0.12f), RoundedCornerShape(trackerRadius))
                )

                // Navigation tab items
                Row(Modifier.fillMaxSize()) {
                    userNav.forEachIndexed { index, item ->
                        val selected = item.route == selectedRoute
                        val motion = itemMotions[index]
                        val itemColor = androidx.compose.ui.graphics.lerp(TextMuted, Color.White, motion.value)
                        val tabInteraction = remember(item.route) { MutableInteractionSource() }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .selectable(
                                    selected = selected,
                                    interactionSource = tabInteraction,
                                    indication = null,
                                    role = Role.Tab,
                                    onClick = {
                                        if (!selected) {
                                            requestedRoute = item.route
                                        } else {
                                            scrollState.expand()
                                        }
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = itemColor,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        translationY = -8.5.dp.toPx() * (1f - progress)
                                        val baseScale = 1f - 0.05f * progress
                                        val selectedScale = 1f + motion.value * 0.07f
                                        scaleX = baseScale * selectedScale
                                        scaleY = baseScale * selectedScale
                                    }
                            )

                            Text(
                                text = item.label,
                                color = itemColor,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                modifier = Modifier.graphicsLayer {
                                    alpha = (1f - progress * 2.2f).coerceIn(0f, 1f)
                                    translationY = 14.dp.toPx() + 6.dp.toPx() * progress
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserTopBar(title: String? = null, nav: NavController, showBack: Boolean = false, animateBrand: Boolean = true) {
    Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showBack) IconButton(onClick = { nav.popBackStack() }, modifier = Modifier.background(Steel, CircleShape)) { Icon(Icons.Default.ChevronLeft, "Back") }
        Text(title ?: "bookmyturf", Modifier.weight(1f).padding(start = if (showBack) 12.dp else 0.dp), fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
        IconButton(onClick = { nav.navigate(Routes.FAVORITES) }) { Icon(Icons.Outlined.FavoriteBorder, "Saved turfs", tint = PulseLavender) }
    }
}

@Composable
fun UserScaffold(current: String, nav: NavController, title: String? = null, showBack: Boolean = false, content: @Composable (PaddingValues) -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Scaffold(
            topBar = { UserTopBar(title, nav, showBack, animateBrand = true) },
            containerColor = Ink,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            content = content
        )
    }
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
            Spacer(Modifier.height(16.dp)); Button(onClick = onBook, shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.buttonColors(containerColor = PulseLavender, contentColor = Ink), contentPadding = PaddingValues(horizontal = 17.dp, vertical = 12.dp)) { Text("FIND A TURF", style = MaterialTheme.typography.labelLarge); Spacer(Modifier.width(8.dp)); Icon(Icons.Default.ArrowForward, null, Modifier.size(17.dp)) }
        }
        Row(Modifier.align(Alignment.BottomEnd).padding(14.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            heroMessages.indices.forEach { index ->
                val width by animateDpAsState(if (messageIndex == index) 18.dp else 6.dp, tween(180), label = "heroIndicator")
                Box(Modifier.width(width).height(4.dp).background(if (messageIndex == index) PulseLavender else Color.White.copy(.42f), CircleShape))
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
    Surface(onClick = { nav.navigate(Routes.PROFILE) }, color = Charcoal, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, PulseLavender.copy(.2f))) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(58.dp)) {
                Image(painterResource(R.drawable.player_arjun), "Arjun Patel", Modifier.fillMaxSize().clip(CircleShape).border(2.dp, PulseLavender, CircleShape), contentScale = ContentScale.Crop)
                Box(Modifier.size(14.dp).align(Alignment.BottomEnd).background(PulseLavender, CircleShape).border(3.dp, Charcoal, CircleShape))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text(player.name.uppercase(), style = MaterialTheme.typography.titleMedium, maxLines = 1); Spacer(Modifier.width(7.dp)); Icon(Icons.Default.Verified, null, Modifier.size(15.dp), tint = PulseLavender) }
                Text("STRIKER • LEVEL 12", color = PulseLavender, style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(7.dp)); LinearProgressIndicator({ levelProgress }, Modifier.fillMaxWidth().height(5.dp), color = PulseLavender, trackColor = Line)
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
    Surface(onClick = onOpen, color = Steel, shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Line)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(PulseLavender, RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) { Icon(Icons.Default.Bolt, null, tint = Ink) }
            Spacer(Modifier.width(12.dp)); Column(Modifier.weight(1f)) { Text("NEXT GAME", color = PulseLavender, style = MaterialTheme.typography.labelMedium); Text("Strike Arena • Today, 7:00 PM", fontWeight = FontWeight.Bold); Text("Gotri • 2.1 km away", color = TextMuted, style = MaterialTheme.typography.bodyMedium) }
            Column(horizontalAlignment = Alignment.End) { Text(countdown, style = MaterialTheme.typography.titleMedium); Text("TO KICKOFF", color = TextMuted, style = MaterialTheme.typography.labelMedium) }
        }
    }
}


@Composable
private fun DistrictHomeTopBar(location: String, unread: Int, nav: NavController, onLocationClick: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "waterWaves")
    val phase1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.283185f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "w1"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = .90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        Modifier
            .fillMaxWidth()
            .height(112.dp)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val amp1a = 11.dp.toPx()
                val amp1b = 5.dp.toPx()

                val path1 = Path().apply {
                    moveTo(0f, 0f)
                    val base1 = h * .72f
                    var x = 0f
                    while (x <= w) {
                        val nx = x / w
                        val a1 = (6.283185f * nx * 1.15f) + phase1
                        val a2 = (6.283185f * nx * 2.1f) - (phase1 * .6f)
                        val y = base1 + (amp1a * sin(a1)) + (amp1b * cos(a2))
                        lineTo(x, y)
                        x += 8f
                    }
                    val e1 = (6.283185f * 1.15f) + phase1
                    val e2 = (6.283185f * 2.1f) - (phase1 * .6f)
                    val endY = base1 + (amp1a * sin(e1)) + (amp1b * cos(e2))
                    lineTo(w, endY)
                    lineTo(w, 0f)
                    close()
                }

                val ambientGlow = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to PulsePurple.copy(alpha = .44f * glowPulse),
                        .36f to PulsePurpleDark.copy(alpha = .26f * glowPulse),
                        .72f to PulsePurpleDark.copy(alpha = .07f),
                        1f to Color.Transparent
                    ),
                    center = Offset(w * .56f, -h * .08f),
                    radius = w * .62f * glowPulse
                )

                val leftGlow = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to PulseLavender.copy(alpha = .18f * glowPulse),
                        .5f to PulsePurple.copy(alpha = .05f),
                        1f to Color.Transparent
                    ),
                    center = Offset(w * .12f, h * .22f),
                    radius = w * .42f
                )

                val wave1Brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to PulsePurpleDark.copy(alpha = .48f),
                        .38f to PulsePurpleDark.copy(alpha = .28f),
                        .72f to PulsePurpleDark.copy(alpha = .07f),
                        1f to Color.Transparent
                    ),
                    startY = 0f,
                    endY = h
                )

                val bottomFade = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        .42f to Color.Transparent,
                        .70f to Ink.copy(alpha = .75f),
                        1f to Ink
                    ),
                    startY = 0f,
                    endY = h
                )

                onDrawBehind {
                    drawRect(ambientGlow)
                    drawRect(leftGlow)
                    drawPath(path1, wave1Brush)
                    drawRect(bottomFade)
                }
            }
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 18.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f).clickable(onClick = onLocationClick), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, "Choose location", tint = Color.White.copy(.95f), modifier = Modifier.size(28.dp))
                Column(Modifier.padding(start = 10.dp)) {
                    Text("PLAYING NEAR", color = PulseLavender, fontFamily = ManropeUi, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
                    Text(location, color = Color.White, fontFamily = ManropeUi, fontWeight = FontWeight.Medium, maxLines = 2)
                }
            }
            BadgedBox(badge = { if (unread > 0) Badge(containerColor = PulsePurple, contentColor = Color.White) { Text(unread.toString()) } }) {
                IconButton(onClick = { nav.navigate(Routes.NOTIFICATIONS) }) { Icon(Icons.Outlined.Notifications, "Notifications", tint = Color.White) }
            }
            IconButton(onClick = { nav.navigate(Routes.PROFILE) }) {
                Icon(Icons.Outlined.AccountCircle, "Profile", modifier = Modifier.size(31.dp), tint = Color.White)
            }
        }
    }
}

@Composable
private fun AnimatedSearchHint(selectedSport: Sport?, selectedCity: String?) {
    val hints = remember {
        listOf(
            "turfs near you",
            "sports & arenas",
            "locations & areas",
            "tournaments & offers"
        )
    }
    var hintIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(selectedSport, selectedCity) {
        if (selectedSport == null && selectedCity == null) {
            while (isActive) {
                delay(2_000)
                hintIndex = (hintIndex + 1) % hints.size
            }
        }
    }
    val text = when {
        selectedSport != null && selectedCity != null -> "${selectedSport.label} turfs in $selectedCity"
        selectedSport != null -> "${selectedSport.label} turfs near you"
        selectedCity != null -> "turfs in $selectedCity"
        else -> hints[hintIndex]
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text("Search ", color = TextMuted, fontFamily = ManropeUi, style = MaterialTheme.typography.bodyMedium)
        Box(Modifier.weight(1f).height(24.dp), contentAlignment = Alignment.CenterStart) {
            AnimatedContent(
                targetState = text,
                transitionSpec = {
                    (slideInVertically(tween(320, easing = FastOutSlowInEasing)) { it } + fadeIn(tween(220))) togetherWith
                        (slideOutVertically(tween(320, easing = FastOutSlowInEasing)) { -it } + fadeOut(tween(180)))
                },
                label = "searchHint"
            ) { hint ->
                Text(
                    hint,
                    color = TextMuted,
                    fontFamily = ManropeUi,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeTurfPosterCard(turf: Turf, onOpen: () -> Unit, onFavorite: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onOpen,
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Charcoal),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, Line)
    ) {
        Box {
            VenuePhoto(turf, Modifier.fillMaxWidth().height(198.dp))
            Surface(Modifier.align(Alignment.TopStart).padding(9.dp), color = Ink.copy(.74f), shape = RoundedCornerShape(18.dp)) {
                Text(if (turf.isIndoor) "INDOOR" else "OUTDOOR", Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = PulseLavender, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onFavorite, Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                Icon(if (turf.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder, "Favorite", tint = if (turf.isFavorite) PulseLavender else Color.White)
            }
        }
        Column(Modifier.padding(12.dp)) {
            Text(turf.name, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            Text("${turf.area} • ${turf.distanceKm} km", color = TextMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Star, null, Modifier.size(15.dp), tint = Color(0xFFFFA24C)); Text(" ${turf.rating}", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.weight(1f)); Text("₹${turf.pricePerHour}/hr", color = PulseLavender, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
fun UserHomeScreen(vm: AppViewModel, nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationPrefs = remember { context.getSharedPreferences("user-location", android.content.Context.MODE_PRIVATE) }
    val operations by vm.operations.collectAsStateWithLifecycle()
    val offer = operations.offers.firstOrNull { it.active }
    val turfs by vm.turfs.collectAsStateWithLifecycle()
    val tournaments by vm.tournaments.collectAsStateWithLifecycle()
    val tournamentSyncUnavailable by vm.tournamentSyncUnavailable.collectAsStateWithLifecycle()
    val profile by vm.profile
    val notifications by vm.notifications.collectAsStateWithLifecycle()
    var selectedSport by rememberSaveable { mutableStateOf<Sport?>(null) }
    var selectedCity by rememberSaveable { mutableStateOf(locationPrefs.getString("city", null) ?: profile.location.substringBefore(',').takeIf { it.isNotBlank() }) }
    var userPlace by remember {
        mutableStateOf(if (locationPrefs.contains("latitude")) UserPlace(
            locationPrefs.getString("city", "") ?: "",
            java.lang.Double.longBitsToDouble(locationPrefs.getLong("latitude", 0)),
            java.lang.Double.longBitsToDouble(locationPrefs.getLong("longitude", 0))
        ) else null)
    }
    val turfPlaces = remember { mutableStateMapOf<String, UserPlace>() }
    var showLocationPicker by rememberSaveable { mutableStateOf(false) }
    var requestedLocation by rememberSaveable { mutableStateOf(false) }
    fun applyPlace(place: UserPlace) {
        userPlace = place
        selectedCity = place.city
        locationPrefs.edit().putString("city", place.city)
            .putLong("latitude", java.lang.Double.doubleToRawLongBits(place.latitude))
            .putLong("longitude", java.lang.Double.doubleToRawLongBits(place.longitude)).apply()
        val label = "${place.city}, India"
        if (profile.location != label) vm.saveProfile(profile.copy(location = label))
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) scope.launch { detectUserPlace(context)?.let(::applyPlace) ?: run { showLocationPicker = true } }
        else showLocationPicker = true
    }
    LaunchedEffect(Unit) {
        if (userPlace != null) return@LaunchedEffect
        if (hasLocationPermission(context)) detectUserPlace(context)?.let(::applyPlace)
        else if (!requestedLocation) { requestedLocation = true; locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION) }
    }
    LaunchedEffect(turfs) {
        turfs.map { it.city }.filter { it.isNotBlank() }.distinct().forEach { city ->
            if (turfPlaces[city.lowercase()] == null) geocodeCity(context, city)?.let { turfPlaces[city.lowercase()] = it }
        }
    }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showOffer by rememberSaveable { mutableStateOf(true) }
    val listState = rememberLazyListState()
    val filteredTurfs = remember(turfs, selectedSport, selectedCity, userPlace, turfPlaces.toMap()) {
        turfs.mapNotNull { turf ->
            if (!turf.active || (selectedSport != null && selectedSport !in turf.sports)) return@mapNotNull null
            val calculated = userPlace?.let { origin -> turfPlaces[turf.city.lowercase()]?.let { distanceKm(origin, it) } }
            val isNearby = calculated?.let { it <= 50.0 } ?: turf.city.equals(selectedCity, ignoreCase = true)
            if (isNearby) turf.copy(distanceKm = calculated ?: turf.distanceKm) else null
        }.sortedBy { it.distanceKm }
    }
    val locationLabel = selectedCity?.let { "$it, India" } ?: "Choose location"
    val recommendedTournaments = tournaments.filter { tournament ->
        tournament.active && (selectedSport == null || tournament.sport == selectedSport) &&
            (selectedCity == null || tournament.city.equals(selectedCity, ignoreCase = true))
    }.take(2)

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            item(key = "header") {
                Column {
                    DistrictHomeTopBar(locationLabel, notifications.count { it.unread }, nav) { showLocationPicker = true }
                    Box(Modifier.fillMaxWidth().height(18.dp), contentAlignment = Alignment.BottomCenter) {
                        Image(
                            painter = painterResource(R.drawable.bookmyturf_wordmark),
                            contentDescription = "BookMyTurf",
                            modifier = Modifier.width(136.dp).aspectRatio(2095f / 472f).offset(y = (-3).dp),
                            contentScale = ContentScale.Fit,
                            colorFilter = ColorFilter.tint(Color.White)
                        )
                    }
                }
            }
            item(key = "search") {
                Surface(
                    onClick = { showSearch = true },
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Steel,
                    shape = RoundedCornerShape(17.dp),
                    border = BorderStroke(1.dp, Line)
                ) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Search, null, tint = Color.White.copy(.8f))
                        Spacer(Modifier.width(12.dp))
                        Box(Modifier.weight(1f).height(24.dp), contentAlignment = Alignment.CenterStart) {
                            AnimatedSearchHint(selectedSport, selectedCity)
                        }
                        Icon(Icons.Default.Tune, "Search filters", tint = PulseLavender)
                    }
                }
            }
            item(key = "spotlight") {
                val visible by remember { derivedStateOf { listState.layoutInfo.visibleItemsInfo.any { it.key == "spotlight" } } }
                HomeHighlights(onSport = { selectedSport = it }, isVisible = visible)
            }
            item(key = "explore-title") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Explore", fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineMedium)
                        Text("Pick a sport and find your arena", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    if (selectedSport != null) TextButton(onClick = { selectedSport = null }) { Text("SHOW ALL", color = PulseLavender) }
                }
            }
            item(key = "sports") {
                LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(homeSportOrder, key = { it.name }) { sport ->
                        HomeSportTile(sport, selectedSport == sport) {
                            selectedSport = if (selectedSport == sport) null else sport
                        }
                    }
                }
            }
            if (showOffer && offer != null) item(key = "offer") {
                Surface(
                    onClick = { vm.showMessage("${offer.code} · ${offer.detail}") },
                    color = Steel,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color.White.copy(.07f))
                ) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).background(PulsePurple.copy(.28f), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.LocalOffer, null, tint = PulseLavender)
                        }
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                            Text(offer.detail, fontWeight = FontWeight.ExtraBold)
                            Text("Offer code: ${offer.code}", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { showOffer = false }) { Icon(Icons.Default.Close, "Dismiss offer", tint = TextMuted) }
                    }
                }
            }
            item(key = "turf-title") {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        selectedSport?.let { "${it.label} picks for you" } ?: "Curated for you",
                        fontFamily = ManropeUi,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text("${filteredTurfs.size} places • Nearest first", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            if (filteredTurfs.isEmpty()) {
                item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.SearchOff, null, Modifier.size(42.dp), tint = PulseLavender)
                        Spacer(Modifier.height(10.dp)); Text("NO TURFS FOUND", style = MaterialTheme.typography.titleLarge)
                        Text("Try another location or sport.", color = TextMuted)
                        TextButton(onClick = { selectedCity = null; selectedSport = null }) { Text("CLEAR FILTERS", color = PulseLavender) }
                    }
                }
            } else {
                items(filteredTurfs.chunked(2), key = { row -> row.joinToString("-") { it.id } }) { row ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        row.forEach { turf ->
                            HomeTurfPosterCard(turf, { nav.navigate(Routes.turf(turf.id)) }, { vm.toggleFavorite(turf.id) }, Modifier.weight(1f))
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            item(key = "tournament-title") {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Tournaments for you", fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
                        Text("Bring your team. Play for the prize.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { vm.exploreContent.value = "Tournaments"; nav.navigate(Routes.EXPLORE) }) { Text("SEE ALL", color = PulseLavender) }
                }
            }
            if (recommendedTournaments.isEmpty()) item(key = "no-tournaments") {
                Text(if (tournamentSyncUnavailable) "Tournament listings are temporarily unavailable." else "No upcoming tournaments in this area yet.", Modifier.padding(horizontal = 16.dp), color = TextMuted)
            }
            items(recommendedTournaments.chunked(2), key = { row -> "tournaments-${row.joinToString("-") { it.id }}" }) { row ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { tournament ->
                        HomeTournamentPosterCard(tournament, { nav.navigate(Routes.tournament(tournament.id)) }, Modifier.weight(1f))
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
    if (showSearch) HomeSearchSheet(
        turfs = turfs,
        selectedCity = selectedCity,
        selectedSport = selectedSport,
        onCityChange = { selectedCity = it },
        onSportChange = { selectedSport = it },
        onTurfOpen = { turf -> showSearch = false; nav.navigate(Routes.turf(turf.id)) },
        onDismiss = { showSearch = false }
    )
    if (showLocationPicker) LocationPickerSheet(
        currentCity = selectedCity,
        onDismiss = { showLocationPicker = false },
        onCurrentLocation = {
            if (hasLocationPermission(context)) scope.launch {
                detectUserPlace(context)?.let { applyPlace(it); showLocationPicker = false }
                    ?: vm.showMessage("Could not detect your location. Choose a city instead.")
            } else locationPermission.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
        },
        onCity = { city ->
            scope.launch {
                geocodeCity(context, city)?.let { applyPlace(it); showLocationPicker = false }
                    ?: vm.showMessage("City not found. Check the spelling and try again.")
            }
        }
    )
}

@Composable
private fun LocationPickerSheet(
    currentCity: String?,
    onDismiss: () -> Unit,
    onCurrentLocation: () -> Unit,
    onCity: (String) -> Unit
) {
    var search by rememberSaveable { mutableStateOf("") }
    val matches = remember(search) {
        indianCities.filter { search.isBlank() || it.contains(search.trim(), ignoreCase = true) }.take(40)
    }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Charcoal) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(18.dp)) {
            Text("Choose your location", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("Only turfs within 50 km will be shown.", color = TextMuted)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onCurrentLocation, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.MyLocation, null); Spacer(Modifier.width(8.dp)); Text("USE CURRENT LOCATION")
            }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(search, { search = it }, Modifier.fillMaxWidth(), singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, null) }, label = { Text("Search Indian city") })
            if (search.trim().length >= 2 && matches.none { it.equals(search.trim(), true) }) {
                TextButton(onClick = { onCity(search.trim()) }, Modifier.fillMaxWidth()) {
                    Text("Use “${search.trim()}”", color = PulseLavender)
                }
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                items(matches, key = { it }) { city ->
                    ListItem(
                        headlineContent = { Text(city) },
                        leadingContent = { Icon(Icons.Default.LocationCity, null, tint = PulseLavender) },
                        trailingContent = { if (city.equals(currentCity, true)) Icon(Icons.Default.Check, null, tint = PulseLavender) },
                        modifier = Modifier.clickable { onCity(city) },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}
