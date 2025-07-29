package com.mhp.cluster.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder

class NavigationRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "navigation_prefs", Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    companion object {
        private const val GOOGLE_MAPS_API_KEY = "YOUR_GOOGLE_MAPS_API_KEY" // Replace with actual API key
        private const val DIRECTIONS_API_URL = "https://maps.googleapis.com/maps/api/directions/json"
        private const val KEY_LAST_ROUTE = "last_route"
        private const val KEY_CURRENT_DESTINATION = "current_destination"
    }
    
    data class RouteInfo(
        val destination: String,
        val eta: String,
        val distance: String,
        val duration: Int, // in seconds
        val polyline: String,
        val startLocation: LatLng,
        val endLocation: LatLng
    )
    
    suspend fun getRouteToDestination(
        destination: String,
        startLat: Double = 40.7128,
        startLng: Double = -74.0060
    ): RouteInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val origin = "$startLat,$startLng"
                val encodedDestination = URLEncoder.encode(destination, "UTF-8")
                val url = "$DIRECTIONS_API_URL?origin=$origin&destination=$encodedDestination&key=$GOOGLE_MAPS_API_KEY"
                
                val response = URL(url).readText()
                val jsonObject = com.google.gson.JsonParser.parseString(response).asJsonObject
                
                if (jsonObject.get("status").asString == "OK") {
                    val routes = jsonObject.getAsJsonArray("routes")
                    if (routes.size() > 0) {
                        val route = routes[0].asJsonObject
                        val legs = route.getAsJsonArray("legs")
                        val leg = legs[0].asJsonObject
                        
                        val distance = leg.get("distance").asJsonObject.get("text").asString
                        val duration = leg.get("duration").asJsonObject.get("value").asInt
                        val eta = leg.get("duration").asJsonObject.get("text").asString
                        
                        val routeInfo = RouteInfo(
                            destination = destination,
                            eta = eta,
                            distance = distance,
                            duration = duration,
                            polyline = route.get("overview_polyline").asJsonObject.get("points").asString,
                            startLocation = LatLng(startLat, startLng),
                            endLocation = LatLng(
                                leg.get("end_location").asJsonObject.get("lat").asDouble,
                                leg.get("end_location").asJsonObject.get("lng").asDouble
                            )
                        )
                        
                        // Save route info
                        saveRouteInfo(routeInfo)
                        
                        routeInfo
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                // Return cached route or null
                getCachedRouteInfo()
            }
        }
    }
    
    private fun saveRouteInfo(routeInfo: RouteInfo) {
        val json = gson.toJson(routeInfo)
        sharedPreferences.edit()
            .putString(KEY_LAST_ROUTE, json)
            .putString(KEY_CURRENT_DESTINATION, routeInfo.destination)
            .apply()
    }
    
    private fun getCachedRouteInfo(): RouteInfo? {
        val json = sharedPreferences.getString(KEY_LAST_ROUTE, null) ?: return null
        return try {
            gson.fromJson(json, RouteInfo::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    fun getCurrentDestination(): String? {
        return sharedPreferences.getString(KEY_CURRENT_DESTINATION, null)
    }
    
    fun clearCurrentRoute() {
        sharedPreferences.edit()
            .remove(KEY_LAST_ROUTE)
            .remove(KEY_CURRENT_DESTINATION)
            .apply()
    }
    
    // Simulate route progress for demo purposes
    fun getRouteProgress(): Float {
        val routeInfo = getCachedRouteInfo() ?: return 0f
        val totalDuration = routeInfo.duration.toLong()
        val elapsedTime = System.currentTimeMillis() / 1000 // Simulate elapsed time
        return (elapsedTime.toFloat() / totalDuration).coerceIn(0f, 1f)
    }
} 