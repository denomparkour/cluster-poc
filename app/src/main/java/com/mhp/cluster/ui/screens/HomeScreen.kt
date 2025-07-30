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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import com.mhp.cluster.data.repository.StocksRepository
import com.mhp.cluster.data.model.Stock
import com.mhp.cluster.ui.components.StocksWidget
import com.mhp.cluster.ui.components.StockSelectionDialog
import com.mhp.cluster.ui.components.MilestoneWidget
import com.mhp.cluster.ui.components.LocationMapWidget
import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.filled.Battery2Bar
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import coil.compose.AsyncImage
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import org.osmdroid.util.GeoPoint
import kotlinx.coroutines.delay

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    val scrollState = rememberScrollState()
    var isLocked by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val weatherRepository = remember { WeatherRepository.getInstance(context) }
    val navigationRepository = remember { NavigationRepository(context) }
    val locationService = remember { LocationService.getInstance(context) }
    val stocksRepository = remember { StocksRepository.getInstance(context) }
    var weatherData by remember { mutableStateOf<WeatherRepository.WeatherData?>(null) }
    var routeInfo by remember { mutableStateOf<NavigationRepository.RouteInfo?>(null) }
    var stocks by remember { mutableStateOf<List<Stock>>(emptyList()) }
    var isPlaying by remember { mutableStateOf(true) }
    var currentProgress by remember { mutableStateOf(0.3f) }
    var isWeatherLoading by remember { mutableStateOf(false) }
    var isStocksLoading by remember { mutableStateOf(false) }
    var hasCurrentJourney by remember { mutableStateOf(false) }
    var isUpdatingRoute by remember { mutableStateOf(false) }
    var showStockSelectionDialog by remember { mutableStateOf(false) }
    val animatedSpeed = remember { Animatable(0f) }
    val vehicleStatus = remember { mutableStateOf("Parked") }
    val currentWeather = remember { mutableIntStateOf(30) }
    val coroutineScope = rememberCoroutineScope()

    // Function to stop current journey
    fun stopCurrentJourney() {
        navigationRepository.clearCurrentRoute()
        hasCurrentJourney = false
        routeInfo = null
        isUpdatingRoute = false
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val locationGranted = permissions.values.all { it }
        if (locationGranted) {
            coroutineScope.launch {
                isWeatherLoading = true
                try {
                    weatherData = weatherRepository.forceRefreshWeather()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isWeatherLoading = false
                }
            }
        }
    }

        val hasLocationPermission by remember {
        derivedStateOf { locationService.hasLocationPermission() }
    }
    
    // Function to load stocks with better error handling
    fun loadStocks() {
        isStocksLoading = true
        coroutineScope.launch {
            try {
                val loadedStocks = stocksRepository.getSelectedStocks()
                stocks = loadedStocks
                println("Loaded ${loadedStocks.size} stocks: ${loadedStocks.map { it.symbol }}")
            } catch (e: Exception) {
                e.printStackTrace()
                // Fallback to mock data if API fails
                stocks = listOf(
                    Stock("AAPL", "Apple Inc.", 175.43, 2.15, 1.24, 1234567890, 2800000000000, 28.5, 0.92, 0.52),
                    Stock("GOOGL", "Alphabet Inc.", 142.56, -1.23, -0.85, 987654321, 1800000000000, 25.2, 0.00, 0.00),
                    Stock("MSFT", "Microsoft Corporation", 378.85, 5.67, 1.52, 456789123, 2800000000000, 35.8, 3.00, 0.79)
                )
                println("Using fallback stocks: ${stocks.map { it.symbol }}")
            } finally {
                isStocksLoading = false
            }
        }
    }
    
    // Check for current journey and update ETA in real-time
    LaunchedEffect(Unit) {
        val currentDestination = navigationRepository.getCurrentDestination()
        if (currentDestination != null) {
            hasCurrentJourney = true
            // Get cached route info
            val cachedRoute = navigationRepository.getCachedRouteInfo()
            if (cachedRoute != null) {
                routeInfo = cachedRoute
            } else {
                // Fallback to demo data
                routeInfo = NavigationRepository.RouteInfo(
                    destination = currentDestination,
                    eta = "9 min",
                    distance = "1.8 mi",
                    duration = 540,
                    polyline = emptyList(),
                    startLocation = GeoPoint(40.7128, -74.0060),
                    endLocation = GeoPoint(40.7589, -73.9851)
                )
            }

            // Try to get real-time update immediately
            coroutineScope.launch {
                try {
                    isUpdatingRoute = true
                    val currentLocation = locationService.getCurrentLocation()
                    if (currentLocation != null) {
                        val updatedRoute = navigationRepository.updateRouteWithCurrentLocation(
                            currentLocation.latitude,
                            currentLocation.longitude
                        )
                        if (updatedRoute != null) {
                            routeInfo = updatedRoute
                        }
                    }
                } catch (e: Exception) {
                    // Ignore errors for initial update
                } finally {
                    isUpdatingRoute = false
                }
            }
        } else {
            hasCurrentJourney = false
            routeInfo = null
        }

        if (hasLocationPermission) {
            isWeatherLoading = true
            coroutineScope.launch {
                try {
                    weatherData = weatherRepository.getCurrentWeather()
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isWeatherLoading = false
                }
            }
        }

        // Load stocks data
        loadStocks()
    }
    
    // Real-time ETA updates
    LaunchedEffect(hasCurrentJourney) {
        if (hasCurrentJourney) {
            while (true) {
                delay(30000) // Update every 30 seconds
                val currentDestination = navigationRepository.getCurrentDestination()
                if (currentDestination != null) {
                    // Get current location and update route
                    val currentLocation = locationService.getCurrentLocation()
                    if (currentLocation != null) {
                        try {
                            val updatedRoute = navigationRepository.updateRouteWithCurrentLocation(
                                currentLocation.latitude,
                                currentLocation.longitude
                            )
                            if (updatedRoute != null) {
                                routeInfo = updatedRoute
                            }
                        } catch (e: Exception) {
                            // Fallback to cached route
                            val cachedRoute = navigationRepository.getCachedRouteInfo()
                            if (cachedRoute != null) {
                                routeInfo = cachedRoute
                            }
                        }
                    } else {
                        // Fallback to cached route if location not available
                        val cachedRoute = navigationRepository.getCachedRouteInfo()
                        if (cachedRoute != null) {
                            routeInfo = cachedRoute
                        }
                    }
                } else {
                    hasCurrentJourney = false
                    routeInfo = null
                    break
                }
            }
        }
    }
    
    // More frequent location updates for active journeys (every 10 seconds)
    LaunchedEffect(hasCurrentJourney) {
        if (hasCurrentJourney) {
            while (true) {
                delay(10000) // Update every 10 seconds for more responsive updates
                val currentDestination = navigationRepository.getCurrentDestination()
                if (currentDestination != null) {
                    val currentLocation = locationService.getCurrentLocation()
                    if (currentLocation != null) {
                        try {
                            isUpdatingRoute = true
                            val updatedRoute = navigationRepository.updateRouteWithCurrentLocation(
                                currentLocation.latitude,
                                currentLocation.longitude
                            )
                            if (updatedRoute != null) {
                                routeInfo = updatedRoute
                            }
                        } catch (e: Exception) {
                            // Silently fail for frequent updates
                        } finally {
                            isUpdatingRoute = false
                        }
                    }
                } else {
                    hasCurrentJourney = false
                    routeInfo = null
                    break
                }
            }
        }
    }
    
    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) {
            isWeatherLoading = true
            try {
                weatherData = weatherRepository.getCurrentWeather()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isWeatherLoading = false
            }
        } else {
            weatherData = null
            isWeatherLoading = false
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE5FFF4))
            .verticalScroll(scrollState)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.porsche_hero),
                contentDescription = "Porsche Hero",
                modifier = Modifier
                    .padding(top = 100.dp)
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 24.dp, top = 32.dp)
            ) {
                Text("Porsche Taycan Turbo S", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.Black)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        tint = Color.LightGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("150 km · ${vehicleStatus.value}", color = Color.Gray, fontSize = 16.sp)
                }
            }
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
                            coroutineScope.launch {
                                isWeatherLoading = true
                                try {
                                    weatherData = weatherRepository.forceRefreshWeather()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    isWeatherLoading = false
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
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
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
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tabs = listOf("Status", "Widgets", "Location")
            tabs.forEachIndexed { i, tab ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable {
                        selectedTab = i
                        coroutineScope.launch {
                            when (i) {
                                0 -> scrollState.animateScrollTo(0) // Status section
                                1 -> scrollState.animateScrollTo(400) // Widgets section
                                2 -> scrollState.animateScrollTo(800) // Location section
                            }
                        }
                    }
                ) {
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
        
        // Status Section (Tab 0)
        if (selectedTab == 0) {
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
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
                            imageVector = Icons.Filled.Battery2Bar,
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(35.dp)
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 180.dp)
                        .fillMaxHeight()
                )
                DashboardCard(
                    title = "Climate",
                    subtitle = "Interior 27°",
                    icon = {
                        Icon(
                            imageVector = Icons.Default.Cloud,
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(28.dp)
                        )
                    },
                    content = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(top = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(onClick = {
                                    if(currentWeather.intValue < 31) {
                                        currentWeather.intValue += 1
                                    }
                                }) {
                                    Text("+", color = Color.Black, fontSize = 18.sp)
                                }

                                Text(
                                    text = "${currentWeather.intValue}°",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = Color.Black
                                )

                                IconButton(onClick = {
                                    if(currentWeather.intValue > 16) {
                                        currentWeather.intValue -= 1
                                    }
                                })
                                {
                                    Text("-", color = Color.Black, fontSize = 18.sp)
                                }
                            }

                            Text(
                                text = "Cooling",
                                color = Color(0xFF60A5FA),
                                fontSize = 13.sp,
                            )
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(max = 180.dp)
                        .fillMaxHeight()
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (hasCurrentJourney && routeInfo != null) {

                vehicleStatus.value = "Driving"
                LaunchedEffect(key1 = hasCurrentJourney) {
                    if (hasCurrentJourney) {
                        val targetSpeeds = listOf(10f, 20f, 30f, 48f)
                        for (speed in targetSpeeds) {
                            animatedSpeed.animateTo(
                                targetValue = speed,
                                animationSpec = tween(durationMillis = 700)
                            )
                            delay(500L)
                        }

                        while (hasCurrentJourney) {
                            val fluctuation = (40..55).random().toFloat()
                            animatedSpeed.animateTo(
                                targetValue = fluctuation,
                                animationSpec = tween(durationMillis = 500)
                            )
                            delay(800L)
                        }
                    } else {
                        animatedSpeed.snapTo(0f)
                    }
                }


                val etaRaw = routeInfo?.eta ?: ""
                val etaHours = Regex("(\\d+)h").find(etaRaw)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val etaMinutes = Regex("(\\d+)\\s*min").find(etaRaw)?.groupValues?.get(1)?.toIntOrNull() ?: 0

                val arrivalTime = LocalTime.now()
                    .plusHours(etaHours.toLong())
                    .plusMinutes(etaMinutes.toLong())
                    .format(DateTimeFormatter.ofPattern("h:mm a"))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = WidgetBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Screen.Search.route) }
                ) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .padding(18.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    routeInfo?.eta ?: "9 min",
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 22.sp, 
                                    color = Color.Black
                                )
                                if (isUpdatingRoute) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    CircularProgressIndicator(
                                        color = Color.Gray,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "${arrivalTime ?: "9:50 AM"} ETA · ${routeInfo?.distance ?: "1.8 mi"}",
                                color = Color.Gray,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_launcher_foreground),
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
                                    animatedSpeed.value.toInt().toString(),
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
                        
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 8.dp, end = 8.dp)
                                .size(24.dp)
                                .clickable { stopCurrentJourney() }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Stop journey",
                                tint = Color.White,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                    }
                }
            } else {
                vehicleStatus.value = "Parked"
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
                                "No Current Journey",
                                fontWeight = FontWeight.Bold, 
                                fontSize = 18.sp, 
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "Tap to start navigation", 
                                color = Color.Gray, 
                                fontSize = 14.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
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
                            "Now Playing",
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
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF232323),
                            modifier = Modifier.size(180.dp)
                        ) {
                            AsyncImage(
                                model = "https://upload.wikimedia.org/wikipedia/en/thumb/6/6a/UltraviolenceLDR.png/250px-UltraviolenceLDR.png",
                                contentDescription = "Lana Del Rey Album",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "West Coast",
                                color = Color.Black,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Lana Del Rey",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
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
                            "4:17",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        }
        
        if (selectedTab == 1) {
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Widgets",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Stocks Widget
                StocksWidget(
                    stocks = stocks,
                    isLoading = isStocksLoading,
                    onStocksClick = { showStockSelectionDialog = true },
                    onRefresh = { loadStocks() },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Milestone Widget
                MilestoneWidget(
                    milestone = "2500",
                    unit = "Kms",
                    title = "Milestone Reached",
                    emoji = "🏆",
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        if (selectedTab == 2) {
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    "Location",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Location Map Widget
                LocationMapWidget(
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
    
    // Stock Selection Dialog
    if (showStockSelectionDialog) {
        StockSelectionDialog(
            onDismiss = { showStockSelectionDialog = false },
            onStocksSelected = { selectedSymbols ->
                stocksRepository.saveSelectedStocks(selectedSymbols)
                println("Stocks selected: $selectedSymbols")
                // Reload stocks after selection
                loadStocks()
            },
            searchStocks = { query -> stocksRepository.searchStocks(query) },
            getSelectedStocks = { stocksRepository.getSelectedStockSymbols() },
            addStock = { symbol -> stocksRepository.addStock(symbol) },
            removeStock = { symbol -> stocksRepository.removeStock(symbol) }
        )
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