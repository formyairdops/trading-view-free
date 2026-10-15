package com.example.data.remote

import com.example.model.Candle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MarketDataProvider {
    val name: String
    val connectionStatus: StateFlow<ConnectionStatus>
    val lastUpdateTimestamp: StateFlow<Long?>

    suspend fun fetchHistoricalCandles(
        symbol: String,
        interval: String,
        limit: Int = 300,
        startTime: Long? = null,
        endTime: Long? = null
    ): Result<List<Candle>>

    suspend fun fetch24hTicker(symbol: String): Result<Binance24hTicker>
    suspend fun fetchAll24hTickers(): Result<List<Binance24hTicker>>
    suspend fun fetchOrderBook(symbol: String, limit: Int = 20): Result<OrderBookData>

    fun startKlineStream(symbol: String, interval: String): Flow<KlineStreamEvent>
    fun stopKlineStream()
}
