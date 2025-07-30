package com.mhp.cluster.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Polyline
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class NavigationRepository(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "navigation_prefs", Context.MODE_PRIVATE
    )
    private val gson = Gson()
    
    companion object {
        private const val NOMINATIM_API_URL = "https://nominatim.openstreetmap.org/search"
        private const val OSRM_API_URL = "https://router.project-osrm.org/route/v1"
        private const val KEY_LAST_ROUTE = "last_route"
        private const val KEY_CURRENT_DESTINATION = "current_destination"
        private const val KEY_SEARCH_RESULTS = "search_results"
    }
    
    data class RouteInfo(
        val destination: String,
        val eta: String,
        val distance: String,
        val duration: Int, // in seconds
        val polyline: List<GeoPoint>,
        val startLocation: GeoPoint,
        val endLocation: GeoPoint
    )
    
    data class SearchResult(
        val displayName: String,
        val lat: Double,
        val lon: Double,
        val type: String
    )
    
    suspend fun searchLocations(query: String): List<SearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val url = "$NOMINATIM_API_URL?q=$encodedQuery&format=json&limit=10&addressdetails=1"
                
                val response = URL(url).readText()
                val jsonArray = com.google.gson.JsonParser.parseString(response).asJsonArray
                
                val results = mutableListOf<SearchResult>()
                for (i in 0 until jsonArray.size()) {
                    val item = jsonArray[i].asJsonObject
                    val displayName = item.get("display_name").asString
                    val lat = item.get("lat").asString.toDouble()
                    val lon = item.get("lon").asString.toDouble()
                    val type = item.get("type").asString
                    
                    results.add(SearchResult(displayName, lat, lon, type))
                }

                saveSearchResults(results)
                results
            } catch (e: Exception) {

                getCachedSearchResults()
            }
        }
    }
    
    suspend fun searchLocationsNearby(query: String, currentLat: Double, currentLng: Double): List<SearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val encodedQuery = URLEncoder.encode(query, "UTF-8")

                val viewbox = "${currentLng - 1},${currentLat - 1},${currentLng + 1},${currentLat + 1}"
                val url = "$NOMINATIM_API_URL?q=$encodedQuery&format=json&limit=10&addressdetails=1&viewbox=$viewbox&bounded=1"
                
                val response = URL(url).readText()
                val jsonArray = com.google.gson.JsonParser.parseString(response).asJsonArray
                
                val results = mutableListOf<SearchResult>()
                for (i in 0 until jsonArray.size()) {
                    val item = jsonArray[i].asJsonObject
                    val displayName = item.get("display_name").asString
                    val lat = item.get("lat").asString.toDouble()
                    val lon = item.get("lon").asString.toDouble()
                    val type = item.get("type").asString

                    val distance = calculateDistance(currentLat, currentLng, lat, lon)
                    
                    results.add(SearchResult(displayName, lat, lon, type))
                }

                results.sortBy { calculateDistance(currentLat, currentLng, it.lat, it.lon) }

                val filteredResults = results.filter { 
                    calculateDistance(currentLat, currentLng, it.lat, it.lon) <= 1000.0 
                }

                saveSearchResults(filteredResults)
                filteredResults
            } catch (e: Exception) {
                android.util.Log.e("NavigationRepository", "Error in nearby search: ${e.message}")

                getCachedSearchResults()
            }
        }
    }
    
    suspend fun getRouteToDestination(
        destination: String,
        startLat: Double = 40.7128,
        startLng: Double = -74.0060
    ): RouteInfo? {

        if (startLat == 0.0 && startLng == 0.0) {
            android.util.Log.w("NavigationRepository", "Invalid coordinates (0,0), using default")
            return null
        }

        if (startLat < -90 || startLat > 90 || startLng < -180 || startLng > 180) {
            android.util.Log.w("NavigationRepository", "Coordinates out of bounds: lat=$startLat, lng=$startLng")
            return null
        }
        return withContext(Dispatchers.IO) {
            try {

                val searchResults = searchLocations(destination)
                if (searchResults.isEmpty()) {
                    android.util.Log.d("NavigationRepository", "No search results found for destination: $destination")
                    return@withContext null
                }
                
                val destinationPoint = searchResults[0]

                if (destinationPoint.lat < -90 || destinationPoint.lat > 90 || 
                    destinationPoint.lon < -180 || destinationPoint.lon > 180) {
                    android.util.Log.w("NavigationRepository", "Destination coordinates out of bounds: lat=${destinationPoint.lat}, lng=${destinationPoint.lon}")
                    return@withContext null
                }

                val distance = calculateDistance(startLat, startLng, destinationPoint.lat, destinationPoint.lon)
                if (distance > 1000.0) {
                    android.util.Log.w("NavigationRepository", "Distance too far: ${distance}km between start and destination")
                    return@withContext null
                }

                val origin = String.format("%.6f,%.6f", startLat, startLng)
                val dest = String.format("%.6f,%.6f", destinationPoint.lat, destinationPoint.lon)
                
                android.util.Log.d("NavigationRepository", "Origin: $origin, Destination: $dest")
                
                val url = "$OSRM_API_URL/driving/$origin;$dest?overview=full&geometries=geojson"
                android.util.Log.d("NavigationRepository", "Requesting route from: $url")
                
                val connection = URL(url).openConnection() as java.net.HttpURLConnection
                connection.connectTimeout = 10000 // 10 seconds
                connection.readTimeout = 10000 // 10 seconds
                connection.requestMethod = "GET"
                
                val responseCode = connection.responseCode
                if (responseCode != 200) {

                    val errorResponse = try {
                        connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "No error details"
                    } catch (e: Exception) {
                        "Error reading error response: ${e.message}"
                    }
                    android.util.Log.e("NavigationRepository", "HTTP error: $responseCode, Response: $errorResponse")
                    return@withContext null
                }
                
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                android.util.Log.d("NavigationRepository", "Route response: $response")
                val jsonObject = com.google.gson.JsonParser.parseString(response).asJsonObject
                
                if (jsonObject.get("code").asString == "Ok") {
                    val routes = jsonObject.getAsJsonArray("routes")
                    if (routes.size() > 0) {
                        val route = routes[0].asJsonObject
                        val distance = route.get("distance").asDouble
                        val duration = route.get("duration").asDouble

                        val geometry = route.getAsJsonObject("geometry")
                        val coordinates = geometry.getAsJsonArray("coordinates")
                        val polyline = mutableListOf<GeoPoint>()
                        
                        for (i in 0 until coordinates.size()) {
                            val coord = coordinates[i].asJsonArray
                            val lon = coord[0].asDouble
                            val lat = coord[1].asDouble
                            polyline.add(GeoPoint(lat, lon))
                        }
                        
                        val routeInfo = RouteInfo(
                            destination = destination,
                            eta = formatDuration(duration.toInt()),
                            distance = formatDistance(distance),
                            duration = duration.toInt(),
                            polyline = polyline,
                            startLocation = GeoPoint(startLat, startLng),
                            endLocation = GeoPoint(destinationPoint.lat, destinationPoint.lon)
                        )
                        
                        android.util.Log.d("NavigationRepository", "Route calculated successfully: ${routeInfo.eta}, ${routeInfo.distance}")

                        saveRouteInfo(routeInfo)
                        
                        routeInfo
                    } else {
                        android.util.Log.d("NavigationRepository", "No routes found in response")
                        null
                    }
                } else {
                    android.util.Log.d("NavigationRepository", "Route calculation failed with code: ${jsonObject.get("code").asString}")
                    null
                }
            } catch (e: Exception) {
                android.util.Log.e("NavigationRepository", "Error calculating route: ${e.message}", e)

                try {
                    val fallbackRoute = createFallbackRoute(destination, startLat, startLng)
                    if (fallbackRoute != null) {
                        android.util.Log.d("NavigationRepository", "Using fallback route")
                        saveRouteInfo(fallbackRoute)
                        return@withContext fallbackRoute
                    }
                } catch (fallbackException: Exception) {
                    android.util.Log.e("NavigationRepository", "Fallback route creation failed: ${fallbackException.message}")
                }

                getCachedRouteInfo()
            }
        }
    }
    
    private fun formatDuration(seconds: Int): String {
        val hours = TimeUnit.SECONDS.toHours(seconds.toLong())
        val minutes = TimeUnit.SECONDS.toMinutes(seconds.toLong()) % 60
        
        return when {
            hours > 0 -> "${hours}h ${minutes} min"
            else -> "${minutes} min"
        }
    }
    
    private fun formatDistance(meters: Double): String {
        val km = meters / 1000
        return if (km >= 1) {
            String.format("%.1f km", km)
        } else {
            String.format("%.0f m", meters)
        }
    }
    
    private fun saveRouteInfo(routeInfo: RouteInfo) {
        val json = gson.toJson(routeInfo)
        sharedPreferences.edit()
            .putString(KEY_LAST_ROUTE, json)
            .putString(KEY_CURRENT_DESTINATION, routeInfo.destination)
            .apply()
    }
    
    fun getCachedRouteInfo(): RouteInfo? {
        val json = sharedPreferences.getString(KEY_LAST_ROUTE, null) ?: return null
        return try {
            gson.fromJson(json, RouteInfo::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun saveSearchResults(results: List<SearchResult>) {
        val json = gson.toJson(results)
        sharedPreferences.edit()
            .putString(KEY_SEARCH_RESULTS, json)
            .apply()
    }
    
    private fun getCachedSearchResults(): List<SearchResult> {
        val json = sharedPreferences.getString(KEY_SEARCH_RESULTS, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<SearchResult>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
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
    
    suspend fun updateRouteWithCurrentLocation(currentLat: Double, currentLng: Double): RouteInfo? {
        val currentDestination = getCurrentDestination() ?: return null
        val cachedRoute = getCachedRouteInfo() ?: return null

        val distanceFromStart = calculateDistance(
            currentLat, currentLng,
            cachedRoute.startLocation.latitude, cachedRoute.startLocation.longitude
        )
        
        if (distanceFromStart < 0.1) { // Less than 100 meters from start
            return cachedRoute
        }

        return getRouteToDestination(currentDestination, currentLat, currentLng)
    }

    fun getRouteProgress(): Float {
        val routeInfo = getCachedRouteInfo() ?: return 0f
        val totalDuration = routeInfo.duration.toLong()
        val elapsedTime = System.currentTimeMillis() / 1000 // Simulate elapsed time
        return (elapsedTime.toFloat() / totalDuration).coerceIn(0f, 1f)
    }
    
    private fun createFallbackRoute(destination: String, startLat: Double, startLng: Double): RouteInfo? {
        return try {

            val estimatedDistance = 5000.0 // 5km default
            val estimatedDuration = 900 // 15 minutes default

            val endLat = startLat + 0.05 // About 5.5km north
            val endLng = startLng + 0.05 // About 5.5km east
            
            val polyline = listOf(
                GeoPoint(startLat, startLng),
                GeoPoint(endLat, endLng)
            )
            
            RouteInfo(
                destination = destination,
                eta = formatDuration(estimatedDuration),
                distance = formatDistance(estimatedDistance),
                duration = estimatedDuration,
                polyline = polyline,
                startLocation = GeoPoint(startLat, startLng),
                endLocation = GeoPoint(endLat, endLng)
            )
        } catch (e: Exception) {
            android.util.Log.e("NavigationRepository", "Error creating fallback route: ${e.message}")
            null
        }
    }
    
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Earth's radius in kilometers
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return r * c
    }
} 