package com.mhp.cluster.data.remote

import com.mhp.cluster.data.model.StockQuote
import com.mhp.cluster.data.model.StockSearchResult
import com.mhp.cluster.data.model.AlphaVantageQuoteResponse
import com.mhp.cluster.data.model.AlphaVantageSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface StockApiService {
    @GET("query")
    suspend fun getStockQuote(
        @Query("function") function: String = "GLOBAL_QUOTE",
        @Query("symbol") symbol: String,
        @Query("apikey") apiKey: String
    ): AlphaVantageQuoteResponse
    
    @GET("query")
    suspend fun searchStocks(
        @Query("function") function: String = "SYMBOL_SEARCH",
        @Query("keywords") keywords: String,
        @Query("apikey") apiKey: String
    ): AlphaVantageSearchResponse
} 