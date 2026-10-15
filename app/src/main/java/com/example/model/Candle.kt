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
            Timeframe.M1, Timeframe.M3, Timeframe.M5, Timeframe.M15, Timeframe.M30 -> "HH:mm"
            Timeframe.H1, Timeframe.H2, Timeframe.H4, Timeframe.H6, Timeframe.H8, Timeframe.H12 -> "MM/dd HH:mm"
            Timeframe.D1, Timeframe.D3 -> "yyyy-MM-dd"
            Timeframe.W1 -> "'W'w yyyy"
            Timeframe.MN1 -> "yyyy-MM"
        }
        val sdf = SimpleDateFormat(pattern, Locale.US)
        return sdf.format(Date(timestamp))
    }

    fun formattedDateTime(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return sdf.format(Date(timestamp))
    }
}

enum class Timeframe(
    val label: String,
    val binanceInterval: String,
    val intervalSeconds: Long,
    val defaultCandleCount: Int = 120
) {
    M1("1m", "1m", 60),
    M3("3m", "3m", 180),
    M5("5m", "5m", 300),
    M15("15m", "15m", 900),
    M30("30m", "30m", 1800),
    H1("1h", "1h", 3600),
    H2("2h", "2h", 7200),
    H4("4h", "4h", 14400),
    H6("6h", "6h", 21600),
    H8("8h", "8h", 28800),
    H12("12h", "12h", 43200),
    D1("1D", "1d", 86400),
    D3("3D", "3d", 259200),
    W1("1W", "1w", 604800),
    MN1("1M", "1M", 2592000);

    companion object {
        fun fromInterval(interval: String): Timeframe {
            return entries.find { it.binanceInterval == interval || it.label.equals(interval, ignoreCase = true) } ?: M15
        }
    }
}

enum class ChartStyle(val label: String) {
    CANDLES("Candles"),
    LINE("Line"),
    AREA("Area")
}
