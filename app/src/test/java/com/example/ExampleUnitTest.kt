package com.example

import com.example.model.AlertCondition
import com.example.model.Candle
import com.example.model.PriceAlert
import com.example.model.TechnicalIndicatorCalculator
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

    @Test
    fun testEmaCalculation() {
        val now = System.currentTimeMillis()
        val candles = (1..30).map { i ->
            Candle(
                timestamp = now + i * 60000L,
                open = 100.0 + i,
                high = 105.0 + i,
                low = 95.0 + i,
                close = 100.0 + i,
                volume = 1000.0
            )
        }

        val ema20 = TechnicalIndicatorCalculator.calculateEma(candles, 20)
        assertEquals(candles.size, ema20.size)
        // First 19 should be null
        assertEquals(null, ema20[18])
        // 20th item onwards should have values
        assertTrue(ema20[19] != null)
        assertTrue(ema20.last() != null)
    }
}
