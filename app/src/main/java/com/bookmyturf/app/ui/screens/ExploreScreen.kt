@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bookmyturf.app.ui.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import com.bookmyturf.app.R
import com.bookmyturf.app.Routes
import com.bookmyturf.app.data.Sport
import com.bookmyturf.app.data.Turf
import com.bookmyturf.app.ui.AppViewModel
import com.bookmyturf.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun ExploreScreen(vm: AppViewModel, nav: NavController) {
    val turfs by vm.turfs.collectAsStateWithLifecycle()
    val profile by vm.profile
    var query by rememberSaveable { mutableStateOf("") }
    var sport by rememberSaveable { mutableStateOf<Sport?>(null) }
    var venue by rememberSaveable { mutableStateOf("All venues") }
    var sort by rememberSaveable { mutableStateOf("Nearest") }
    var showFilters by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current
    // UserBottomBar occupies 86.dp above the system navigation inset.
    val bottomObstruction = with(density) { 86.dp.roundToPx() } + WindowInsets.navigationBars.getBottom(density)
    val filtered = remember(turfs, query, sport, venue, sort) {
        val search = query.trim()
        turfs.filter { t ->
            t.active && (search.isBlank() || listOf(t.name, t.city, t.area).any { it.contains(search, true) } ||
                t.sports.any { it.label.contains(search, true) }) &&
                (sport == null || sport in t.sports) &&
                (venue == "All venues" || (venue == "Indoor") == t.isIndoor)
        }.let { results ->
            when (sort) {
                "Price: low to high" -> results.sortedBy { it.pricePerHour }
                "Top rated" -> results.sortedByDescending { it.rating }
                else -> results.sortedBy { it.distanceKm }
            }
        }
    }
    val resetFilters = { query = ""; sport = null; venue = "All venues"; sort = "Nearest" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Ink).statusBarsPadding(),
        state = listState,
        contentPadding = PaddingValues(bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "explore-header") {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocationOn, null, Modifier.size(15.dp), tint = PulseLavender)
                        Text(profile.location, Modifier.padding(start = 5.dp), color = TextMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Find your next game", fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
                    Text("Great spaces. Better games.", Modifier.padding(top = 5.dp), color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(onClick = { nav.navigate(Routes.FAVORITES) }, modifier = Modifier.background(Steel, CircleShape)) {
                    Icon(Icons.Outlined.FavoriteBorder, "Saved turfs", tint = PulseLavender)
                }
            }
        }
        item(key = "explore-search") {
            OutlinedTextField(
                value = query, onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                placeholder = { Text("Search turf, area or sport") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = TextMuted) },
                trailingIcon = {
                    if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Default.Close, "Clear search") }
                },
                shape = RoundedCornerShape(18.dp), singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PulseLavender, unfocusedBorderColor = Line,
                    focusedContainerColor = Steel, unfocusedContainerColor = Steel, cursorColor = PulseLavender
                )
            )
        }
        item(key = "explore-sports") {
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { ExploreChip("All sports", sport == null) { sport = null } }
                items(Sport.entries, key = { it.name }) { option ->
                    ExploreChip(option.label, sport == option) { sport = if (sport == option) null else option }
                }
            }
        }
        item(key = "explore-results") {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(sport?.let { "${it.label} picks" } ?: "Curated for you", fontFamily = ManropeUi, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text("${filtered.size} places · $sort", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    OutlinedButton(onClick = { showFilters = true }, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Line), colors = ButtonDefaults.outlinedButtonColors(contentColor = PulseLavender)) {
                        Icon(Icons.Default.Tune, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Filters" + if (venue != "All venues" || sort != "Nearest") " •" else "")
                    }
                }
                if (venue != "All venues") {
                    Spacer(Modifier.height(10.dp))
                    ExploreChip("$venue ×", true) { venue = "All venues" }
                }
            }
        }
        if (filtered.isEmpty()) item(key = "explore-empty") {
            Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.SearchOff, null, Modifier.size(44.dp), tint = PulseLavender)
                Text("No matching turfs", fontFamily = ManropeUi, fontWeight = FontWeight.Bold)
                Text("Try a different area, sport or venue type.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = resetFilters) { Text("Reset search & filters", color = PulseLavender) }
            }
        }
        items(filtered, key = { it.id }) { turf ->
            val fullyVisible by remember(listState, turf.id, bottomObstruction) {
                derivedStateOf {
                    val layout = listState.layoutInfo
                    val visibleBottom = layout.viewportEndOffset - bottomObstruction
                    !listState.isScrollInProgress && layout.visibleItemsInfo.any { item ->
                        item.key == turf.id && item.offset >= layout.viewportStartOffset &&
                            item.offset + item.size <= visibleBottom
                    }
                }
            }
            ExploreTurfCard(turf, fullyVisible && !showFilters, { nav.navigate(Routes.turf(turf.id)) }, { vm.toggleFavorite(turf.id) })
        }
    }
    if (showFilters) ModalBottomSheet(onDismissRequest = { showFilters = false }, containerColor = Charcoal) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(20.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Make it your game", fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
            Text("Venue type", color = TextMuted)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All venues", "Indoor", "Outdoor").forEach { option -> ExploreChip(option, venue == option) { venue = option } }
            }
            Text("Sort by", color = TextMuted)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Nearest", "Price: low to high", "Top rated").forEach { option -> ExploreChip(option, sort == option) { sort = option } }
            }
            Button(onClick = { showFilters = false }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = PulsePurple, contentColor = Color.White)) {
                Text("Show ${filtered.size} places")
            }
        }
    }
}

@Composable
private fun ExploreChip(label: String, selected: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = selected, onClick = onClick, label = { Text(label) }, shape = RoundedCornerShape(50),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Steel, labelColor = TextMuted,
            selectedContainerColor = PulsePurple.copy(.25f), selectedLabelColor = PulseLavender
        ),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected, borderColor = Line, selectedBorderColor = PulseLavender.copy(.5f))
    )
}

@Composable
private fun ExploreTurfCard(turf: Turf, isVisible: Boolean, onOpen: () -> Unit, onFavorite: () -> Unit) {
    val fallbackPhoto = when (turf.visualSeed.mod(6)) {
        1, 4 -> R.drawable.venue_cricket
        5 -> R.drawable.venue_badminton
        else -> R.drawable.venue_football
    }
    // Supply four photoResIds on Turf to replace the bundled placeholder views.
    val photos = remember(turf.photoResIds, fallbackPhoto) {
        List(4) { index -> turf.photoResIds.getOrNull(index) ?: fallbackPhoto }
    }
    val pager = rememberPagerState(initialPage = 400, pageCount = { 800 })
    val dragging by pager.interactionSource.collectIsDraggedAsState()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle, isVisible, dragging) {
        if (isVisible && !dragging) lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (isActive) {
                delay(2_000)
                if (!pager.isScrollInProgress) {
                    if (pager.currentPage == pager.pageCount - 1) pager.scrollToPage(399)
                    pager.animateScrollToPage(pager.currentPage + 1, animationSpec = tween(400))
                }
            }
        }
    }
    Card(
        onClick = onOpen, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Charcoal),
        border = BorderStroke(1.dp, Line)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.85f)) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                val index = page.mod(4)
                Image(
                    painterResource(photos[index]), "${turf.name}, photo ${index + 1} of 4",
                    Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                    alignment = when (index) { 1 -> Alignment.CenterStart; 2 -> Alignment.CenterEnd; 3 -> Alignment.BottomCenter; else -> Alignment.Center }
                )
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink.copy(.2f), Color.Transparent, Ink.copy(.75f)))))
            Surface(Modifier.align(Alignment.TopStart).padding(12.dp), color = Ink.copy(.8f), shape = RoundedCornerShape(10.dp)) {
                Text(if (turf.isIndoor) "Indoor arena" else "Outdoor arena", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            IconButton(onClick = onFavorite, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).background(Ink.copy(.65f), CircleShape)) {
                Icon(if (turf.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                    if (turf.isFavorite) "Remove ${turf.name} from favorites" else "Save ${turf.name}", tint = if (turf.isFavorite) PulseLavender else Color.White)
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 13.dp).semantics { contentDescription = "Photo ${pager.currentPage.mod(4) + 1} of 4" }, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(4) { index ->
                    val selected = pager.currentPage.mod(4) == index
                    val width by animateDpAsState(if (selected) 20.dp else 6.dp, label = "photo indicator")
                    Box(Modifier.width(width).height(6.dp).background(if (selected) PulseLavender else Color.White.copy(.65f), CircleShape))
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(turf.name, Modifier.weight(1f), fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.width(10.dp))
                Icon(Icons.Default.Star, null, Modifier.size(16.dp), tint = Orange)
                Text(" ${turf.rating}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
            }
            Text("${turf.area}, ${turf.city} · ${turf.distanceKm} km", color = TextMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(turf.sports.joinToString(" · ") { it.label }, color = PulseLavender, style = MaterialTheme.typography.labelSmall)
            HorizontalDivider(Modifier.padding(top = 5.dp), color = Line)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("₹${turf.pricePerHour}", fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    Text("per hour", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = onOpen, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = PulsePurple.copy(.25f), contentColor = PulseLavender)) {
                    Text("View slots")
                }
            }
        }
    }
}
