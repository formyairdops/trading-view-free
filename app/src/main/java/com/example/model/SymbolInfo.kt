package com.example.model

import java.text.DecimalFormat

enum class AssetCategory(val label: String) {
    ALL("All"),
    CRYPTO("Crypto"),
    STOCKS("Stocks"),
    FOREX("Forex"),
    COMMODITIES("Commodities")
}

data class SymbolInfo(
    val symbol: String,
    val name: String,
    val category: AssetCategory,
    val currentPrice: Double,
    val previousClose: Double,
    val high24h: Double,
    val low24h: Double,
    val volume24h: Double,
    val priceDecimals: Int = 2,
    val sparkline: List<Double> = emptyList(),
    val isFavorite: Boolean = false
) {
    val change: Double get() = currentPrice - previousClose
    val changePercent: Double get() = if (previousClose > 0) (change / previousClose) * 100.0 else 0.0
    val isPositive: Boolean get() = change >= 0

    fun formatPrice(price: Double = currentPrice): String {
        return when {
            priceDecimals == 0 -> DecimalFormat("#,##0").format(price)
            priceDecimals == 2 -> DecimalFormat("#,##0.00").format(price)
            priceDecimals == 4 -> DecimalFormat("#,##0.0000").format(price)
            else -> DecimalFormat("#,##0.00").format(price)
        }
    }

    fun formatChange(): String {
        val sign = if (change >= 0) "+" else ""
        return "$sign${formatPrice(change)} (${String.format("%.2f", changePercent)}%)"
    }

    fun formatVolume(): String {
        return when {
            volume24h >= 1_000_000_000 -> String.format("%.2fB", volume24h / 1_000_000_000)
            volume24h >= 1_000_000 -> String.format("%.2fM", volume24h / 1_000_000)
            volume24h >= 1_000 -> String.format("%.2fK", volume24h / 1_000)
            else -> String.format("%.0f", volume24h)
        }
    }
}
