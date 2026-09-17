@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bookmyturf.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookmyturf.app.data.*
import com.bookmyturf.app.ui.AppViewModel
import com.bookmyturf.app.ui.components.formatInr
import com.bookmyturf.app.ui.theme.*
import java.time.LocalDate
import java.util.UUID

@Composable
fun TournamentManagement(vm: AppViewModel) {
    val tournaments by vm.tournaments.collectAsStateWithLifecycle()
    val registrations by vm.tournamentRegistrations.collectAsStateWithLifecycle()
    val tournamentSyncUnavailable by vm.tournamentSyncUnavailable.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Tournament?>(null) }
    var selected by remember { mutableStateOf<String?>(null) }
    val selectedTournament = tournaments.firstOrNull { it.id == selected }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 14.dp, 16.dp, 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("${tournaments.size} TOURNAMENTS · ${registrations.size} TEAM REGISTRATIONS", color = TextMuted, style = MaterialTheme.typography.labelMedium) }
        if (tournamentSyncUnavailable) item { Text("Tournament sync needs Firestore permissions before changes can be saved.", color = Danger) }
        item { Button(onClick = {
            val date = LocalDate.now().plusDays(21)
            editing = Tournament("", "", Sport.FOOTBALL, "Vadodara", "", date.toString(), date.minusDays(3).toString(), 6, 16, 1000, 20000, "", "")
        }, colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) { Text("ADD TOURNAMENT") } }
        if (tournaments.isEmpty()) item { OutlinedButton(onClick = { sampleTournaments().forEach(vm::upsertTournament) }) { Text("PUBLISH SAMPLE TOURNAMENTS") } }
        items(tournaments, key = { it.id }) { tournament ->
            Surface(color = Charcoal, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.dp, Line)) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(tournament.name, fontWeight = FontWeight.ExtraBold)
                    Text("${tournament.sport.label} · ${tournamentDate(tournament.date)} · ${tournament.city}", color = TextMuted)
                    Text("${tournament.teamSize} players · ${formatInr(tournament.entryFee.toDouble())} entry · ${formatInr(tournament.prizePool.toDouble())} prize", color = Lime, style = MaterialTheme.typography.bodySmall)
                    Text("${registrations.count { it.tournamentId == tournament.id && it.status == "CONFIRMED" }} / ${tournament.maxTeams} teams", color = TextMuted)
                    Row {
                        TextButton(onClick = { editing = tournament }) { Text("EDIT") }
                        TextButton(onClick = { selected = tournament.id }) { Text("TEAMS") }
                        Switch(tournament.active, { vm.setTournamentActive(tournament.id, it) })
                    }
                }
            }
        }
    }
    editing?.let { tournament -> TournamentEditor(tournament, vm) { editing = null } }
    if (selectedTournament != null) ModalBottomSheet(onDismissRequest = { selected = null }, containerColor = Charcoal) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${selectedTournament.name} · teams", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
            val teams = registrations.filter { it.tournamentId == selectedTournament.id }
            if (teams.isEmpty()) Text("No registrations yet.", color = TextMuted)
            teams.forEach { team ->
                Surface(color = Steel, shape = RoundedCornerShape(10.dp)) { Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${team.teamName} · ${team.status}", fontWeight = FontWeight.Bold)
                    Text("Captain: ${team.captainName} · ${team.captainPhone}", color = TextMuted)
                    Text("${team.paymentStatus} · ${team.transactionId}", color = Lime, style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { vm.setTournamentRegistrationStatus(team.id, if (team.status == "CONFIRMED") "CANCELLED" else "CONFIRMED") }) {
                        Text(if (team.status == "CONFIRMED") "CANCEL TEAM" else "RESTORE TEAM", color = if (team.status == "CONFIRMED") Danger else Lime)
                    }
                } }
            }
        }
    }
}

@Composable
private fun TournamentEditor(initial: Tournament, vm: AppViewModel, onDismiss: () -> Unit) {
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var sport by remember(initial.id) { mutableStateOf(initial.sport) }
    var city by remember(initial.id) { mutableStateOf(initial.city) }
    var venue by remember(initial.id) { mutableStateOf(initial.venue) }
    var date by remember(initial.id) { mutableStateOf(initial.date) }
    var deadline by remember(initial.id) { mutableStateOf(initial.registrationDeadline) }
    var teamSize by remember(initial.id) { mutableStateOf(initial.teamSize.toString()) }
    var maxTeams by remember(initial.id) { mutableStateOf(initial.maxTeams.toString()) }
    var entryFee by remember(initial.id) { mutableStateOf(initial.entryFee.toString()) }
    var prizePool by remember(initial.id) { mutableStateOf(initial.prizePool.toString()) }
    var description by remember(initial.id) { mutableStateOf(initial.description) }
    var rules by remember(initial.id) { mutableStateOf(initial.rules) }
    var imageUrls by remember(initial.id) { mutableStateOf(List(4) { initial.imageUrls.getOrNull(it).orEmpty() }) }
    var error by remember { mutableStateOf<String?>(null) }
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Charcoal) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(if (initial.id.isBlank()) "New tournament" else "Edit tournament", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("Tournament name") })
            Text("Sport", color = TextMuted)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sport.entries.forEach { option -> FilterChip(sport == option, { sport = option }, label = { Text(option.label) }) }
            }
            OutlinedTextField(city, { city = it }, Modifier.fillMaxWidth(), label = { Text("City") })
            OutlinedTextField(venue, { venue = it }, Modifier.fillMaxWidth(), label = { Text("Venue and area") })
            OutlinedTextField(date, { date = it }, Modifier.fillMaxWidth(), label = { Text("Event date (YYYY-MM-DD)") })
            OutlinedTextField(deadline, { deadline = it }, Modifier.fillMaxWidth(), label = { Text("Registration deadline (YYYY-MM-DD)") })
            OutlinedTextField(teamSize, { teamSize = it }, Modifier.fillMaxWidth(), label = { Text("Players per team") })
            OutlinedTextField(maxTeams, { maxTeams = it }, Modifier.fillMaxWidth(), label = { Text("Maximum teams") })
            OutlinedTextField(entryFee, { entryFee = it }, Modifier.fillMaxWidth(), label = { Text("Entry fee per team (₹)") })
            OutlinedTextField(prizePool, { prizePool = it }, Modifier.fillMaxWidth(), label = { Text("Prize pool (₹)") })
            OutlinedTextField(description, { description = it }, Modifier.fillMaxWidth(), label = { Text("Description") }, minLines = 2)
            OutlinedTextField(rules, { rules = it }, Modifier.fillMaxWidth(), label = { Text("Rules") }, minLines = 2)
            Text("Card photos · four HTTPS image URLs", color = TextMuted)
            imageUrls.forEachIndexed { index, url ->
                OutlinedTextField(url, { value -> imageUrls = imageUrls.toMutableList().also { it[index] = value } }, Modifier.fillMaxWidth(), label = { Text("Image URL ${index + 1}") }, singleLine = true)
            }
            error?.let { Text(it, color = Danger) }
            Button(onClick = {
                runCatching {
                    val value = initial.copy(id = initial.id.ifBlank { UUID.randomUUID().toString() }, name = name, sport = sport, city = city, venue = venue, date = date, registrationDeadline = deadline,
                        teamSize = teamSize.toInt(), maxTeams = maxTeams.toInt(), entryFee = entryFee.toInt(), prizePool = prizePool.toInt(), description = description, rules = rules,
                        imageUrls = imageUrls.map(String::trim).filter(String::isNotBlank))
                    validateTournament(value)
                    value
                }.onSuccess { vm.upsertTournament(it, onDismiss) }.onFailure { error = it.message ?: "Check the fields." }
            }, Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Lime, contentColor = Ink)) { Text("SAVE TOURNAMENT") }
        }
    }
}
