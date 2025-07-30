package com.mhp.cluster.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhp.cluster.data.repository.NotificationRepository
import com.mhp.cluster.ui.theme.WidgetBackground

@Composable
fun NotificationsScreen() {
    val context = LocalContext.current
    val notificationRepository = remember { NotificationRepository(context) }
    
    var notifications by remember { mutableStateOf(emptyList<NotificationItem>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (notificationRepository.isFirstLaunch()) {
            val defaultNotifications = notificationRepository.getDefaultNotifications()
            notificationRepository.saveNotifications(defaultNotifications)
            notificationRepository.setFirstLaunchComplete()
            notifications = defaultNotifications
        } else {
            notifications = notificationRepository.getNotifications()
        }
    }
    
    Surface(color = Color(0xFFE5FFF4), modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Notifications", 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 24.sp
                )










            }

            if (notifications.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No notifications",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(notifications) { notification ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = WidgetBackground),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(notification.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    Text(notification.timestamp, color = Color.Gray, fontSize = 14.sp)
                                }
                                
                                IconButton(
                                    onClick = {
                                        val updatedNotifications = notifications.filter { it != notification }
                                        notificationRepository.saveNotifications(updatedNotifications)
                                        notifications = updatedNotifications
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Clear,
                                        contentDescription = "Delete notification",
                                        tint = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete All Notifications") },
            text = { Text("Are you sure you want to delete all notifications? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        notificationRepository.clearNotifications()
                        notifications = emptyList()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFF6B6B)
                    )
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

data class NotificationItem(val title: String, val timestamp: String) 