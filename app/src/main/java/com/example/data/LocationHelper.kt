package com.example.data

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.example.model.LocationSample
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object LocationHelper {
    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    suspend fun fetchCurrentDeviceLocation(context: Context): Result<LocationSample> {
        if (!hasLocationPermission(context)) {
            return Result.failure(SecurityException("Location permission not granted"))
        }
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val freshLocation: Location? = client.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cts.token
            ).await()
            val resolved = freshLocation ?: client.lastLocation.await()
            if (resolved != null) {
                Result.success(
                    LocationSample(
                        latitude = resolved.latitude,
                        longitude = resolved.longitude,
                        accuracyMeters = resolved.accuracy.toDouble().coerceIn(0.0, 5000.0),
                        speedMps = resolved.speed.toDouble().coerceIn(0.0, 150.0),
                        bearing = resolved.bearing.toDouble().coerceIn(0.0, 360.0),
                        capturedAtMillis = System.currentTimeMillis()
                    )
                )
            } else {
                Result.failure(IllegalStateException("Device GPS did not return a fix. Enable location services or set a GPS coordinate in emulator controls."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun haversineKm(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2) * sin(dLng / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return ((r * c) * 10.0).roundToInt() / 10.0
    }
}
