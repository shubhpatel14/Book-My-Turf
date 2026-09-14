@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package com.bookmyturf.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    var photos by remember { mutableStateOf(initial.photoUris) }
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
            Text("Venue photos · up to 4 · first photo is the cover")
            photos.forEachIndexed { index, _ ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VenuePhoto(initial.copy(photoUris = photos), Modifier.size(72.dp), index)
                    TextButton(enabled = !busy && CloudFeatures.photoUploadsEnabled, onClick = { photoIndex = index; picker.launch(arrayOf("image/*")) }) { Text("Replace") }
                    TextButton(enabled = !busy, onClick = { photos = photos.filterIndexed { i, _ -> i != index } }) { Text("Remove") }
                    if (index > 0) TextButton(enabled = !busy, onClick = { photos = listOf(photos[index]) + photos.filterIndexed { i, _ -> i != index } }) { Text("Cover") }
                }
            }
            if (!CloudFeatures.photoUploadsEnabled) Text(CloudFeatures.photosUnavailable, color = TextMuted)
            if (photos.size < 4) OutlinedButton(enabled = !busy && CloudFeatures.photoUploadsEnabled, onClick = { photoIndex = photos.size; picker.launch(arrayOf("image/*")) }) { Text("Add photo") }
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
