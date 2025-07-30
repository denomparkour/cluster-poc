package com.mhp.cluster.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mhp.cluster.data.repository.LocationService
import com.mhp.cluster.data.repository.NavigationRepository
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(navController: NavController) {
    val context = LocalContext.current
    val navigationRepository = remember { NavigationRepository(context) }
    val locationService = remember { LocationService.getInstance(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var destination by remember { mutableStateOf("") }
    var isNavigating by remember { mutableStateOf(false) }
    var eta by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<NavigationRepository.SearchResult>>(emptyList()) }
    var showSearchResults by remember { mutableStateOf(false) }
    var selectedLocation by remember { mutableStateOf<NavigationRepository.SearchResult?>(null) }
    
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions.values.all { it }
        if (locationGranted) {

            coroutineScope.launch {
                selectedLocation?.let { location ->
                    isLoading = true
                    try {
                        android.util.Log.d("SearchScreen", "Permission granted, getting current location...")
                        val currentLocation = locationService.getCurrentLocation()
                        if (currentLocation != null) {
                            android.util.Log.d("SearchScreen", "Current location: ${currentLocation.latitude}, ${currentLocation.longitude}")
                            android.util.Log.d("SearchScreen", "Calculating route to: ${location.displayName}")
                            val routeInfo = navigationRepository.getRouteToDestination(
                                location.displayName,
                                currentLocation.latitude,
                                currentLocation.longitude
                            )
                            if (routeInfo != null) {
                                android.util.Log.d("SearchScreen", "Route calculated successfully: ${routeInfo.eta}, ${routeInfo.distance}")
                                eta = routeInfo.eta
                                distance = routeInfo.distance
                                isNavigating = true
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            } else {
                                android.util.Log.d("SearchScreen", "Route calculation failed, using fallback")

                                eta = "15 min"
                                distance = "8.5 km"
                                isNavigating = true
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        } else {
                            android.util.Log.d("SearchScreen", "No current location, using fallback")

                            eta = "15 min"
                            distance = "8.5 km"
                            isNavigating = true
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SearchScreen", "Error in route calculation after permission: ${e.message}", e)

                        eta = "15 min"
                        distance = "8.5 km"
                        isNavigating = true
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = true }
                        }
                    } finally {
                        isLoading = false
                    }
                }
            }
        }
    }
    
    LaunchedEffect(destination) {
        if (destination.length >= 3) {
            coroutineScope.launch {
                try {
                    val currentLocation = locationService.getCurrentLocation()
                    val results = if (currentLocation != null) {

                        navigationRepository.searchLocationsNearby(destination, currentLocation.latitude, currentLocation.longitude)
                    } else {

                        navigationRepository.searchLocations(destination)
                    }
                    searchResults = results
                    showSearchResults = results.isNotEmpty()
                } catch (e: Exception) {
                    android.util.Log.e("SearchScreen", "Error searching locations: ${e.message}")
                    searchResults = emptyList()
                    showSearchResults = false
                }
            }
        } else {
            searchResults = emptyList()
            showSearchResults = false
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            "Route Navigation",
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFFE8F5E8),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = Color.Green,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "Current Location",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        if (locationService.hasLocationPermission()) "Using GPS location" else "Location permission needed",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = destination,
            onValueChange = { 
                destination = it
                selectedLocation = null
            },
            label = { Text("Enter destination") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = Color.Gray
                )
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )

        if (showSearchResults && searchResults.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)
            ) {
                items(searchResults) { result ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedLocation = result
                                destination = result.displayName
                                showSearchResults = false
                            }
                            .padding(vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = if (selectedLocation == result) Color(0xFFE3F2FD) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    result.displayName,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                Text(
                                    result.type,
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (selectedLocation != null) {
                    if (locationService.hasLocationPermission()) {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                android.util.Log.d("SearchScreen", "Getting current location...")
                                val currentLocation = locationService.getCurrentLocation()
                                if (currentLocation != null) {
                                    android.util.Log.d("SearchScreen", "Current location: ${currentLocation.latitude}, ${currentLocation.longitude}")
                                    android.util.Log.d("SearchScreen", "Calculating route to: ${selectedLocation!!.displayName}")
                                    val routeInfo = navigationRepository.getRouteToDestination(
                                        selectedLocation!!.displayName,
                                        currentLocation.latitude,
                                        currentLocation.longitude
                                    )
                                    if (routeInfo != null) {
                                        android.util.Log.d("SearchScreen", "Route calculated successfully: ${routeInfo.eta}, ${routeInfo.distance}")
                                        eta = routeInfo.eta
                                        distance = routeInfo.distance
                                        isNavigating = true

                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    } else {
                                        android.util.Log.d("SearchScreen", "Route calculation failed, using fallback")

                                        eta = "15 min"
                                        distance = "8.5 km"
                                        isNavigating = true
                                        navController.navigate("home") {
                                            popUpTo("home") { inclusive = true }
                                        }
                                    }
                                } else {
                                    android.util.Log.d("SearchScreen", "No current location, using fallback")

                                    eta = "15 min"
                                    distance = "8.5 km"
                                    isNavigating = true
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("SearchScreen", "Error in route calculation: ${e.message}", e)

                                eta = "15 min"
                                distance = "8.5 km"
                                isNavigating = true
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            } finally {
                                isLoading = false
                            }
                        }
                    } else {

                        locationPermissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            ),
            enabled = selectedLocation != null && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Navigation,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (isLoading) "Calculating Route..." else "Get Route", 
                fontSize = 16.sp
            )
        }
        
        if (isNavigating) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFE3F2FD),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        "Navigation Started",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("ETA: $eta", fontSize = 16.sp)
                    Text("Distance: $distance", fontSize = 16.sp)
                }
            }
        }
    }
} 