package com.bookmyturf.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bookmyturf.app.R
import com.bookmyturf.app.data.Sport
import com.bookmyturf.app.data.Tournament
import com.bookmyturf.app.data.DemoPaymentMode
import com.bookmyturf.app.data.validateTournamentRegistration
import com.bookmyturf.app.ui.AppViewModel
import com.bookmyturf.app.ui.components.formatInr
import com.bookmyturf.app.ui.components.TurfImagePresets
import com.bookmyturf.app.ui.theme.*
import coil.compose.SubcomposeAsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private fun tournamentPhoto(sport: Sport): Int = when (sport) {
    Sport.CRICKET -> R.drawable.venue_cricket
    Sport.BADMINTON, Sport.TENNIS, Sport.VOLLEYBALL -> R.drawable.venue_badminton
    else -> R.drawable.venue_football
}

internal fun tournamentDate(value: String): String = runCatching {
    LocalDate.parse(value).format(DateTimeFormatter.ofPattern("d MMM yyyy"))
}.getOrDefault(value)

@Composable
fun TournamentCard(tournament: Tournament, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val defaultUrls = remember(tournament.sport) {
        val first = when (tournament.sport) {
            Sport.CRICKET -> TurfImagePresets.CRICKET
            Sport.BADMINTON, Sport.TENNIS, Sport.VOLLEYBALL -> TurfImagePresets.BADMINTON
            else -> TurfImagePresets.FOOTBALL
        }
        (listOf(first) + TurfImagePresets.items.map { it.second }).distinct().take(4)
    }
    val photos = remember(tournament.imageUrls, defaultUrls) {
        (tournament.imageUrls + defaultUrls).distinct().take(4)
    }
    val pager = rememberPagerState(pageCount = { photos.size })
    val dragging by pager.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(pager, dragging, photos) {
        if (!dragging && photos.size > 1) while (isActive) {
            delay(3_500)
            if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.currentPage + 1) % photos.size, animationSpec = tween(450))
        }
    }
    Card(
        onClick = onOpen, modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Charcoal),
        border = BorderStroke(1.dp, Line)
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.85f)) {
            HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                SubcomposeAsyncImage(
                    model = photos[page], contentDescription = "${tournament.name} photo ${page + 1} of ${photos.size}",
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                    loading = { Image(painterResource(tournamentPhoto(tournament.sport)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) },
                    error = { Image(painterResource(tournamentPhoto(tournament.sport)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                )
            }
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink.copy(.2f), Color.Transparent, Ink.copy(.75f)))))
            Surface(Modifier.align(Alignment.TopStart).padding(12.dp), color = Ink.copy(.82f), shape = RoundedCornerShape(10.dp)) {
                Text(tournament.sport.label.uppercase(), Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = PulseLavender, style = MaterialTheme.typography.labelSmall)
            }
            Surface(Modifier.align(Alignment.TopEnd).padding(12.dp), color = PulsePurple.copy(.86f), shape = RoundedCornerShape(10.dp)) {
                Text("TOURNAMENT", Modifier.padding(horizontal = 10.dp, vertical = 7.dp), color = Color.White, style = MaterialTheme.typography.labelSmall)
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 13.dp), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(photos.size) { index ->
                    Box(Modifier.width(if (pager.currentPage == index) 20.dp else 6.dp).height(6.dp).background(if (pager.currentPage == index) PulseLavender else Color.White.copy(.6f), CircleShape))
                }
            }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(tournament.name, fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${tournament.venue}, ${tournament.city} · ${tournamentDate(tournament.date)}", color = TextMuted, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            Text("${tournament.teamSize} players / team · Up to ${tournament.maxTeams} teams", color = PulseLavender, style = MaterialTheme.typography.labelSmall)
            HorizontalDivider(Modifier.padding(top = 5.dp), color = Line)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(formatInr(tournament.prizePool.toDouble()), fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    Text("Prize pool", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(formatInr(tournament.entryFee.toDouble()), fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
                    Text("Entry / team · demo fee", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            }
            Text("View details & register →", color = PulseLavender, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun HomeTournamentPosterCard(tournament: Tournament, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val defaultUrls = remember(tournament.sport) {
        val first = when (tournament.sport) {
            Sport.CRICKET -> TurfImagePresets.CRICKET
            Sport.BADMINTON, Sport.TENNIS, Sport.VOLLEYBALL -> TurfImagePresets.BADMINTON
            else -> TurfImagePresets.FOOTBALL
        }
        (listOf(first) + TurfImagePresets.items.map { it.second }).distinct().take(4)
    }
    val photos = remember(tournament.imageUrls, defaultUrls) { (tournament.imageUrls + defaultUrls).distinct().take(4) }
    val pager = rememberPagerState(pageCount = { photos.size })
    val dragging by pager.interactionSource.collectIsDraggedAsState()
    LaunchedEffect(pager, dragging, photos) {
        if (!dragging && photos.size > 1) while (isActive) {
            delay(3_500)
            if (!pager.isScrollInProgress) pager.animateScrollToPage((pager.currentPage + 1) % photos.size, animationSpec = tween(450))
        }
    }
    Card(onClick = onOpen, modifier = modifier, colors = CardDefaults.cardColors(containerColor = Charcoal), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, Line)) {
        Box(Modifier.fillMaxWidth().height(198.dp)) {
            HorizontalPager(pager, Modifier.fillMaxSize()) { page ->
                SubcomposeAsyncImage(model = photos[page], contentDescription = "${tournament.name} photo ${page + 1} of ${photos.size}", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop,
                    loading = { Image(painterResource(tournamentPhoto(tournament.sport)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) },
                    error = { Image(painterResource(tournamentPhoto(tournament.sport)), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) })
            }
            Surface(Modifier.align(Alignment.TopStart).padding(9.dp), color = Ink.copy(.74f), shape = RoundedCornerShape(18.dp)) {
                Text(tournament.sport.label.uppercase(), Modifier.padding(horizontal = 8.dp, vertical = 5.dp), color = PulseLavender, style = MaterialTheme.typography.labelSmall)
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(photos.size) { index -> Box(Modifier.width(if (index == pager.currentPage) 18.dp else 5.dp).height(5.dp).background(if (index == pager.currentPage) PulseLavender else Color.White.copy(.7f), CircleShape)) }
            }
        }
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(tournament.name, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${tournament.sport.label} · ${tournament.teamSize} players", color = PulseLavender, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(tournamentDate(tournament.date), color = TextMuted, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatInr(tournament.prizePool.toDouble()), fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                Text(formatInr(tournament.entryFee.toDouble()), color = PulseLavender, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.bodySmall, maxLines = 1)
            }
            Text("Prize pool · Entry per team", color = TextMuted, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
fun TournamentDetailsScreen(vm: AppViewModel, id: String, nav: NavController) {
    val tournaments by vm.tournaments.collectAsStateWithLifecycle()
    val registrations by vm.tournamentRegistrations.collectAsStateWithLifecycle()
    val tournament = tournaments.firstOrNull { it.id == id }
    val existing = registrations.firstOrNull { it.tournamentId == id }
    val busy by vm.tournamentPaymentBusy
    var teamName by rememberSaveable(id) { mutableStateOf("") }
    var captainName by rememberSaveable(id) { mutableStateOf(vm.profile.value.name) }
    var captainPhone by rememberSaveable(id) { mutableStateOf(vm.profile.value.phone) }
    var checkout by rememberSaveable(id) { mutableStateOf(false) }
    var selectedPaymentMethod by rememberSaveable(id) { mutableStateOf("Google Pay") }
    var formError by rememberSaveable(id) { mutableStateOf<String?>(null) }
    val registrationOpen = tournament?.let { it.active && runCatching { !LocalDate.parse(it.registrationDeadline).isBefore(LocalDate.now()) }.getOrDefault(false) } == true
    LazyColumn(
        Modifier.fillMaxSize().background(Ink).statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 48.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Back") }
            Text("TOURNAMENT", color = PulseLavender, style = MaterialTheme.typography.labelLarge)
        } }
        if (tournament == null) item { Text("Tournament unavailable.", Modifier.padding(20.dp), color = TextMuted) }
        else {
            item { TournamentCard(tournament, onOpen = {}, modifier = Modifier.padding(horizontal = 16.dp)) }
            item { Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("About the event", fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                Text(tournament.description, color = TextMuted)
                Text("Rules", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                Text(tournament.rules, color = TextMuted)
                Text("Registration closes ${tournamentDate(tournament.registrationDeadline)}", color = PulseLavender)
            } }
            if (existing != null) item { Surface(Modifier.fillMaxWidth().padding(horizontal = 16.dp), color = Steel, shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("${existing.teamName} · ${existing.status}", fontWeight = FontWeight.Bold)
                    Text("Entry fee: ${existing.paymentStatus} · ${existing.transactionId}", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                }
            } }
            else if (registrationOpen) {
                item { Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Register your team", fontFamily = ManropeUi, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
                    if (!checkout) {
                        Text("Team size: ${tournament.teamSize}. One team per account.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        OutlinedTextField(teamName, { teamName = it }, Modifier.fillMaxWidth(), label = { Text("Team name") }, singleLine = true)
                        OutlinedTextField(captainName, { captainName = it }, Modifier.fillMaxWidth(), label = { Text("Captain name") }, singleLine = true)
                        OutlinedTextField(captainPhone, { captainPhone = it }, Modifier.fillMaxWidth(), label = { Text("Captain phone") }, singleLine = true)
                        formError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                        Button(onClick = {
                            formError = runCatching { validateTournamentRegistration(tournament, teamName, captainName, captainPhone) }.exceptionOrNull()?.message
                            if (formError == null) checkout = true
                        }, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = PulsePurple, contentColor = Color.White)) {
                            Text("Continue to payment")
                        }
                    } else {
                        Text("${tournament.name} · ${teamName.trim()}", color = TextMuted)
                        Text("Entry fee: ${formatInr(tournament.entryFee.toDouble())}", color = PulseLavender, fontWeight = FontWeight.Bold)
                        Text("Choose a demo UPI method", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        listOf("Google Pay", "PhonePe", "Paytm", "BHIM UPI").forEach { method ->
                            Surface(onClick = { selectedPaymentMethod = method }, color = Steel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(selected = selectedPaymentMethod == method, onClick = { selectedPaymentMethod = method })
                                    Text(method)
                                }
                            }
                        }
                        Text("Demo checkout. No real money will be charged.", color = TextMuted, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { checkout = false }, enabled = !busy) { Text("Edit team details") }
                        Button(onClick = { vm.registerTournament(tournament, teamName, captainName, captainPhone, selectedPaymentMethod) {} }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = PulsePurple, contentColor = Color.White)) {
                            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                            else Text("Pay ${formatInr(tournament.entryFee.toDouble())} & register")
                        }
                    }
                } }
            } else item { Text("Registration is closed.", Modifier.padding(20.dp), color = TextMuted) }
        }
    }
}
