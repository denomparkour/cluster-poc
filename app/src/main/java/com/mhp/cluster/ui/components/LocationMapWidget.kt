package com.mhp.cluster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mhp.cluster.ui.theme.WidgetBackground
import kotlin.random.Random
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@Composable
fun LocationMapWidget(
    modifier: Modifier = Modifier
) {
    val randomLocation = remember {
        getRandomLocation()
    }
    
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = WidgetBackground,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Last Known Location",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.Black
                )
                Row() {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(
                        imageVector = Icons.Filled.MyLocation,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Real Map
            Surface(
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                AndroidView(
                    factory = { context ->
                        MapView(context).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            isTilesScaledToDpi = true
                            
                            // Set the map center to the random location
                            controller.setCenter(randomLocation.coordinates)
                            controller.setZoom(15.0)
                            
                            // Add a marker for the location
                            val marker = Marker(this).apply {
                                position = randomLocation.coordinates
                                title = randomLocation.name
                                snippet = randomLocation.address
                                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            }
                            overlays.add(marker)
                            invalidate()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Location details
            Column {
                Text(
                    text = randomLocation.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = randomLocation.address,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Last located here • ${randomLocation.time}",
                        fontSize = 12.sp,
                        color = Color(0xFF059669),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

private fun getRandomLocation(): LocationData {
    val locations = listOf(
        LocationData(
            name = "Central Park",
            address = "Manhattan, New York, NY 10024",
            time = "2 hours ago",
            coordinates = GeoPoint(40.7829, -73.9654)
        ),
        LocationData(
            name = "Times Square",
            address = "Manhattan, New York, NY 10036",
            time = "1 hour ago",
            coordinates = GeoPoint(40.7580, -73.9855)
        ),
        LocationData(
            name = "Brooklyn Bridge",
            address = "Brooklyn, New York, NY 11201",
            time = "30 minutes ago",
            coordinates = GeoPoint(40.7061, -73.9969)
        ),
        LocationData(
            name = "Empire State Building",
            address = "Manhattan, New York, NY 10001",
            time = "45 minutes ago",
            coordinates = GeoPoint(40.7484, -73.9857)
        ),
        LocationData(
            name = "Statue of Liberty",
            address = "Liberty Island, New York, NY 10004",
            time = "1.5 hours ago",
            coordinates = GeoPoint(40.6892, -74.0445)
        ),
        LocationData(
            name = "High Line Park",
            address = "Manhattan, New York, NY 10011",
            time = "20 minutes ago",
            coordinates = GeoPoint(40.7480, -74.0048)
        ),
        LocationData(
            name = "Metropolitan Museum",
            address = "Manhattan, New York, NY 10028",
            time = "3 hours ago",
            coordinates = GeoPoint(40.7794, -73.9632)
        ),
        LocationData(
            name = "Rockefeller Center",
            address = "Manhattan, New York, NY 10020",
            time = "1 hour ago",
            coordinates = GeoPoint(40.7587, -73.9787)
        )
    )
    
    return locations[Random.nextInt(locations.size)]
}

data class LocationData(
    val name: String,
    val address: String,
    val time: String,
    val coordinates: GeoPoint
) 