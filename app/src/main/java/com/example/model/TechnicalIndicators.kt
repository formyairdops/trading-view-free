package com.example.model

import kotlin.math.sqrt

data class IndicatorSettings(
    val showEma20: Boolean = true,
    val showEma50: Boolean = false,
    val showBollingerBands: Boolean = false,
    val showRsi: Boolean = false,
    val showVolume: Boolean = true
)

data class BollingerBandPoint(
    val upper: Double,
    val middle: Double,
    val lower: Double
)

object TechnicalIndicatorCalculator {

    /**
     * Calculates Exponential Moving Average (EMA) for the given period.
     * Returns a list of the same size as candles, with null for indices where EMA is not yet valid.
     */
    fun calculateEma(candles: List<Candle>, period: Int): List<Double?> {
        if (candles.isEmpty()) return emptyList()
        val result = MutableList<Double?>(candles.size) { null }
        if (candles.size < period) return result

        val multiplier = 2.0 / (period + 1.0)

        // Seed with simple average of first 'period' close prices
        var sum = 0.0
        for (i in 0 until period) {
            sum += candles[i].close
        }
        var prevEma = sum / period
        result[period - 1] = prevEma

        for (i in period until candles.size) {
            val currentPrice = candles[i].close
            val ema = (currentPrice - prevEma) * multiplier + prevEma
            result[i] = ema
            prevEma = ema
        }

        return result
    }

    /**
     * Calculates Bollinger Bands (20 period, 2 std dev)
     */
    fun calculateBollingerBands(candles: List<Candle>, period: Int = 20, stdDevMultiplier: Double = 2.0): List<BollingerBandPoint?> {
        val result = MutableList<BollingerBandPoint?>(candles.size) { null }
        if (candles.size < period) return result

        for (i in (period - 1) until candles.size) {
            val window = candles.subList(i - period + 1, i + 1)
            val mean = window.map { it.close }.average()
            val variance = window.map { (it.close - mean) * (it.close - mean) }.average()
            val stdDev = sqrt(variance)

            val upper = mean + stdDevMultiplier * stdDev
            val lower = mean - stdDevMultiplier * stdDev

            result[i] = BollingerBandPoint(upper = upper, middle = mean, lower = lower)
        }

        return result
    }

    /**
     * Calculates Relative Strength Index (RSI 14)
     * Values range from 0 to 100.
     */
    fun calculateRsi(candles: List<Candle>, period: Int = 14): List<Double?> {
        val result = MutableList<Double?>(candles.size) { null }
        if (candles.size <= period) return result

        var gains = 0.0
        var losses = 0.0

        // Initial period
        for (i in 1..period) {
            val change = candles[i].close - candles[i - 1].close
            if (change >= 0) gains += change else losses += -change
        }

        var avgGain = gains / period
        var avgLoss = losses / period

        if (avgLoss == 0.0) {
            result[period] = 100.0
        } else {
            val rs = avgGain / avgLoss
            result[period] = 100.0 - (100.0 / (1.0 + rs))
        }

        for (i in (period + 1) until candles.size) {
            val change = candles[i].close - candles[i - 1].close
            val currentGain = if (change > 0) change else 0.0
            val currentLoss = if (change < 0) -change else 0.0

            avgGain = ((avgGain * (period - 1)) + currentGain) / period
            avgLoss = ((avgLoss * (period - 1)) + currentLoss) / period

            if (avgLoss == 0.0) {
                result[i] = 100.0
            } else {
                val rs = avgGain / avgLoss
                result[i] = 100.0 - (100.0 / (1.0 + rs))
            }
        }

        return result
    }
}
