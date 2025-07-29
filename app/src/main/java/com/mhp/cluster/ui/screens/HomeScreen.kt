package com.mhp.cluster.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
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

@Composable
fun HomeScreen(navController: NavController) {
    val selectedTab = remember { 0 }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE5FFF4))
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
                    shape = RoundedCornerShape(16.dp), color = Color(0xFF232323).copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("32°", color = Color.White, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with weather icon if available
                            contentDescription = null,
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
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
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
                        Text("9 min", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("9:50 ETA · 1.8 mi", color = Color.Gray, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with route icon if available
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Your preferred route", color = Color.Gray, fontSize = 15.sp)
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
            DashboardCard(
                title = "Playing now",
                subtitle = "Seamless (feat. Kevin)",
                content = {
                    Text("Virtual Riot", color = Color.Black, fontSize = 18.sp)
                },
                icon = {
                    Surface(
                        shape = RoundedCornerShape(8.dp), color = Color(0xFF232323)
                    ) {
                        Box(modifier = Modifier.size(36.dp))
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
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