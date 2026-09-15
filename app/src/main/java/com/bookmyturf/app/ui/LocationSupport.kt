package com.bookmyturf.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

data class UserPlace(val city: String, val latitude: Double, val longitude: Double)

val indianCities = listOf(
    "Ahmedabad", "Aizawl", "Ajmer", "Agra", "Aligarh", "Amritsar", "Aurangabad", "Bengaluru",
    "Bhopal", "Bhubaneswar", "Chandigarh", "Chennai", "Coimbatore", "Cuttack", "Dehradun",
    "Delhi", "Dhanbad", "Durgapur", "Faridabad", "Gandhinagar", "Ghaziabad", "Goa", "Gurugram",
    "Guwahati", "Gwalior", "Hubballi", "Hyderabad", "Imphal", "Indore", "Itanagar", "Jabalpur",
    "Jaipur", "Jalandhar", "Jammu", "Jamshedpur", "Jodhpur", "Kanpur", "Kochi", "Kolkata",
    "Kota", "Kozhikode", "Lucknow", "Ludhiana", "Madurai", "Mangaluru", "Meerut", "Mumbai",
    "Mysuru", "Nagpur", "Nashik", "Navi Mumbai", "Noida", "Panaji", "Patna", "Pimpri-Chinchwad",
    "Prayagraj", "Pune", "Raipur", "Rajkot", "Ranchi", "Shillong", "Shimla", "Siliguri", "Srinagar",
    "Surat", "Thane", "Thiruvananthapuram", "Thrissur", "Udaipur", "Vadodara", "Varanasi", "Vijayawada",
    "Visakhapatnam"
).sorted()

fun hasLocationPermission(context: Context) = ContextCompat.checkSelfPermission(
    context, Manifest.permission.ACCESS_COARSE_LOCATION
) == PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission")
suspend fun detectUserPlace(context: Context): UserPlace? {
    if (!hasLocationPermission(context)) return null
    val location = LocationServices.getFusedLocationProviderClient(context)
        .getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token).await()
        ?: return null
    val city = reverseGeocode(context, location.latitude, location.longitude) ?: return null
    return UserPlace(city, location.latitude, location.longitude)
}

suspend fun geocodeCity(context: Context, city: String): UserPlace? {
    val result = geocode(context, "$city, India") ?: return null
    return UserPlace(result.locality ?: result.subAdminArea ?: city, result.latitude, result.longitude)
}

private suspend fun reverseGeocode(context: Context, lat: Double, lon: Double): String? =
    geocodeCoordinates(context, lat, lon)?.let { it.locality ?: it.subAdminArea }

@Suppress("DEPRECATION")
private suspend fun geocode(context: Context, name: String): android.location.Address? {
    val coder = Geocoder(context, Locale.getDefault())
    return if (android.os.Build.VERSION.SDK_INT >= 33) suspendCancellableCoroutine { continuation ->
        coder.getFromLocationName(name, 1) { results -> if (continuation.isActive) continuation.resume(results.firstOrNull()) }
    } else withContext(Dispatchers.IO) { coder.getFromLocationName(name, 1)?.firstOrNull() }
}

@Suppress("DEPRECATION")
private suspend fun geocodeCoordinates(context: Context, lat: Double, lon: Double): android.location.Address? {
    val coder = Geocoder(context, Locale.getDefault())
    return if (android.os.Build.VERSION.SDK_INT >= 33) suspendCancellableCoroutine { continuation ->
        coder.getFromLocation(lat, lon, 1) { results -> if (continuation.isActive) continuation.resume(results.firstOrNull()) }
    } else withContext(Dispatchers.IO) { coder.getFromLocation(lat, lon, 1)?.firstOrNull() }
}

fun distanceKm(a: UserPlace, b: UserPlace): Double {
    val result = FloatArray(1)
    android.location.Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, result)
    return result[0] / 1000.0
}
