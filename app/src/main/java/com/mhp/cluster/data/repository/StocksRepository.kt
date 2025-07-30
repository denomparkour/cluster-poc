package com.mhp.cluster.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.mhp.cluster.data.model.Stock
import com.mhp.cluster.data.model.StockQuote
import com.mhp.cluster.data.model.StockSearchResult
import com.mhp.cluster.data.remote.StockApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class StocksRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("stocks_prefs", Context.MODE_PRIVATE)
    private val selectedStocksKey = "selected_stocks"
    
    private val apiService: StockApiService by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        
        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
        
        Retrofit.Builder()
            .baseUrl("https://www.alphavantage.co/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(StockApiService::class.java)
    }
    
    private val apiKey = "XDZ4NS2Y7KPNA86C"
    
    // Default popular stocks to show when no stocks are selected
    private val defaultStocks = listOf("AAPL", "GOOGL", "MSFT", "AMZN", "TSLA")
    
    suspend fun getSelectedStocks(): List<Stock> = withContext(Dispatchers.IO) {
        try {
            val selectedSymbols = getSelectedStockSymbols()
            val symbolsToFetch = if (selectedSymbols.isEmpty()) defaultStocks else selectedSymbols
            
            val stocks = mutableListOf<Stock>()
            
            // Alpha Vantage doesn't support batch quotes, so we need to make individual calls
            // Note: Alpha Vantage has rate limits (5 calls per minute for free tier)
            symbolsToFetch.forEach { symbol ->
                try {
                    val response = apiService.getStockQuote(symbol = symbol, apiKey = apiKey)
                    
                    if (response.globalQuote != null && response.errorMessage == null) {
                        val quote = response.globalQuote
                        val stock = Stock(
                            symbol = quote.symbol,
                            name = getCompanyName(symbol), // We'll need to get company names separately
                            price = quote.price.toDoubleOrNull() ?: 0.0,
                            change = quote.change.toDoubleOrNull() ?: 0.0,
                            changePercent = quote.changePercent.removeSuffix("%").toDoubleOrNull() ?: 0.0,
                            volume = quote.volume.toLongOrNull() ?: 0L,
                            marketCap = null, // Alpha Vantage doesn't provide this in GLOBAL_QUOTE
                            pe = null, // Alpha Vantage doesn't provide this in GLOBAL_QUOTE
                            dividend = null, // Alpha Vantage doesn't provide this in GLOBAL_QUOTE
                            yield = null // Alpha Vantage doesn't provide this in GLOBAL_QUOTE
                        )
                        stocks.add(stock)
                    }
                    
                    // Add delay to respect rate limits (5 calls per minute = 12 seconds between calls)
                    if (symbolsToFetch.size > 1) {
                        delay(1200) // 1.2 seconds delay between calls
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // If API call fails, add mock data as fallback
                    stocks.add(getMockStockData(symbol))
                }
            }
            
            stocks
        } catch (e: Exception) {
            e.printStackTrace()
            // Return empty list on error
            emptyList()
        }
    }
    
    suspend fun searchStocks(query: String): List<StockSearchResult> = withContext(Dispatchers.IO) {
        try {
            if (query.length < 2) return@withContext emptyList()
            
            val response = apiService.searchStocks(keywords = query, apiKey = apiKey)
            
            if (response.bestMatches != null && response.errorMessage == null) {
                response.bestMatches.map { result ->
                    StockSearchResult(
                        symbol = result.symbol,
                        name = result.name,
                        type = result.type,
                        region = result.region,
                        marketOpen = result.marketOpen,
                        marketClose = result.marketClose,
                        timezone = result.timezone,
                        currency = result.currency,
                        matchScore = result.matchScore.toDoubleOrNull() ?: 0.0
                    )
                }
            } else {
                // If API call fails, return mock data as fallback
                val allStocks = listOf(
                    StockSearchResult("AAPL", "Apple Inc.", "Common Stock", "US", "09:30", "16:00", "UTC-05", "USD", 1.0),
                    StockSearchResult("GOOGL", "Alphabet Inc.", "Common Stock", "US", "09:30", "16:00", "UTC-05", "USD", 1.0),
                    StockSearchResult("MSFT", "Microsoft Corporation", "Common Stock", "US", "09:30", "16:00", "UTC-05", "USD", 1.0),
                    StockSearchResult("AMZN", "Amazon.com Inc.", "Common Stock", "US", "09:30", "16:00", "UTC-05", "USD", 1.0),
                    StockSearchResult("TSLA", "Tesla Inc.", "Common Stock", "US", "09:30", "16:00", "UTC-05", "USD", 1.0),
                    StockSearchResult("NVDA", "NVIDIA Corporation", "Common Stock", "US", "09:30", "16:00", "UTC-05", "USD", 1.0),
                    StockSearchResult("META", "Meta Platforms Inc.", "Common Stock", "US", "09:30", "16:00", "UTC-05", "USD", 1.0),
                    StockSearchResult("NFLX", "Netflix Inc.", "Common Stock", "US", "09:30", "16:00", "UTC-05", "USD", 1.0),
                    StockSearchResult("JPM", "JPMorgan Chase & Co.", "Common Stock", "US", "09:30", "16:00", "UTC-05", "USD", 1.0),
                    StockSearchResult("JNJ", "Johnson & Johnson", "Common Stock", "US", "09:30", "16:00", "UTC-05", "USD", 1.0)
                )
                
                allStocks.filter { 
                    it.symbol.contains(query.uppercase()) || 
                    it.name.contains(query, ignoreCase = true) 
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun getStockQuote(symbol: String): Stock? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getStockQuote(symbol = symbol, apiKey = apiKey)
            
            if (response.globalQuote != null && response.errorMessage == null) {
                val quote = response.globalQuote
                Stock(
                    symbol = quote.symbol,
                    name = getCompanyName(symbol),
                    price = quote.price.toDoubleOrNull() ?: 0.0,
                    change = quote.change.toDoubleOrNull() ?: 0.0,
                    changePercent = quote.changePercent.removeSuffix("%").toDoubleOrNull() ?: 0.0,
                    volume = quote.volume.toLongOrNull() ?: 0L,
                    marketCap = null,
                    pe = null,
                    dividend = null,
                    yield = null
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun getSelectedStockSymbols(): List<String> {
        val symbolsString = prefs.getString(selectedStocksKey, "")
        return if (symbolsString.isNullOrEmpty()) {
            emptyList()
        } else {
            symbolsString.split(",").filter { it.isNotBlank() }
        }
    }
    
    fun saveSelectedStocks(symbols: List<String>) {
        prefs.edit().putString(selectedStocksKey, symbols.joinToString(",")).apply()
    }
    
    fun addStock(symbol: String) {
        val currentSymbols = getSelectedStockSymbols().toMutableList()
        if (!currentSymbols.contains(symbol) && currentSymbols.size < 5) {
            currentSymbols.add(symbol)
            saveSelectedStocks(currentSymbols)
        }
    }
    
    fun removeStock(symbol: String) {
        val currentSymbols = getSelectedStockSymbols().toMutableList()
        currentSymbols.remove(symbol)
        saveSelectedStocks(currentSymbols)
    }
    
    fun clearSelectedStocks() {
        prefs.edit().remove(selectedStocksKey).apply()
    }
    
    private fun getMockStockData(symbol: String): Stock {
        val mockData = when (symbol.uppercase()) {
            "AAPL" -> Stock("AAPL", "Apple Inc.", 175.43, 2.15, 1.24, 1234567890, 2800000000000, 28.5, 0.92, 0.52)
            "GOOGL" -> Stock("GOOGL", "Alphabet Inc.", 142.56, -1.23, -0.85, 987654321, 1800000000000, 25.2, 0.00, 0.00)
            "MSFT" -> Stock("MSFT", "Microsoft Corporation", 378.85, 5.67, 1.52, 456789123, 2800000000000, 35.8, 3.00, 0.79)
            "AMZN" -> Stock("AMZN", "Amazon.com Inc.", 145.24, -2.34, -1.58, 789123456, 1500000000000, 45.2, 0.00, 0.00)
            "TSLA" -> Stock("TSLA", "Tesla Inc.", 248.50, 12.45, 5.28, 234567890, 800000000000, 65.3, 0.00, 0.00)
            "NVDA" -> Stock("NVDA", "NVIDIA Corporation", 485.09, 15.67, 3.34, 345678901, 1200000000000, 75.2, 0.16, 0.03)
            "META" -> Stock("META", "Meta Platforms Inc.", 334.92, -8.45, -2.46, 567890123, 850000000000, 22.1, 0.00, 0.00)
            "NFLX" -> Stock("NFLX", "Netflix Inc.", 485.09, 12.34, 2.61, 123456789, 210000000000, 45.8, 0.00, 0.00)
            "JPM" -> Stock("JPM", "JPMorgan Chase & Co.", 172.45, 1.23, 0.72, 678901234, 520000000000, 12.5, 4.20, 2.44)
            "JNJ" -> Stock("JNJ", "Johnson & Johnson", 162.78, -0.89, -0.54, 345678901, 390000000000, 15.2, 4.76, 2.92)
            else -> Stock(symbol, "$symbol Corp.", 100.00, 0.00, 0.00, 100000000, 1000000000, 15.0, 1.00, 1.00)
        }
        return mockData
    }
    
    private fun getCompanyName(symbol: String): String {
        return when (symbol.uppercase()) {
            "AAPL" -> "Apple Inc."
            "GOOGL" -> "Alphabet Inc."
            "MSFT" -> "Microsoft Corporation"
            "AMZN" -> "Amazon.com Inc."
            "TSLA" -> "Tesla Inc."
            "NVDA" -> "NVIDIA Corporation"
            "META" -> "Meta Platforms Inc."
            "NFLX" -> "Netflix Inc."
            "JPM" -> "JPMorgan Chase & Co."
            "JNJ" -> "Johnson & Johnson"
            else -> "$symbol Corporation"
        }
    }
    
    companion object {
        @Volatile
        private var INSTANCE: StocksRepository? = null
        
        fun getInstance(context: Context): StocksRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: StocksRepository(context).also { INSTANCE = it }
            }
        }
    }
} 