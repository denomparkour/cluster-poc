package com.mhp.cluster.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mhp.cluster.data.model.Stock
import com.mhp.cluster.data.model.StockSearchResult
import com.mhp.cluster.ui.theme.WidgetBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StockSelectionDialog(
    onDismiss: () -> Unit,
    onStocksSelected: (List<String>) -> Unit,
    searchStocks: suspend (String) -> List<StockSearchResult>,
    getSelectedStocks: () -> List<String>,
    addStock: (String) -> Unit,
    removeStock: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<StockSearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var selectedStocks by remember { mutableStateOf(getSelectedStocks()) }
    
    val coroutineScope = rememberCoroutineScope()
    
    // Debounced search
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            delay(300) // Debounce for 300ms
            isSearching = true
            try {
                searchResults = searchStocks(searchQuery)
            } catch (e: Exception) {
                searchResults = emptyList()
            } finally {
                isSearching = false
            }
        } else {
            searchResults = emptyList()
        }
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Select Stocks",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.Black
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color.Gray
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Search bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search stocks...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Black,
                        unfocusedBorderColor = Color.Gray
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Selected stocks section
                if (selectedStocks.isNotEmpty()) {
                    Text(
                        "Selected Stocks (${selectedStocks.size}/5)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(selectedStocks) { symbol ->
                            SelectedStockChip(
                                symbol = symbol,
                                onRemove = {
                                    removeStock(symbol)
                                    selectedStocks = selectedStocks.filter { it != symbol }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Search results
                if (searchQuery.isNotEmpty()) {
                    Text(
                        "Search Results",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    if (isSearching) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.Gray,
                                strokeWidth = 2.dp
                            )
                        }
                    } else if (searchResults.isEmpty()) {
                        Text(
                            "No stocks found",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(searchResults) { result ->
                                StockSearchItem(
                                    stock = result,
                                    isSelected = selectedStocks.contains(result.symbol),
                                    onSelect = {
                                        if (selectedStocks.size < 5 && !selectedStocks.contains(result.symbol)) {
                                            addStock(result.symbol)
                                            selectedStocks = selectedStocks + result.symbol
                                        }
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Popular stocks suggestions
                    Text(
                        "Popular Stocks",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val popularStocks = listOf(
                        StockSearchResult("AAPL", "Apple Inc.", "Common Stock", "US", "09:30", "16:00", "UTC-05", "USD", 1.0),
                        StockSearchResult("GOOGL", "Alphabet Inc.", "Common Stock", "US", "09:30", "16:00", "UTC-05", "USD", 1.0),
                        StockSearchResult("MSFT", "Microsoft Corporation", "Common Stock", "US", "09:30", "16:00", "UTC-05", "USD", 1.0),
                        StockSearchResult("AMZN", "Amazon.com Inc.", "Common Stock", "US", "09:30", "16:00", "UTC-05", "USD", 1.0),
                        StockSearchResult("TSLA", "Tesla Inc.", "Common Stock", "US", "09:30", "16:00", "UTC-05", "USD", 1.0)
                    )
                    
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(popularStocks) { result ->
                            StockSearchItem(
                                stock = result,
                                isSelected = selectedStocks.contains(result.symbol),
                                onSelect = {
                                    if (selectedStocks.size < 5 && !selectedStocks.contains(result.symbol)) {
                                        addStock(result.symbol)
                                        selectedStocks = selectedStocks + result.symbol
                                    }
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Save button
                Button(
                    onClick = {
                        onStocksSelected(selectedStocks)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Black
                    )
                ) {
                    Text(
                        "Save Selection",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun StockSearchItem(
    stock: StockSearchResult,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) Color(0xFFE5FFF4) else Color.White,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stock.symbol,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.Black
                )
                Text(
                    stock.name,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Selected",
                    tint = Color(0xFF4ADE80),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SelectedStockChip(
    symbol: String,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFE5FFF4),
        modifier = Modifier.clickable { onRemove() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                symbol,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.Black
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.Close,
                contentDescription = "Remove",
                tint = Color.Gray,
                modifier = Modifier.size(16.dp)
            )
        }
    }
} 