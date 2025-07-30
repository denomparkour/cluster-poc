package com.mhp.cluster.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import android.util.Log

class LocationService private constructor(private val context: Context) {
    
    private val fusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)
    
    private var cachedLocation: Location? = null
    private var lastLocationUpdate: Long = 0
    private val LOCATION_CACHE_DURATION = 5 * 60 * 1000 // 5 minutes
    
    companion object {
        @Volatile
        private var INSTANCE: LocationService? = null
        
        fun getInstance(context: Context): LocationService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocationService(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun getCachedLocation(): Location? {
        val currentTime = System.currentTimeMillis()
        return if (cachedLocation != null && 
                   (currentTime - lastLocationUpdate) < LOCATION_CACHE_DURATION) {
            Log.d("LocationService", "CACHE HIT: Returning cached location: ${cachedLocation?.latitude}, ${cachedLocation?.longitude}")
            cachedLocation
        } else {
            Log.d("LocationService", "CACHE MISS: Cache expired or no cached location")
            null
        }
    }
    
    private fun updateCachedLocation(location: Location?) {
        cachedLocation = location
        lastLocationUpdate = System.currentTimeMillis()
        Log.d("LocationService", "Updated cached location: ${location?.latitude}, ${location?.longitude}")
    }
    
    suspend fun getCurrentLocation(): Location? {
        if (!hasLocationPermission()) {
            Log.d("LocationService", "No location permission")
            return null
        }

        getCachedLocation()?.let { return it }
        
        return try {
            Log.d("LocationService", "Requesting current location...")
            withTimeout(10000) { // 10 second timeout
                suspendCancellableCoroutine { continuation ->
                    val cancellationTokenSource = CancellationTokenSource()
                    
                    fusedLocationClient.getCurrentLocation(
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                        cancellationTokenSource.token
                    ).addOnSuccessListener { location ->
                        Log.d("LocationService", "Current location received: ${location?.latitude}, ${location?.longitude}")
                        updateCachedLocation(location)
                        continuation.resume(location)
                    }.addOnFailureListener { exception ->
                        Log.e("LocationService", "Failed to get current location: ${exception.message}")
                        continuation.resumeWithException(exception)
                    }
                    
                    continuation.invokeOnCancellation {
                        Log.d("LocationService", "Location request cancelled")
                        cancellationTokenSource.cancel()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LocationService", "Error getting current location: ${e.message}", e)
            null
        }
    }
    
    fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) {
            Log.d("LocationService", "No location permission for last known location")
            return null
        }

        getCachedLocation()?.let { return it }
        
        return try {
            Log.d("LocationService", "Getting last known location...")
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var bestLocation: Location? = null
            
            val providers = locationManager.getProviders(true)
            Log.d("LocationService", "Available providers: ${providers.joinToString()}")
            
            for (provider in providers) {
                val location = locationManager.getLastKnownLocation(provider)
                if (location != null) {
                    Log.d("LocationService", "Provider $provider: ${location.latitude}, ${location.longitude}, accuracy: ${location.accuracy}")
                    if (bestLocation == null || location.accuracy < bestLocation.accuracy) {
                        bestLocation = location
                    }
                }
            }
            
            if (bestLocation != null) {
                Log.d("LocationService", "Best last known location: ${bestLocation.latitude}, ${bestLocation.longitude}")
                updateCachedLocation(bestLocation)
            } else {
                Log.d("LocationService", "No last known location available")
            }
            
            bestLocation
        } catch (e: Exception) {
            Log.e("LocationService", "Error getting last known location: ${e.message}", e)
            null
        }
    }
    
    fun clearCache() {
        cachedLocation = null
        lastLocationUpdate = 0
        Log.d("LocationService", "Location cache cleared")
    }
} 