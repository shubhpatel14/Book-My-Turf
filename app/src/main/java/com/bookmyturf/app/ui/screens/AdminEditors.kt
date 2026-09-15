@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.bookmyturf.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookmyturf.app.data.*
import com.bookmyturf.app.ui.AppViewModel
import com.bookmyturf.app.ui.components.*
import com.bookmyturf.app.ui.theme.*
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.launch

@Composable
internal fun TurfEditor(initial: Turf, vm: AppViewModel, close: () -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var area by remember { mutableStateOf(initial.area) }
    var city by remember { mutableStateOf(initial.city) }
    var price by remember { mutableStateOf(initial.pricePerHour.toString()) }
    var desc by remember { mutableStateOf(initial.description) }
    var facilities by remember { mutableStateOf(initial.facilities.joinToString(", ")) }
    var distance by remember { mutableStateOf(initial.distanceKm.toString()) }
    var rating by remember { mutableStateOf(initial.rating.toString()) }
    var opening by remember { mutableStateOf(initial.openingHour.toString()) }
    var closing by remember { mutableStateOf(initial.closingHour.toString()) }
    var indoor by remember { mutableStateOf(initial.isIndoor) }
    var sports by remember { mutableStateOf(initial.sports) }
    var photos by remember { mutableStateOf(initial.photoUris.ifEmpty { initial.imageUrls }) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var photoIndex by remember { mutableIntStateOf(0) }
    var confirmDelete by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            runCatching { importVenuePhoto(context, uri) }.onSuccess { imported ->
                photos = photos.toMutableList().apply { if (photoIndex < size) set(photoIndex, imported) else add(imported) }
            }.onFailure { error = it.message }
            busy = false
        }
    }
    ModalBottomSheet(onDismissRequest = { if (!busy) close() }, containerColor = Charcoal) {
        Column(Modifier.verticalScroll(rememberScrollState()).imePadding().padding(18.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle(if (initial.id.isBlank()) "Add turf" else "Edit turf")
            listOf(
                Triple("Turf name", name, { v: String -> name = v }), Triple("Area / address", area, { v: String -> area = v }),
                Triple("City", city, { v: String -> city = v }), Triple("Price per hour (₹)", price, { v: String -> price = v }),
                Triple("Description", desc, { v: String -> desc = v }), Triple("Facilities (comma separated)", facilities, { v: String -> facilities = v }),
                Triple("Distance (km)", distance, { v: String -> distance = v }), Triple("Rating (0–5)", rating, { v: String -> rating = v }),
                Triple("Opening hour (0–22)", opening, { v: String -> opening = v }), Triple("Closing hour (1–23)", closing, { v: String -> closing = v })
            ).forEach { (label, value, change) -> OutlinedTextField(value, change, Modifier.fillMaxWidth(), label = { Text(label) }, enabled = !busy) }
            Text("Sports")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Sport.entries.forEach { sport -> Tag(sport.label, sport in sports) { sports = if (sport in sports) sports - sport else sports + sport } }
            }
            Row { Text("Indoor venue", Modifier.weight(1f)); Switch(indoor, { indoor = it }, enabled = !busy) }

            Text("Venue photos · 4 cover photos · first is cover", fontWeight = FontWeight.Bold)
            Text("Remote HTTPS URLs stored in Firestore (no Cloud Storage billing required).", color = TextMuted, style = MaterialTheme.typography.bodySmall)

            photos.forEachIndexed { index, url ->
                Surface(
                    color = Steel,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (index == 0) PulseLavender.copy(alpha = 0.6f) else Line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (index == 0) "★ COVER PHOTO (SLOT 1)" else "PHOTO SLOT #${index + 1}",
                                color = if (index == 0) PulseLavender else TextMuted,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            if (index > 0) {
                                TextButton(onClick = {
                                    photos = listOf(photos[index]) + photos.filterIndexed { i, _ -> i != index }
                                }) { Text("Make Cover", color = PulseLavender) }
                            }
                            TextButton(onClick = {
                                photos = photos.filterIndexed { i, _ -> i != index }
                            }) { Text("Remove", color = Danger) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            VenuePhoto(initial.copy(photoUris = photos), Modifier.size(80.dp, 60.dp).clip(RoundedCornerShape(8.dp)), index)
                            OutlinedTextField(
                                value = url,
                                onValueChange = { newUrl ->
                                    photos = photos.toMutableList().apply { set(index, newUrl) }
                                },
                                modifier = Modifier.weight(1f),
                                label = { Text("Image URL") },
                                singleLine = true,
                                enabled = !busy
                            )
                        }
                    }
                }
            }

            if (photos.size < 4) {
                var newUrlInput by remember { mutableStateOf("") }
                Surface(
                    color = Steel.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Line),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Add image URL · Slot ${photos.size + 1} of 4", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = newUrlInput,
                                onValueChange = { newUrlInput = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Enter HTTPS Image URL") },
                                placeholder = { Text("https://example.com/turf.jpg") },
                                singleLine = true,
                                enabled = !busy
                            )
                            Button(
                                onClick = {
                                    if (newUrlInput.isNotBlank()) {
                                        photos = photos + newUrlInput.trim()
                                        newUrlInput = ""
                                    }
                                },
                                enabled = !busy && newUrlInput.isNotBlank(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PulsePurple, contentColor = Color.White)
                            ) { Text("Add") }
                        }
                        Text("Quick Presets (public HTTPS sports photos):", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TurfImagePresets.items.forEach { (title, url) ->
                                OutlinedButton(
                                    onClick = { photos = photos + url },
                                    enabled = !busy && photos.size < 4 && url !in photos,
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) { Text(title, style = MaterialTheme.typography.labelSmall) }
                            }
                            OutlinedButton(
                                onClick = { photoIndex = photos.size; picker.launch(arrayOf("image/*")) },
                                enabled = !busy && photos.size < 4,
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) { Text("Device Gallery", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                }
            }
            error?.let { Text(it, color = Danger) }
            PrimaryButton(if (busy) "Saving…" else "Save turf", enabled = !busy, onClick = {
                val parsedPrice = price.toIntOrNull(); val parsedDistance = distance.toDoubleOrNull(); val parsedRating = rating.toDoubleOrNull()
                val open = opening.toIntOrNull(); val shut = closing.toIntOrNull()
                if (name.isBlank() || area.isBlank() || city.isBlank() || parsedPrice == null || parsedPrice <= 0 || parsedDistance == null || !parsedDistance.isFinite() || parsedDistance < 0 || parsedRating == null || parsedRating !in 0.0..5.0 || sports.isEmpty() || open == null || open !in 0..22 || shut == null || shut !in (open + 1)..23) {
                    error = "Enter name, area, city, a positive price, valid distance/rating, at least one sport, and valid opening hours."
                } else {
                    busy = true
                    val job = vm.upsertTurf(initial.copy(id = initial.id.ifBlank { UUID.randomUUID().toString() }, name = name.trim(), area = area.trim(), city = city.trim(), pricePerHour = parsedPrice, description = desc.trim(), facilities = facilities.split(',').map { it.trim() }.filter { it.isNotBlank() }, sports = sports, isIndoor = indoor, distanceKm = parsedDistance, rating = parsedRating, photoUris = photos, openingHour = open, closingHour = shut), close)
                    scope.launch { job.join(); busy = false }
                }
            })
            if (initial.id.isNotBlank()) TextButton(enabled = !busy, onClick = { confirmDelete = true }) { Text("Delete turf", color = Danger) }
        }
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, containerColor = Charcoal,
        title = { Text("Delete ${initial.name}?") }, text = { Text("The venue will be removed from customer listings. Booking history will be retained. You can deactivate it instead to hide it temporarily.") },
        confirmButton = { TextButton(onClick = { vm.deleteTurf(initial.id); confirmDelete = false; close() }) { Text("DELETE", color = Danger) } },
        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("KEEP TURF") } })
}

@Composable
internal fun SlotManager(turf: Turf, vm: AppViewModel, close: () -> Unit) {
    val ops by vm.operations.collectAsStateWithLifecycle()
    val bookings by vm.bookings.collectAsStateWithLifecycle()
    var date by remember { mutableStateOf(LocalDate.now().format(bookingDateFormat)) }
    var changes by remember(date) { mutableStateOf<Map<String, TurfSlot>>(emptyMap()) }
    var priceInputs by remember(date) { mutableStateOf<Map<String, String>>(emptyMap()) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val slots = venueSlots(turf, date, ops, bookings)
    ModalBottomSheet(onDismissRequest = close, containerColor = Charcoal) {
        Column(Modifier.padding(18.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle("${turf.name} slots")
            Text("Blocked slots disappear from customer availability. Booked slots must be cancelled from Bookings.", color = TextMuted)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items((0L..6L).map { LocalDate.now().plusDays(it).format(bookingDateFormat) }) { day -> Tag(day, date == day) { date = day } }
            }
            LazyColumn(Modifier.heightIn(max = 400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(slots, key = { it.time }) { original ->
                    val slot = changes[original.time] ?: original
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(slot.time, Modifier.weight(1f))
                        OutlinedTextField(
                            value = priceInputs[slot.time] ?: slot.price.toString(),
                            onValueChange = { value ->
                                val digits = value.filter(Char::isDigit).take(6)
                                priceInputs = priceInputs + (slot.time to digits)
                                digits.toIntOrNull()?.takeIf { it > 0 }?.let { amount -> changes = changes + (slot.time to slot.copy(price = amount)) }
                            },
                            modifier = Modifier.width(105.dp), singleLine = true, label = { Text("₹ / hour") },
                            enabled = original.state != SlotState.BOOKED,
                            isError = priceInputs[slot.time]?.let { (it.toIntOrNull() ?: 0) <= 0 } == true
                        )
                        if (original.state == SlotState.BOOKED) Text("BOOKED", color = TextMuted)
                        else Tag(slot.state.name) { changes = changes + (slot.time to slot.copy(state = if (slot.state == SlotState.BLOCKED) SlotState.AVAILABLE else SlotState.BLOCKED)) }
                    }
                }
            }
            PrimaryButton("Save slot changes", enabled = !busy && priceInputs.values.all { (it.toIntOrNull() ?: 0) > 0 }, onClick = {
                busy = true
                val existing = ops.slotOverrides["${turf.id}|$date"].orEmpty().associateBy { it.time }
                val job = vm.saveSlots(turf.id, date, (existing + changes).values.toList(), close)
                scope.launch { job.join(); busy = false }
            })
        }
    }
}
