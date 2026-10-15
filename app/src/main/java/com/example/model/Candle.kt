package com.example.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Candle(
    val timestamp: Long,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Double
) {
    val isBullish: Boolean get() = close >= open
    val change: Double get() = close - open
    val changePercent: Double get() = if (open != 0.0) ((close - open) / open) * 100.0 else 0.0

    fun formattedTime(timeframe: Timeframe): String {
        val pattern = when (timeframe) {
            Timeframe.M1, Timeframe.M5, Timeframe.M15 -> "HH:mm"
            Timeframe.H1, Timeframe.H4 -> "MMM dd HH:mm"
            Timeframe.D1, Timeframe.W1 -> "yyyy-MM-dd"
        }
        val sdf = SimpleDateFormat(pattern, Locale.US)
        return sdf.format(Date(timestamp))
    }
}

enum class Timeframe(val label: String, val intervalSeconds: Long, val defaultCandleCount: Int) {
    M1("1m", 60, 60),
    M5("5m", 300, 60),
    M15("15m", 900, 60),
    H1("1h", 3600, 50),
    H4("4h", 14400, 45),
    D1("1D", 86400, 40),
    W1("1W", 604800, 35)
}

enum class ChartStyle(val label: String) {
    CANDLES("Candles"),
    LINE("Line"),
    AREA("Area")
}
