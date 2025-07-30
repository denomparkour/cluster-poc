package com.mhp.cluster.data.model

import com.google.gson.annotations.SerializedName

data class Stock(
    val symbol: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long,
    val marketCap: Long? = null,
    val pe: Double? = null,
    val dividend: Double? = null,
    val yield: Double? = null
)

data class StockQuote(
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("companyName")
    val companyName: String,
    @SerializedName("latestPrice")
    val latestPrice: Double,
    @SerializedName("change")
    val change: Double,
    @SerializedName("changePercent")
    val changePercent: Double,
    @SerializedName("volume")
    val volume: Long,
    @SerializedName("marketCap")
    val marketCap: Long?,
    @SerializedName("peRatio")
    val peRatio: Double?,
    @SerializedName("dividend")
    val dividend: Double?,
    @SerializedName("yield")
    val yield: Double?
)

data class StockSearchResult(
    @SerializedName("symbol")
    val symbol: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("type")
    val type: String,
    @SerializedName("region")
    val region: String,
    @SerializedName("marketOpen")
    val marketOpen: String,
    @SerializedName("marketClose")
    val marketClose: String,
    @SerializedName("timezone")
    val timezone: String,
    @SerializedName("currency")
    val currency: String,
    @SerializedName("matchScore")
    val matchScore: Double
)

data class AlphaVantageQuoteResponse(
    @SerializedName("Global Quote")
    val globalQuote: AlphaVantageQuote?,
    @SerializedName("Note")
    val note: String? = null,
    @SerializedName("Error Message")
    val errorMessage: String? = null
)

data class AlphaVantageQuote(
    @SerializedName("01. symbol")
    val symbol: String,
    @SerializedName("02. open")
    val open: String,
    @SerializedName("03. high")
    val high: String,
    @SerializedName("04. low")
    val low: String,
    @SerializedName("05. price")
    val price: String,
    @SerializedName("06. volume")
    val volume: String,
    @SerializedName("07. latest trading day")
    val latestTradingDay: String,
    @SerializedName("08. previous close")
    val previousClose: String,
    @SerializedName("09. change")
    val change: String,
    @SerializedName("10. change percent")
    val changePercent: String
)

data class AlphaVantageSearchResponse(
    @SerializedName("bestMatches")
    val bestMatches: List<AlphaVantageSearchResult>?,
    @SerializedName("Note")
    val note: String? = null,
    @SerializedName("Error Message")
    val errorMessage: String? = null
)

data class AlphaVantageSearchResult(
    @SerializedName("1. symbol")
    val symbol: String,
    @SerializedName("2. name")
    val name: String,
    @SerializedName("3. type")
    val type: String,
    @SerializedName("4. region")
    val region: String,
    @SerializedName("5. marketOpen")
    val marketOpen: String,
    @SerializedName("6. marketClose")
    val marketClose: String,
    @SerializedName("7. timezone")
    val timezone: String,
    @SerializedName("8. currency")
    val currency: String,
    @SerializedName("9. matchScore")
    val matchScore: String
) 