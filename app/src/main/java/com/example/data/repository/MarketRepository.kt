package com.example.data.repository

import com.example.model.AssetCategory
import com.example.model.Candle
import com.example.model.SymbolInfo
import com.example.model.Timeframe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Random
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

data class PriceTick(
    val symbol: String,
    val price: Double,
    val previousPrice: Double,
    val timestamp: Long = System.currentTimeMillis()
)

class MarketRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    private val random = Random(42)

    private val initialSymbols = listOf(
        SymbolInfo(
            symbol = "BTC/USDT",
            name = "Bitcoin",
            category = AssetCategory.CRYPTO,
            currentPrice = 67450.00,
            previousClose = 66120.00,
            high24h = 68150.00,
            low24h = 65890.00,
            volume24h = 42890000000.0,
            priceDecimals = 2,
            isFavorite = true
        ),
        SymbolInfo(
            symbol = "ETH/USDT",
            name = "Ethereum",
            category = AssetCategory.CRYPTO,
            currentPrice = 3520.40,
            previousClose = 3450.00,
            high24h = 3585.00,
            low24h = 3410.00,
            volume24h = 18450000000.0,
            priceDecimals = 2,
            isFavorite = true
        ),
        SymbolInfo(
            symbol = "SOL/USDT",
            name = "Solana",
            category = AssetCategory.CRYPTO,
            currentPrice = 184.65,
            previousClose = 179.20,
            high24h = 189.50,
            low24h = 176.80,
            volume24h = 5890000000.0,
            priceDecimals = 2,
            isFavorite = false
        ),
        SymbolInfo(
            symbol = "NVDA",
            name = "NVIDIA Corp.",
            category = AssetCategory.STOCKS,
            currentPrice = 134.80,
            previousClose = 131.50,
            high24h = 136.20,
            low24h = 130.40,
            volume24h = 68900000000.0,
            priceDecimals = 2,
            isFavorite = true
        ),
        SymbolInfo(
            symbol = "AAPL",
            name = "Apple Inc.",
            category = AssetCategory.STOCKS,
            currentPrice = 228.75,
            previousClose = 226.40,
            high24h = 230.10,
            low24h = 225.80,
            volume24h = 41200000000.0,
            priceDecimals = 2,
            isFavorite = false
        ),
        SymbolInfo(
            symbol = "TSLA",
            name = "Tesla Inc.",
            category = AssetCategory.STOCKS,
            currentPrice = 248.90,
            previousClose = 254.10,
            high24h = 256.40,
            low24h = 246.20,
            volume24h = 35800000000.0,
            priceDecimals = 2,
            isFavorite = false
        ),
        SymbolInfo(
            symbol = "SPY",
            name = "SPDR S&P 500 ETF",
            category = AssetCategory.STOCKS,
            currentPrice = 582.40,
            previousClose = 580.10,
            high24h = 584.20,
            low24h = 579.50,
            volume24h = 75000000000.0,
            priceDecimals = 2,
            isFavorite = false
        ),
        SymbolInfo(
            symbol = "EUR/USD",
            name = "Euro / US Dollar",
            category = AssetCategory.FOREX,
            currentPrice = 1.0845,
            previousClose = 1.0820,
            high24h = 1.0872,
            low24h = 1.0815,
            volume24h = 120000000000.0,
            priceDecimals = 4,
            isFavorite = false
        ),
        SymbolInfo(
            symbol = "XAU/USD",
            name = "Gold Spot / US Dollar",
            category = AssetCategory.COMMODITIES,
            currentPrice = 2685.50,
            previousClose = 2668.00,
            high24h = 2692.00,
            low24h = 2664.00,
            volume24h = 28000000000.0,
            priceDecimals = 2,
            isFavorite = true
        )
    )

    private val _symbols = MutableStateFlow<List<SymbolInfo>>(emptyList())
    val symbols: StateFlow<List<SymbolInfo>> = _symbols.asStateFlow()

    private val _priceTicks = MutableSharedFlow<PriceTick>(extraBufferCapacity = 64)
    val priceTicks: SharedFlow<PriceTick> = _priceTicks.asSharedFlow()

    // Cache of candle history per symbol + timeframe
    private val candleCache = mutableMapOf<String, MutableList<Candle>>()

    private var isSimulating = true

    init {
        // Pre-generate sparklines for symbols
        val initializedSymbols = initialSymbols.map { sym ->
            val spark = generateSparkline(sym.currentPrice, sym.priceDecimals)
            sym.copy(sparkline = spark)
        }
        _symbols.value = initializedSymbols

        // Start live ticker loop
        startLiveSimulation()
    }

    private fun generateSparkline(basePrice: Double, decimals: Int): List<Double> {
        val points = mutableListOf<Double>()
        var price = basePrice * 0.98
        points.add(price)
        val rnd = Random()
        repeat(15) {
            val delta = price * ((rnd.nextDouble() - 0.48) * 0.008)
            price += delta
            points.add(price)
        }
        points[points.lastIndex] = basePrice
        return points
    }

    fun getSymbol(symbol: String): SymbolInfo? {
        return _symbols.value.find { it.symbol == symbol }
    }

    fun toggleFavorite(symbol: String) {
        _symbols.value = _symbols.value.map {
            if (it.symbol == symbol) it.copy(isFavorite = !it.isFavorite) else it
        }
    }

    /**
     * Gets or generates historical candles for the given symbol and timeframe.
     */
    fun getCandles(symbol: String, timeframe: Timeframe): List<Candle> {
        val key = "${symbol}_${timeframe.name}"
        return synchronized(candleCache) {
            candleCache.getOrPut(key) {
                generateHistoricalCandles(symbol, timeframe).toMutableList()
            }.toList()
        }
    }

    private fun generateHistoricalCandles(symbol: String, timeframe: Timeframe): List<Candle> {
        val symInfo = getSymbol(symbol) ?: initialSymbols.first()
        val count = timeframe.defaultCandleCount
        val intervalMs = timeframe.intervalSeconds * 1000L
        val now = System.currentTimeMillis()
        val currentPrice = symInfo.currentPrice

        val candles = mutableListOf<Candle>()
        val rnd = Random(symbol.hashCode().toLong() + timeframe.ordinal * 1000L)

        // Volatility depends on asset
        val volatility = when (symInfo.category) {
            AssetCategory.CRYPTO -> 0.012
            AssetCategory.STOCKS -> 0.008
            AssetCategory.FOREX -> 0.002
            AssetCategory.COMMODITIES -> 0.005
            else -> 0.008
        }

        // Walk backwards from current price
        var closePrice = currentPrice
        val tempCandles = mutableListOf<Candle>()

        for (i in 0 until count) {
            val candleTime = now - ((count - 1 - i) * intervalMs)

            // Random walk with slight mean reversion
            val percentChange = (rnd.nextGaussian() * volatility) + 0.0005
            val open = if (i == count - 1) {
                closePrice * (1.0 - (rnd.nextDouble() - 0.5) * volatility * 0.5)
            } else {
                closePrice / (1.0 + percentChange)
            }

            val highLowSpread = abs(open * volatility * (0.8 + rnd.nextDouble()))
            val high = max(open, closePrice) + (rnd.nextDouble() * highLowSpread)
            val low = min(open, closePrice) - (rnd.nextDouble() * highLowSpread)
            val volume = (symInfo.volume24h / (count * 2)) * (0.5 + rnd.nextDouble() * 1.2)

            tempCandles.add(
                Candle(
                    timestamp = candleTime,
                    open = roundToDecimals(open, symInfo.priceDecimals),
                    high = roundToDecimals(high, symInfo.priceDecimals),
                    low = roundToDecimals(low, symInfo.priceDecimals),
                    close = roundToDecimals(closePrice, symInfo.priceDecimals),
                    volume = volume
                )
            )

            // Next step in backwards walk
            closePrice = open
        }

        // Sort by timestamp ascending
        return tempCandles
    }

    private fun startLiveSimulation() {
        scope.launch {
            while (true) {
                delay(1200) // Live tick pulse every 1.2 seconds
                if (!isSimulating) continue

                // Pick a symbol to update or update all
                _symbols.value = _symbols.value.map { sym ->
                    val rnd = Random()
                    val volatility = when (sym.category) {
                        AssetCategory.CRYPTO -> 0.0018
                        AssetCategory.STOCKS -> 0.0010
                        AssetCategory.FOREX -> 0.0002
                        AssetCategory.COMMODITIES -> 0.0008
                        else -> 0.001
                    }

                    // Drift tick
                    val drift = (rnd.nextDouble() - 0.495) * volatility
                    val oldPrice = sym.currentPrice
                    val newPrice = roundToDecimals(oldPrice * (1.0 + drift), sym.priceDecimals)

                    // Emit tick event
                    _priceTicks.tryEmit(PriceTick(sym.symbol, newPrice, oldPrice))

                    // Update candle cache for active candles
                    updateLatestCandle(sym.symbol, newPrice)

                    val updatedHigh = max(sym.high24h, newPrice)
                    val updatedLow = min(sym.low24h, newPrice)
                    val newSparkline = sym.sparkline.drop(1) + newPrice

                    sym.copy(
                        currentPrice = newPrice,
                        high24h = updatedHigh,
                        low24h = updatedLow,
                        sparkline = newSparkline
                    )
                }
            }
        }
    }

    private fun updateLatestCandle(symbol: String, newPrice: Double) {
        synchronized(candleCache) {
            Timeframe.entries.forEach { tf ->
                val key = "${symbol}_${tf.name}"
                val list = candleCache[key]
                if (list != null && list.isNotEmpty()) {
                    val lastIdx = list.lastIndex
                    val lastCandle = list[lastIdx]
                    val updated = lastCandle.copy(
                        high = max(lastCandle.high, newPrice),
                        low = min(lastCandle.low, newPrice),
                        close = newPrice,
                        volume = lastCandle.volume + (lastCandle.volume * 0.002)
                    )
                    list[lastIdx] = updated
                }
            }
        }
    }

    /**
     * Instantly stimulates a price move (e.g. +2% or -2%) to test price alerts immediately!
     */
    fun triggerPriceShock(symbol: String, percentDelta: Double) {
        val sym = getSymbol(symbol) ?: return
        val oldPrice = sym.currentPrice
        val newPrice = roundToDecimals(oldPrice * (1.0 + percentDelta / 100.0), sym.priceDecimals)

        _priceTicks.tryEmit(PriceTick(symbol, newPrice, oldPrice))
        updateLatestCandle(symbol, newPrice)

        _symbols.value = _symbols.value.map {
            if (it.symbol == symbol) {
                it.copy(
                    currentPrice = newPrice,
                    high24h = max(it.high24h, newPrice),
                    low24h = min(it.low24h, newPrice),
                    sparkline = it.sparkline.drop(1) + newPrice
                )
            } else it
        }
    }

    fun setSimulationActive(active: Boolean) {
        isSimulating = active
    }

    private fun roundToDecimals(value: Double, decimals: Int): Double {
        val factor = Math.pow(10.0, decimals.toDouble())
        return Math.round(value * factor) / factor
    }
}
