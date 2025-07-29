package com.mhp.cluster.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
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

@Composable
fun HomeScreen() {
    val selectedTab = remember { 0 }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top section with car info and icons
        Box(modifier = Modifier.fillMaxWidth()) {
            // Car image
            Image(
                painter = painterResource(id = R.drawable.porsche_hero),
                contentDescription = "Porsche Hero",
                modifier = Modifier
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
                Text("Porsche Taycan Turbo S", fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color.Black)
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
            // Weather and profile icons
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
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(8.dp).size(22.dp)
                    )
                }
            }
            // Lock icon (centered on car)
            Surface(
                shape = CircleShape,
                color = Color(0xFF232323).copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 32.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(16.dp).size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        // Tab row
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val tabs = listOf("Status", "Climate", "Battery", "Safety", "Location")
            tabs.forEachIndexed { i, tab ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        tab,
                        color = if (i == selectedTab) Color.White else Color.LightGray,
                        fontWeight = if (i == selectedTab) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 18.sp
                    )
                    if (i == selectedTab) Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        // Info cards
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardCard(
                    title = "Battery",
                    subtitle = "Last charge 2w ago",
                    content = {
                        Column {
                            Text("212 km", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White)
                            Text("85%  117kw", color = Color.LightGray, fontSize = 16.sp)
                        }
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with battery icon if available
                            contentDescription = null,
                            tint = Color(0xFF4ADE80),
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
                DashboardCard(
                    title = "Climate",
                    subtitle = "Interior 27°",
                    content = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { }) {
                                Text("+", color = Color.White, fontSize = 22.sp)
                            }
                            Text("20°", fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White)
                            IconButton(onClick = { }) {
                                Text("-", color = Color.White, fontSize = 22.sp)
                            }
                        }
                        Text("Cooling", color = Color(0xFF60A5FA), fontSize = 16.sp)
                    },
                    icon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with fan icon if available
                            contentDescription = null,
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            DashboardCard(
                title = "Playing now",
                subtitle = "Seamless (feat. Kevin)",
                content = {
                    Text("Virtual Riot", color = Color.White, fontSize = 18.sp)
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
        color = Color(0xFF232323).copy(alpha = 0.85f),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(subtitle, color = Color.LightGray, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                content()
            }
        }
    }
} 