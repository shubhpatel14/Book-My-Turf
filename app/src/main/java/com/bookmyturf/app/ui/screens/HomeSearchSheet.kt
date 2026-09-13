@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.bookmyturf.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bookmyturf.app.data.Sport
import com.bookmyturf.app.data.Turf
import com.bookmyturf.app.ui.components.SportIcon
import com.bookmyturf.app.ui.components.TurfVisual
import com.bookmyturf.app.ui.theme.*

@Composable
fun HomeSearchSheet(
    turfs: List<Turf>,
    selectedCity: String?,
    selectedSport: Sport?,
    onCityChange: (String?) -> Unit,
    onSportChange: (Sport?) -> Unit,
    onTurfOpen: (Turf) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val cities = remember(turfs) { turfs.map { it.city }.distinct() }
    val matches = remember(turfs, query, selectedCity, selectedSport) {
        turfs.filter { turf ->
            turf.active &&
                (selectedCity == null || turf.city == selectedCity) &&
                (selectedSport == null || selectedSport in turf.sports) &&
                (query.isBlank() || turf.name.contains(query, true) || turf.area.contains(query, true) ||
                    turf.city.contains(query, true) || turf.sports.any { it.label.contains(query, true) })
        }.sortedBy { it.distanceKm }
    }
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = PulsePurple,
        selectedLabelColor = Color.White,
        selectedLeadingIconColor = Color.White
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Charcoal,
        dragHandle = { BottomSheetDefaults.DragHandle(color = PulseLavender) },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(bottom = 12.dp)) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                Text("FIND YOUR TURF", style = MaterialTheme.typography.headlineMedium)
                Text("Choose a location or sport. Results update instantly.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search turfs, areas or sports") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PulseLavender, unfocusedBorderColor = Line)
                )
                Spacer(Modifier.height(18.dp))
                Text("LOCATION", color = TextMuted, style = MaterialTheme.typography.labelMedium)
            }
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = selectedCity == null, onClick = { onCityChange(null) }, colors = chipColors, label = { Text("Near me") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, null, Modifier.size(17.dp)) })
                }
                items(cities) { city ->
                    FilterChip(selected = selectedCity == city, onClick = { onCityChange(if (selectedCity == city) null else city) }, colors = chipColors, label = { Text(city) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("SPORT", Modifier.padding(horizontal = 20.dp), color = TextMuted, style = MaterialTheme.typography.labelMedium)
            LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item { FilterChip(selected = selectedSport == null, onClick = { onSportChange(null) }, colors = chipColors, label = { Text("All sports") }) }
                items(Sport.entries) { sport ->
                    FilterChip(
                        selected = selectedSport == sport,
                        onClick = { onSportChange(if (selectedSport == sport) null else sport) },
                        colors = chipColors,
                        label = { Text(sport.label) },
                        leadingIcon = { SportIcon(sport, Modifier.size(17.dp)) }
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${matches.size} MATCHES", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                Text("NEAREST FIRST", color = PulseLavender, style = MaterialTheme.typography.labelSmall)
            }
            if (matches.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(30.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.SearchOff, null, Modifier.size(38.dp), tint = TextMuted)
                    Spacer(Modifier.height(8.dp)); Text("No matching turfs")
                    Text("Try another location or sport.", color = TextMuted, style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(Modifier.heightIn(max = 330.dp), contentPadding = PaddingValues(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(matches, key = { it.id }) { turf ->
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { onTurfOpen(turf) },
                            color = Steel,
                            shape = RoundedCornerShape(15.dp),
                            border = BorderStroke(1.dp, Line)
                        ) {
                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                TurfVisual(turf.visualSeed, Modifier.size(82.dp, 66.dp).clip(RoundedCornerShape(11.dp)))
                                Column(Modifier.weight(1f).padding(horizontal = 11.dp)) {
                                    Text(turf.name, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${turf.area}, ${turf.city} • ${turf.distanceKm} km", color = TextMuted, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                                Text("₹${turf.pricePerHour}", color = PulseLavender, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}
