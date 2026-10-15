package com.example

import com.example.data.remote.BinanceMarketDataProvider
import com.example.data.remote.OrderBookData
import com.example.data.remote.OrderBookEntry
import com.example.model.AlertCondition
import com.example.model.Candle
import com.example.model.PriceAlert
import com.example.model.TechnicalIndicatorCalculator
import com.example.model.Timeframe
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAlertConditionCrossingUp() {
        val alert = PriceAlert(
            symbol = "BTC/USDT",
            targetPrice = 68000.0,
            condition = AlertCondition.CROSSING_UP
        )

        // Previous below, current above -> met
        assertTrue(alert.isConditionMet(currentPrice = 68100.0, previousPrice = 67900.0))

        // Previous below, current still below -> not met
        assertFalse(alert.isConditionMet(currentPrice = 67950.0, previousPrice = 67900.0))

        // Downward move through target -> not met for crossing UP
        assertFalse(alert.isConditionMet(currentPrice = 67900.0, previousPrice = 68100.0))
    }

    @Test
    fun testAlertConditionCrossingDown() {
        val alert = PriceAlert(
            symbol = "BTC/USDT",
            targetPrice = 66000.0,
            condition = AlertCondition.CROSSING_DOWN
        )

        // Previous above, current below -> met
        assertTrue(alert.isConditionMet(currentPrice = 65950.0, previousPrice = 66100.0))

        // Upward move through target -> not met for crossing DOWN
        assertFalse(alert.isConditionMet(currentPrice = 66100.0, previousPrice = 65900.0))
    }

    @Test
    fun testAlertConditionCrossingEither() {
        val alert = PriceAlert(
            symbol = "BTC/USDT",
            targetPrice = 67000.0,
            condition = AlertCondition.CROSSING
        )

        assertTrue(alert.isConditionMet(currentPrice = 67100.0, previousPrice = 66900.0))
        assertTrue(alert.isConditionMet(currentPrice = 66900.0, previousPrice = 67100.0))
    }

    private fun generateTestCandles(count: Int = 50): List<Candle> {
        val now = 1700000000000L
        return (1..count).map { i ->
            val base = 100.0 + (i * 0.5)
            Candle(
                timestamp = now + (i * 60000L),
                open = base,
                high = base + 2.0,
                low = base - 1.5,
                close = base + 1.0,
                volume = 1000.0 + (i * 10.0)
            )
        }
    }

    @Test
    fun testSmaCalculation() {
        val candles = generateTestCandles(30)
        val sma10 = TechnicalIndicatorCalculator.calculateSma(candles, 10)
        assertEquals(30, sma10.size)
        // First 9 should be null
        for (i in 0 until 9) {
            assertEquals(null, sma10[i])
        }
        // 10th item onwards must have non-null average
        assertTrue(sma10[9] != null)
        assertTrue(sma10.last() != null)
    }

    @Test
    fun testEmaCalculation() {
        val candles = generateTestCandles(35)
        val ema20 = TechnicalIndicatorCalculator.calculateEma(candles, 20)
        assertEquals(candles.size, ema20.size)
        assertEquals(null, ema20[18])
        assertTrue(ema20[19] != null)
        assertTrue(ema20.last() != null)
    }

    @Test
    fun testVwapCalculation() {
        val candles = generateTestCandles(25)
        val vwap = TechnicalIndicatorCalculator.calculateVwap(candles)
        assertEquals(candles.size, vwap.size)
        assertTrue(vwap.all { it != null && it > 0 })
    }

    @Test
    fun testBollingerBands() {
        val candles = generateTestCandles(40)
        val bb = TechnicalIndicatorCalculator.calculateBollingerBands(candles, period = 20, stdDevMultiplier = 2.0)
        assertEquals(candles.size, bb.size)
        assertEquals(null, bb[18])

        val point = bb[25]
        assertTrue(point != null)
        if (point != null) {
            assertTrue("Upper must be >= Middle", point.upper >= point.middle)
            assertTrue("Middle must be >= Lower", point.middle >= point.lower)
        }
    }

    @Test
    fun testRsiCalculation() {
        val candles = generateTestCandles(40)
        val rsi = TechnicalIndicatorCalculator.calculateRsi(candles, period = 14)
        assertEquals(candles.size, rsi.size)
        val validRsi = rsi.filterNotNull()
        assertTrue(validRsi.isNotEmpty())
        for (v in validRsi) {
            assertTrue("RSI must be between 0 and 100: $v", v in 0.0..100.0)
        }
    }

    @Test
    fun testMacdCalculation() {
        val candles = generateTestCandles(50)
        val macd = TechnicalIndicatorCalculator.calculateMacd(candles, 12, 26, 9)
        assertEquals(candles.size, macd.size)
        val lastMacd = macd.last()
        assertTrue("MACD point should be calculated after sufficient periods", lastMacd != null)
        if (lastMacd != null) {
            val expectedHist = lastMacd.macd - lastMacd.signal
            assertEquals(expectedHist, lastMacd.histogram, 0.001)
        }
    }

    @Test
    fun testAtrCalculation() {
        val candles = generateTestCandles(30)
        val atr = TechnicalIndicatorCalculator.calculateAtr(candles, 14)
        assertEquals(candles.size, atr.size)
        val lastAtr = atr.last()
        assertTrue(lastAtr != null && lastAtr > 0.0)
    }

    @Test
    fun testSupertrendCalculation() {
        val candles = generateTestCandles(30)
        val st = TechnicalIndicatorCalculator.calculateSupertrend(candles, 10, 3.0)
        assertEquals(candles.size, st.size)
        val lastSt = st.last()
        assertTrue(lastSt != null)
        if (lastSt != null) {
            assertTrue(lastSt.value > 0.0)
        }
    }

    @Test
    fun testCleanSymbol() {
        assertEquals("BTCUSDT", BinanceMarketDataProvider.cleanSymbol("BTC/USDT"))
        assertEquals("ETHUSDT", BinanceMarketDataProvider.cleanSymbol("ETH-USDT"))
        assertEquals("SOLUSDT", BinanceMarketDataProvider.cleanSymbol("sol_usdt"))
    }

    @Test
    fun testTimeframeMapping() {
        assertEquals(15, Timeframe.entries.size)
        assertEquals("1m", Timeframe.M1.binanceInterval)
        assertEquals("15m", Timeframe.M15.binanceInterval)
        assertEquals("1h", Timeframe.H1.binanceInterval)
        assertEquals("4h", Timeframe.H4.binanceInterval)
        assertEquals("1d", Timeframe.D1.binanceInterval)
        assertEquals("1w", Timeframe.W1.binanceInterval)
        assertEquals("1M", Timeframe.MN1.binanceInterval)
    }

    @Test
    fun testOrderBookSpread() {
        val bids = listOf(OrderBookEntry(price = 65000.0, quantity = 1.5))
        val asks = listOf(OrderBookEntry(price = 65005.0, quantity = 2.0))
        val ob = OrderBookData(symbol = "BTC/USDT", bids = bids, asks = asks)

        assertEquals(5.0, ob.spread, 0.001)
        assertTrue(ob.spreadPercent > 0.0)
    }
}
