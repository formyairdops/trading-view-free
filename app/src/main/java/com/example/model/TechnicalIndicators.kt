package com.example.model

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

data class IndicatorSettings(
    // Overlays
    val showSma: Boolean = false,
    val smaPeriod: Int = 20,

    val showEma20: Boolean = true,
    val ema20Period: Int = 20,

    val showEma50: Boolean = false,
    val ema50Period: Int = 50,

    val showVwap: Boolean = false,

    val showBollingerBands: Boolean = false,
    val bbPeriod: Int = 20,
    val bbStdDev: Double = 2.0,

    val showSupertrend: Boolean = false,
    val supertrendAtrPeriod: Int = 10,
    val supertrendMultiplier: Double = 3.0,

    // Sub-pane Oscillators
    val showVolume: Boolean = true,

    val showRsi: Boolean = false,
    val rsiPeriod: Int = 14,

    val showMacd: Boolean = false,
    val macdFastPeriod: Int = 12,
    val macdSlowPeriod: Int = 26,
    val macdSignalPeriod: Int = 9,

    val showAtr: Boolean = false,
    val atrPeriod: Int = 14,

    val showStochRsi: Boolean = false,
    val stochRsiPeriod: Int = 14,
    val stochKPeriod: Int = 3,
    val stochDPeriod: Int = 3
)

data class BollingerBandPoint(
    val upper: Double,
    val middle: Double,
    val lower: Double
)

data class MacdPoint(
    val macd: Double,
    val signal: Double,
    val histogram: Double
)

data class StochRsiPoint(
    val k: Double,
    val d: Double
)

data class SupertrendPoint(
    val value: Double,
    val isBullish: Boolean // true for uptrend (green), false for downtrend (red)
)

object TechnicalIndicatorCalculator {

    /**
     * Simple Moving Average (SMA)
     */
    fun calculateSma(candles: List<Candle>, period: Int): List<Double?> {
        if (candles.isEmpty() || period <= 0) return emptyList()
        val result = MutableList<Double?>(candles.size) { null }
        if (candles.size < period) return result

        var sum = 0.0
        for (i in 0 until period) {
            sum += candles[i].close
        }
        result[period - 1] = sum / period

        for (i in period until candles.size) {
            sum += candles[i].close - candles[i - period].close
            result[i] = sum / period
        }
        return result
    }

    /**
     * Exponential Moving Average (EMA)
     */
    fun calculateEma(candles: List<Candle>, period: Int): List<Double?> {
        if (candles.isEmpty() || period <= 0) return emptyList()
        val result = MutableList<Double?>(candles.size) { null }
        if (candles.size < period) return result

        val multiplier = 2.0 / (period + 1.0)

        // Seed with simple average
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
     * Volume Weighted Average Price (VWAP)
     * VWAP = sum(Typical Price * Volume) / sum(Volume)
     */
    fun calculateVwap(candles: List<Candle>): List<Double?> {
        if (candles.isEmpty()) return emptyList()
        val result = ArrayList<Double?>(candles.size)

        var cumulativePv = 0.0
        var cumulativeVol = 0.0

        for (candle in candles) {
            val typicalPrice = (candle.high + candle.low + candle.close) / 3.0
            cumulativePv += typicalPrice * candle.volume
            cumulativeVol += candle.volume

            if (cumulativeVol > 0) {
                result.add(cumulativePv / cumulativeVol)
            } else {
                result.add(typicalPrice)
            }
        }
        return result
    }

    /**
     * Bollinger Bands (SMA + 2 StdDev)
     */
    fun calculateBollingerBands(
        candles: List<Candle>,
        period: Int = 20,
        stdDevMultiplier: Double = 2.0
    ): List<BollingerBandPoint?> {
        val result = MutableList<BollingerBandPoint?>(candles.size) { null }
        if (candles.size < period || period <= 0) return result

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
     * Relative Strength Index (RSI 14) - Wilder's smoothed method
     */
    fun calculateRsi(candles: List<Candle>, period: Int = 14): List<Double?> {
        val result = MutableList<Double?>(candles.size) { null }
        if (candles.size <= period || period <= 0) return result

        var gains = 0.0
        var losses = 0.0

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

    /**
     * Moving Average Convergence Divergence (MACD)
     * MACD Line = 12 EMA - 26 EMA
     * Signal Line = 9 EMA of MACD Line
     * Histogram = MACD Line - Signal Line
     */
    fun calculateMacd(
        candles: List<Candle>,
        fastPeriod: Int = 12,
        slowPeriod: Int = 26,
        signalPeriod: Int = 9
    ): List<MacdPoint?> {
        val result = MutableList<MacdPoint?>(candles.size) { null }
        if (candles.size < slowPeriod + signalPeriod) return result

        val fastEma = calculateEma(candles, fastPeriod)
        val slowEma = calculateEma(candles, slowPeriod)

        val macdLine = MutableList<Double?>(candles.size) { null }
        for (i in slowPeriod - 1 until candles.size) {
            val f = fastEma[i]
            val s = slowEma[i]
            if (f != null && s != null) {
                macdLine[i] = f - s
            }
        }

        // Calculate Signal Line (EMA of valid MACD values)
        val validMacdValues = mutableListOf<Double>()
        val validIndices = mutableListOf<Int>()
        for (i in macdLine.indices) {
            val v = macdLine[i]
            if (v != null) {
                validMacdValues.add(v)
                validIndices.add(i)
            }
        }

        if (validMacdValues.size < signalPeriod) return result

        val signalMultiplier = 2.0 / (signalPeriod + 1.0)
        var sigSum = 0.0
        for (i in 0 until signalPeriod) {
            sigSum += validMacdValues[i]
        }
        var prevSignal = sigSum / signalPeriod

        val signalStartGlobalIndex = validIndices[signalPeriod - 1]
        val macdAtStart = macdLine[signalStartGlobalIndex] ?: 0.0
        result[signalStartGlobalIndex] = MacdPoint(
            macd = macdAtStart,
            signal = prevSignal,
            histogram = macdAtStart - prevSignal
        )

        for (i in signalPeriod until validMacdValues.size) {
            val currentMacd = validMacdValues[i]
            val globalIdx = validIndices[i]
            val currentSignal = (currentMacd - prevSignal) * signalMultiplier + prevSignal
            result[globalIdx] = MacdPoint(
                macd = currentMacd,
                signal = currentSignal,
                histogram = currentMacd - currentSignal
            )
            prevSignal = currentSignal
        }

        return result
    }

    /**
     * Average True Range (ATR)
     * TR = max(high - low, abs(high - prevClose), abs(low - prevClose))
     */
    fun calculateAtr(candles: List<Candle>, period: Int = 14): List<Double?> {
        val result = MutableList<Double?>(candles.size) { null }
        if (candles.size <= period || period <= 0) return result

        val trueRanges = ArrayList<Double>(candles.size)
        trueRanges.add(candles[0].high - candles[0].low)

        for (i in 1 until candles.size) {
            val current = candles[i]
            val prevClose = candles[i - 1].close
            val tr = max(
                current.high - current.low,
                max(abs(current.high - prevClose), abs(current.low - prevClose))
            )
            trueRanges.add(tr)
        }

        var sumTr = 0.0
        for (i in 0 until period) {
            sumTr += trueRanges[i]
        }
        var prevAtr = sumTr / period
        result[period - 1] = prevAtr

        for (i in period until candles.size) {
            val currentAtr = (prevAtr * (period - 1) + trueRanges[i]) / period
            result[i] = currentAtr
            prevAtr = currentAtr
        }
        return result
    }

    /**
     * Stochastic RSI (%K, %D)
     */
    fun calculateStochRsi(
        candles: List<Candle>,
        rsiPeriod: Int = 14,
        stochPeriod: Int = 14,
        kPeriod: Int = 3,
        dPeriod: Int = 3
    ): List<StochRsiPoint?> {
        val result = MutableList<StochRsiPoint?>(candles.size) { null }
        val rsiValues = calculateRsi(candles, rsiPeriod)

        val rawStochRsi = MutableList<Double?>(candles.size) { null }
        for (i in (rsiPeriod + stochPeriod) until candles.size) {
            val window = rsiValues.subList(i - stochPeriod + 1, i + 1).filterNotNull()
            if (window.size == stochPeriod) {
                val minRsi = window.minOrNull() ?: 0.0
                val maxRsi = window.maxOrNull() ?: 100.0
                val currentRsi = rsiValues[i] ?: 50.0
                val stoch = if (maxRsi - minRsi != 0.0) {
                    ((currentRsi - minRsi) / (maxRsi - minRsi)) * 100.0
                } else {
                    50.0
                }
                rawStochRsi[i] = stoch
            }
        }

        // Smooth %K (SMA of raw StochRsi)
        val kValues = MutableList<Double?>(candles.size) { null }
        for (i in candles.indices) {
            if (i >= kPeriod - 1) {
                val sub = rawStochRsi.subList(i - kPeriod + 1, i + 1)
                if (sub.all { it != null }) {
                    kValues[i] = sub.filterNotNull().average()
                }
            }
        }

        // Smooth %D (SMA of %K)
        for (i in candles.indices) {
            if (i >= dPeriod - 1) {
                val sub = kValues.subList(i - dPeriod + 1, i + 1)
                if (sub.all { it != null }) {
                    val kVal = kValues[i]!!
                    val dVal = sub.filterNotNull().average()
                    result[i] = StochRsiPoint(k = kVal, d = dVal)
                }
            }
        }

        return result
    }

    /**
     * Supertrend Indicator
     * Uses ATR with multiplier to generate dynamic support/resistance stop lines.
     */
    fun calculateSupertrend(
        candles: List<Candle>,
        atrPeriod: Int = 10,
        multiplier: Double = 3.0
    ): List<SupertrendPoint?> {
        val result = MutableList<SupertrendPoint?>(candles.size) { null }
        if (candles.size <= atrPeriod) return result

        val atr = calculateAtr(candles, atrPeriod)

        var prevUpper = 0.0
        var prevLower = 0.0
        var prevClose = 0.0
        var inUptrend = true

        for (i in atrPeriod until candles.size) {
            val c = candles[i]
            val atrVal = atr[i] ?: continue
            val hl2 = (c.high + c.low) / 2.0

            var basicUpper = hl2 + (multiplier * atrVal)
            var basicLower = hl2 - (multiplier * atrVal)

            var finalUpper = if (basicUpper < prevUpper || prevClose > prevUpper) basicUpper else prevUpper
            var finalLower = if (basicLower > prevLower || prevClose < prevLower) basicLower else prevLower

            val isBullish = if (c.close > prevUpper) {
                true
            } else if (c.close < prevLower) {
                false
            } else {
                inUptrend
            }

            val value = if (isBullish) finalLower else finalUpper
            result[i] = SupertrendPoint(value = value, isBullish = isBullish)

            prevUpper = finalUpper
            prevLower = finalLower
            prevClose = c.close
            inUptrend = isBullish
        }

        return result
    }
}
