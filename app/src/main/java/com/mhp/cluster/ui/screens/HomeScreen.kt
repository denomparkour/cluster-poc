package com.mhp.cluster.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhp.cluster.R
import com.mhp.cluster.ui.theme.WidgetBackground
import androidx.compose.foundation.clickable
import androidx.navigation.NavController
import com.mhp.cluster.ui.navigation.Screen
import com.mhp.cluster.data.repository.WeatherRepository
import com.mhp.cluster.data.repository.NavigationRepository
import com.mhp.cluster.data.repository.LocationService
import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.remember

@Composable
fun HomeScreen(navController: NavController) {
    val selectedTab = remember { 0 }
    var isLocked by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val weatherRepository = remember { WeatherRepository.getInstance(context) }
    val navigationRepository = remember { NavigationRepository(context) }
    val locationService = remember { LocationService.getInstance(context) }
    var weatherData by remember { mutableStateOf<WeatherRepository.WeatherData?>(null) }
    var routeInfo by remember { mutableStateOf<NavigationRepository.RouteInfo?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentProgress by remember { mutableStateOf(0.3f) }
    var isWeatherLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions.values.all { it }
        if (locationGranted) {
            // Permission granted, refresh weather data
            coroutineScope.launch {
                isWeatherLoading = true
                try {
                    weatherData = weatherRepository.forceRefreshWeather()
                } catch (e: Exception) {
                    // Handle error if needed
                } finally {
                    isWeatherLoading = false
                }
            }
        }
    }
    
    // Check if location permission is granted
    val hasLocationPermission by remember {
        derivedStateOf { locationService.hasLocationPermission() }
    }
    
    // Load weather data and navigation info
    LaunchedEffect(Unit) {
        // Load cached route info if available
        val currentDestination = navigationRepository.getCurrentDestination()
        if (currentDestination != null) {
            // Try to load cached route info
            // For now, we'll use demo data
            routeInfo = NavigationRepository.RouteInfo(
                destination = currentDestination,
                eta = "9 min",
                distance = "1.8 mi",
                duration = 540,
                polyline = "",
                startLocation = com.google.android.gms.maps.model.LatLng(40.7128, -74.0060),
                endLocation = com.google.android.gms.maps.model.LatLng(40.7589, -73.9851)
            )
        }
        
        // Auto-load weather on app start if permission is already granted
        if (hasLocationPermission) {
            isWeatherLoading = true
            coroutineScope.launch {
                try {
                    weatherData = weatherRepository.getCurrentWeather()
                } catch (e: Exception) {
                    // Handle error if needed
                } finally {
                    isWeatherLoading = false
                }
            }
        }
    }
    
    // Load weather data when location permission changes
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            isWeatherLoading = true
            try {
                weatherData = weatherRepository.getCurrentWeather()
            } catch (e: Exception) {
                // Handle error if needed
            } finally {
                isWeatherLoading = false
            }
        } else {
            // Clear weather data when permission is revoked
            weatherData = null
            isWeatherLoading = false
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE5FFF4))
            .verticalScroll(rememberScrollState())
    ) {
        // Top section with car info and icons
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            // Car image
            Image(
                painter = painterResource(id = R.drawable.porsche_hero),
                contentDescription = "Porsche Hero",
                modifier = Modifier
                    .padding(top = 100.dp)
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Crop
            )
            // Overlayed info
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 32.dp)
            ) {
                Text("Porsche Taycan Turbo S", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with battery icon if available
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("150 km · Parked", color = Color.Gray, fontSize = 16.sp)
                }
            }
            // Weather and notifications icons (top right)
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 32.dp, end = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp), 
                    color = Color(0xFF232323).copy(alpha = 0.7f),
                    modifier = Modifier.clickable { 
                        if (hasLocationPermission) {
                            // Force refresh weather data (bypass cache)
                            coroutineScope.launch {
                                isWeatherLoading = true
                                try {
                                    weatherData = weatherRepository.forceRefreshWeather()
                                } catch (e: Exception) {
                                    // Handle error if needed
                                } finally {
                                    isWeatherLoading = false
                                }
                            }
                        } else {
                            // Request location permission
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isWeatherLoading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            Text(
                                if (hasLocationPermission && weatherData != null) {
                                    "${weatherData?.temperature?.toInt()}°"
                                } else {
                                    "N/A"
                                }, 
                                color = Color.White, 
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with weather icon if available
                            contentDescription = if (hasLocationPermission) "Tap to refresh weather" else "Tap to enable location",
                            tint = Color.Yellow,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Surface(
                    shape = CircleShape, color = Color(0xFF232323).copy(alpha = 0.7f)
                ) {
                    IconButton(onClick = { navController.navigate("notifications") }) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            // Lock icon floating near the car image (upper right)
            Surface(
                shape = CircleShape,
                color = Color(0xFF232323).copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 120.dp, end = 32.dp)
                    .clickable { isLocked = !isLocked }
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = if (isLocked) "Unlock car" else "Lock car",
                    tint = Color.White,
                    modifier = Modifier.padding(16.dp).size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        // Tab row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val tabs = listOf("Status", "Safety", "Location")
            tabs.forEachIndexed { i, tab ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        tab,
                        color = if (i == selectedTab) Color.Black else Color.Gray,
                        fontWeight = if (i == selectedTab) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 18.sp
                    )
                    if (i == selectedTab) Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.Black)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Info cards
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp), // Set fixed height for both cards
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DashboardCard(
                    title = "Battery",
                    subtitle = "Last charge 2w ago",
                    content = {
                        Column {
                            Text("212 km", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                            Text("85%  117kw", color = Color.Gray, fontSize = 13.sp)
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with battery icon if available
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 180.dp)
                        .fillMaxHeight() // Make card fill the Row's height
                )
                DashboardCard(
                    title = "Climate",
                    subtitle = "Interior 27°",
                    content = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { }) {
                                Text("+", color = Color.Black, fontSize = 18.sp)
                            }
                            Text("20°", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.Black)
                            IconButton(onClick = { }) {
                                Text("-", color = Color.Black, fontSize = 18.sp)
                            }
                        }
                        Text("Cooling", color = Color(0xFF60A5FA), fontSize = 13.sp)
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with fan icon if available
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 180.dp)
                        .fillMaxHeight() // Make card fill the Row's height
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            // ETA Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = WidgetBackground,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Screen.Search.route) }
            ) {
                Row(
                    modifier = Modifier
                        .padding(18.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            routeInfo?.eta ?: "9 min", 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 22.sp, 
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "9:50 ETA · ${routeInfo?.distance ?: "1.8 mi"}", 
                            color = Color.Gray, 
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with route icon if available
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                routeInfo?.destination ?: "Your preferred route", 
                                color = Color.Gray, 
                                fontSize = 15.sp
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    ) {
                        Text(
                            "48",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 32.sp
                        )
                        Text(
                            "km/h",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            // Playing now card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFB8F9DE),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Playing now",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Album art centered at top
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF232323),
                            modifier = Modifier.size(80.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize())
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Song info centered
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Virtual Riot",
                                color = Color.Black,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Seamless (feat. Kevin)",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // Media controls centered
                            Row(
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(
                                    onClick = { /* Previous track */ },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipPrevious,
                                        contentDescription = "Previous",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                
                                IconButton(
                                    onClick = { isPlaying = !isPlaying },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = Color.Black,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                IconButton(
                                    onClick = { /* Next track */ },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipNext,
                                        contentDescription = "Next",
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Progress slider
                    Slider(
                        value = currentProgress,
                        onValueChange = { currentProgress = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.Black,
                            activeTrackColor = Color.Black,
                            inactiveTrackColor = Color.Gray.copy(alpha = 0.3f)
                        )
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "1:23",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                        Text(
                            "3:45",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFFB8F9DE),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(4.dp))
                content()
            }
        }
    }
} 