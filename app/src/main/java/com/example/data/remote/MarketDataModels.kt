package com.example.data.remote

import com.example.model.Candle

sealed class ConnectionStatus {
    object Connected : ConnectionStatus()
    object Connecting : ConnectionStatus()
    data class Reconnecting(val attempt: Int, val delayMs: Long) : ConnectionStatus()
    object Disconnected : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()

    val label: String
        get() = when (this) {
            is Connected -> "LIVE"
            is Connecting -> "CONNECTING"
            is Reconnecting -> "RETRYING ($attempt)"
            is Disconnected -> "OFFLINE"
            is Error -> "ERROR"
        }
}

data class OrderBookEntry(
    val price: Double,
    val quantity: Double,
    val total: Double = price * quantity
)

data class OrderBookData(
    val symbol: String,
    val bids: List<OrderBookEntry>,
    val asks: List<OrderBookEntry>,
    val lastUpdateId: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val spread: Double
        get() {
            val bestAsk = asks.firstOrNull()?.price ?: return 0.0
            val bestBid = bids.firstOrNull()?.price ?: return 0.0
            return (bestAsk - bestBid).coerceAtLeast(0.0)
        }

    val spreadPercent: Double
        get() {
            val bestBid = bids.firstOrNull()?.price ?: return 0.0
            if (bestBid <= 0) return 0.0
            return (spread / bestBid) * 100.0
        }
}

data class Binance24hTicker(
    val symbol: String,
    val lastPrice: Double,
    val priceChange: Double,
    val priceChangePercent: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val volume: Double,
    val quoteVolume: Double
)

data class KlineStreamEvent(
    val symbol: String,
    val interval: String,
    val candle: Candle,
    val isClosed: Boolean,
    val eventTime: Long
)
