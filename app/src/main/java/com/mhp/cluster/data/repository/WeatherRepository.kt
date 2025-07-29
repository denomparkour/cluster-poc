package com.mhp.cluster.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import org.json.JSONObject
import android.util.Log

class WeatherRepository private constructor(private val context: Context) {
    
    private val sharedPreferences: SharedPreferences = context.getSharedPreferences(
        "weather_prefs", Context.MODE_PRIVATE
    )
    private val locationService = LocationService.getInstance(context)
    
    // In-memory cache for weather data
    private var cachedWeatherData: WeatherData? = null
    private var lastWeatherUpdate: Long = 0
    private val WEATHER_CACHE_DURATION = 10 * 60 * 1000 // 10 minutes
    
    companion object {
        private const val WEATHER_API_KEY = "26247d7e89e942e52097ea38c373bfc3"
        private const val BASE_URL = "https://api.openweathermap.org/data/2.5/weather"
        private const val KEY_LAST_WEATHER = "last_weather"
        private const val KEY_LAST_UPDATE = "last_update"
        
        @Volatile
        private var INSTANCE: WeatherRepository? = null
        
        fun getInstance(context: Context): WeatherRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WeatherRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    data class WeatherData(
        val temperature: Double,
        val description: String,
        val icon: String,
        val humidity: Int,
        val windSpeed: Double
    )
    
    suspend fun getCurrentWeather(): WeatherData? {
        return withContext(Dispatchers.IO) {
            try {
                // Check in-memory cache first
                val currentTime = System.currentTimeMillis()
                if (cachedWeatherData != null && 
                    (currentTime - lastWeatherUpdate) < WEATHER_CACHE_DURATION) {
                    Log.d("WeatherRepository", "CACHE HIT: Returning cached weather data: ${cachedWeatherData?.temperature}°C")
                    return@withContext cachedWeatherData
                } else {
                    Log.d("WeatherRepository", "CACHE MISS: Cache expired or empty, fetching fresh data")
                }
                
                // Check if we have location permission
                if (!locationService.hasLocationPermission()) {
                    Log.d("WeatherRepository", "No location permission")
                    return@withContext null
                }
                
                // Get current location
                Log.d("WeatherRepository", "Getting current location...")
                val location = locationService.getCurrentLocation() ?: locationService.getLastKnownLocation()
                if (location == null) {
                    Log.d("WeatherRepository", "No location available")
                    return@withContext null
                }
                
                Log.d("WeatherRepository", "Location: ${location.latitude}, ${location.longitude}")
                
                val url = "$BASE_URL?lat=${location.latitude}&lon=${location.longitude}&appid=$WEATHER_API_KEY&units=metric"
                Log.d("WeatherRepository", "Fetching weather from: $url")
                val response = URL(url).readText()
                val jsonObject = JSONObject(response)
                val main = jsonObject.getJSONObject("main")
                val weather = jsonObject.getJSONArray("weather").getJSONObject(0)
                val wind = jsonObject.getJSONObject("wind")

                val weatherData = WeatherData(
                    temperature = main.getDouble("temp"),
                    description = weather.getString("description"),
                    icon = weather.getString("icon"),
                    humidity = main.getInt("humidity"),
                    windSpeed = wind.getDouble("speed")
                )

                Log.d("WeatherRepository", "Weather data received: ${weatherData.temperature}°C, ${weatherData.description}")
                
                // Update in-memory cache
                cachedWeatherData = weatherData
                lastWeatherUpdate = currentTime
                
                // Save to SharedPreferences
                saveWeatherData(weatherData)
                
                weatherData
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error getting weather: ${e.message}", e)
                // Return cached data or null
                getCachedWeatherData()
            }
        }
    }
    
    private fun saveWeatherData(weatherData: WeatherData) {
        val json = JSONObject().apply {
            put("temperature", weatherData.temperature)
            put("description", weatherData.description)
            put("icon", weatherData.icon)
            put("humidity", weatherData.humidity)
            put("windSpeed", weatherData.windSpeed)
        }
        
        sharedPreferences.edit()
            .putString(KEY_LAST_WEATHER, json.toString())
            .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
            .apply()
    }
    
    private fun getCachedWeatherData(): WeatherData? {
        val jsonString = sharedPreferences.getString(KEY_LAST_WEATHER, null) ?: return null
        val lastUpdate = sharedPreferences.getLong(KEY_LAST_UPDATE, 0)
        
        // Check if data is less than 30 minutes old
        if (System.currentTimeMillis() - lastUpdate > 30 * 60 * 1000) {
            return null
        }
        
        return try {
            val json = JSONObject(jsonString)
            WeatherData(
                temperature = json.getDouble("temperature"),
                description = json.getString("description"),
                icon = json.getString("icon"),
                humidity = json.getInt("humidity"),
                windSpeed = json.getDouble("windSpeed")
            )
        } catch (e: Exception) {
            null
        }
    }
    
    fun clearCache() {
        cachedWeatherData = null
        lastWeatherUpdate = 0
        Log.d("WeatherRepository", "Weather cache cleared")
    }
    
    suspend fun forceRefreshWeather(): WeatherData? {
        clearCache()
        return getCurrentWeather()
    }
} 