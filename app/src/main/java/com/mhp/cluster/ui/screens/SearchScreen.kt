package com.mhp.cluster.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mhp.cluster.data.repository.NavigationRepository
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun SearchScreen(navController: NavController) {
    val context = LocalContext.current
    val navigationRepository = remember { NavigationRepository(context) }
    val coroutineScope = rememberCoroutineScope()
    
    var destination by remember { mutableStateOf("") }
    var isNavigating by remember { mutableStateOf(false) }
    var eta by remember { mutableStateOf("") }
    var distance by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    
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
                        "Using GPS location",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = destination,
            onValueChange = { destination = it },
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
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (destination.isNotEmpty()) {
                    isLoading = true
                    coroutineScope.launch {
                        try {
                            val routeInfo = navigationRepository.getRouteToDestination(destination)
                            if (routeInfo != null) {
                                eta = routeInfo.eta
                                distance = routeInfo.distance
                                isNavigating = true
                                navController.navigate("home") {
                                    popUpTo("home") { inclusive = true }
                                }
                            }
                        } catch (e: Exception) {
                            // Fallback to demo data
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
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            ),
            enabled = destination.isNotEmpty() && !isLoading
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
                if (isLoading) "Calculating Route..." else "Start Navigation", 
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