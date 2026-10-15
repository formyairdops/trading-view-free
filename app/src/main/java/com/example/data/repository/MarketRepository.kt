package com.example.data.repository

import android.util.Log
import com.example.data.remote.BinanceMarketDataProvider
import com.example.data.remote.ConnectionStatus
import com.example.data.remote.KlineStreamEvent
import com.example.data.remote.MarketDataProvider
import com.example.data.remote.OrderBookData
import com.example.model.AssetCategory
import com.example.model.Candle
import com.example.model.SymbolInfo
import com.example.model.Timeframe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.max
import kotlin.math.min

data class PriceTick(
    val symbol: String,
    val price: Double,
    val previousPrice: Double,
    val timestamp: Long = System.currentTimeMillis()
)

class MarketRepository(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val dataProvider: MarketDataProvider = BinanceMarketDataProvider(scope)
) {
    private val TAG = "MarketRepository"

    val connectionStatus: StateFlow<ConnectionStatus> = dataProvider.connectionStatus
    val lastUpdateTimestamp: StateFlow<Long?> = dataProvider.lastUpdateTimestamp

    // Pre-configured cryptocurrency pairs supported on public exchange
    private val defaultCryptoSymbols = listOf(
        SymbolInfo(
            symbol = "BTC/USDT",
            name = "Bitcoin",
            category = AssetCategory.CRYPTO,
            currentPrice = 0.0,
            previousClose = 0.0,
            high24h = 0.0,
            low24h = 0.0,
            volume24h = 0.0,
            priceDecimals = 2,
            isFavorite = true
        ),
        SymbolInfo(
            symbol = "ETH/USDT",
            name = "Ethereum",
            category = AssetCategory.CRYPTO,
            currentPrice = 0.0,
            previousClose = 0.0,
            high24h = 0.0,
            low24h = 0.0,
            volume24h = 0.0,
            priceDecimals = 2,
            isFavorite = true
        ),
        SymbolInfo(
            symbol = "SOL/USDT",
            name = "Solana",
            category = AssetCategory.CRYPTO,
            currentPrice = 0.0,
            previousClose = 0.0,
            high24h = 0.0,
            low24h = 0.0,
            volume24h = 0.0,
            priceDecimals = 2,
            isFavorite = true
        ),
        SymbolInfo(
            symbol = "BNB/USDT",
            name = "BNB",
            category = AssetCategory.CRYPTO,
            currentPrice = 0.0,
            previousClose = 0.0,
            high24h = 0.0,
            low24h = 0.0,
            volume24h = 0.0,
            priceDecimals = 2,
            isFavorite = false
        ),
        SymbolInfo(
            symbol = "XRP/USDT",
            name = "Ripple",
            category = AssetCategory.CRYPTO,
            currentPrice = 0.0,
            previousClose = 0.0,
            high24h = 0.0,
            low24h = 0.0,
            volume24h = 0.0,
            priceDecimals = 4,
            isFavorite = false
        ),
        SymbolInfo(
            symbol = "DOGE/USDT",
            name = "Dogecoin",
            category = AssetCategory.CRYPTO,
            currentPrice = 0.0,
            previousClose = 0.0,
            high24h = 0.0,
            low24h = 0.0,
            volume24h = 0.0,
            priceDecimals = 4,
            isFavorite = false
        ),
        SymbolInfo(
            symbol = "ADA/USDT",
            name = "Cardano",
            category = AssetCategory.CRYPTO,
            currentPrice = 0.0,
            previousClose = 0.0,
            high24h = 0.0,
            low24h = 0.0,
            volume24h = 0.0,
            priceDecimals = 4,
            isFavorite = false
        ),
        SymbolInfo(
            symbol = "AVAX/USDT",
            name = "Avalanche",
            category = AssetCategory.CRYPTO,
            currentPrice = 0.0,
            previousClose = 0.0,
            high24h = 0.0,
            low24h = 0.0,
            volume24h = 0.0,
            priceDecimals = 2,
            isFavorite = false
        )
    )

    private val _symbols = MutableStateFlow<List<SymbolInfo>>(defaultCryptoSymbols)
    val symbols: StateFlow<List<SymbolInfo>> = _symbols.asStateFlow()

    private val _priceTicks = MutableSharedFlow<PriceTick>(extraBufferCapacity = 64)
    val priceTicks: SharedFlow<PriceTick> = _priceTicks.asSharedFlow()

    // Real candle cache per symbol_timeframe
    private val candleCache = ConcurrentHashMap<String, MutableList<Candle>>()

    // Real-time kline stream subscription job
    private var activeStreamJob: Job? = null
    private var currentStreamSymbol: String? = null
    private var currentStreamTimeframe: Timeframe? = null

    // Real Order Book state
    private val _orderBook = MutableStateFlow<OrderBookData?>(null)
    val orderBook: StateFlow<OrderBookData?> = _orderBook.asStateFlow()

    private val _isLoadingCandles = MutableStateFlow(false)
    val isLoadingCandles: StateFlow<Boolean> = _isLoadingCandles.asStateFlow()

    private val _candleLoadError = MutableStateFlow<String?>(null)
    val candleLoadError: StateFlow<String?> = _candleLoadError.asStateFlow()

    init {
        // Start background polling for 24h ticker updates for all watchlist symbols
        start24hTickerPoll()
    }

    private fun start24hTickerPoll() {
        scope.launch {
            while (isActive) {
                refresh24hTickers()
                delay(8000) // update 24h statistics every 8 seconds
            }
        }
    }

    suspend fun refresh24hTickers() = withContext(Dispatchers.IO) {
        val result = dataProvider.fetchAll24hTickers()
        if (result.isSuccess) {
            val tickerMap = result.getOrNull()?.associateBy { it.symbol } ?: emptyMap()
            _symbols.value = _symbols.value.map { sym ->
                val clean = sym.symbol.replace("/", "")
                val tick = tickerMap[clean]
                if (tick != null && tick.lastPrice > 0) {
                    val prevPrice = if (sym.currentPrice > 0) sym.currentPrice else tick.lastPrice - tick.priceChange
                    sym.copy(
                        currentPrice = tick.lastPrice,
                        previousClose = tick.lastPrice - tick.priceChange,
                        high24h = tick.highPrice,
                        low24h = tick.lowPrice,
                        volume24h = tick.quoteVolume, // USDT volume
                        sparkline = if (sym.sparkline.isEmpty()) listOf(tick.lowPrice, tick.lastPrice, tick.highPrice) else (sym.sparkline.takeLast(15) + tick.lastPrice)
                    )
                } else {
                    sym
                }
            }
        }
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
     * Loads historical candles from real public market data API.
     */
    suspend fun loadHistoricalCandles(
        symbol: String,
        timeframe: Timeframe,
        forceRefresh: Boolean = false
    ): Result<List<Candle>> = withContext(Dispatchers.IO) {
        val key = "${symbol}_${timeframe.binanceInterval}"
        if (!forceRefresh) {
            val cached = candleCache[key]
            if (cached != null && cached.isNotEmpty()) {
                return@withContext Result.success(cached.toList())
            }
        }

        _isLoadingCandles.value = true
        _candleLoadError.value = null

        val result = dataProvider.fetchHistoricalCandles(
            symbol = symbol,
            interval = timeframe.binanceInterval,
            limit = timeframe.defaultCandleCount
        )

        _isLoadingCandles.value = false

        if (result.isSuccess) {
            val fetchedCandles = result.getOrNull().orEmpty()
            if (fetchedCandles.isNotEmpty()) {
                candleCache[key] = fetchedCandles.toMutableList()

                // Update current symbol price with latest closed candle if valid
                val latest = fetchedCandles.last()
                updateSymbolPrice(symbol, latest.close)

                // Start or switch WebSocket streaming for live updates
                subscribeToLiveUpdates(symbol, timeframe)

                return@withContext Result.success(fetchedCandles)
            } else {
                _candleLoadError.value = "No candles returned from exchange for $symbol"
                return@withContext Result.failure(Exception("Empty candles response"))
            }
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: "Failed to connect to market data feed"
            _candleLoadError.value = errorMsg
            Log.e(TAG, "Candle fetch failed: $errorMsg")
            return@withContext Result.failure(result.exceptionOrNull() ?: Exception(errorMsg))
        }
    }

    /**
     * Synchronously returns cached candles, or empty list if not yet loaded.
     */
    fun getCandles(symbol: String, timeframe: Timeframe): List<Candle> {
        val key = "${symbol}_${timeframe.binanceInterval}"
        return candleCache[key]?.toList() ?: emptyList()
    }

    /**
     * Subscribes to live WebSocket stream for current symbol and timeframe.
     */
    fun subscribeToLiveUpdates(symbol: String, timeframe: Timeframe) {
        if (currentStreamSymbol == symbol && currentStreamTimeframe == timeframe && activeStreamJob?.isActive == true) {
            return
        }

        activeStreamJob?.cancel()
        currentStreamSymbol = symbol
        currentStreamTimeframe = timeframe

        activeStreamJob = scope.launch {
            dataProvider.startKlineStream(symbol, timeframe.binanceInterval)
                .collect { event ->
                    handleLiveKline(event)
                }
        }
    }

    /**
     * Processes live incoming kline from WebSocket.
     */
    private fun handleLiveKline(event: KlineStreamEvent) {
        val incoming = event.candle
        val key = "${event.symbol}_${event.interval}"
        val list = candleCache.getOrPut(key) { mutableListOf() }

        synchronized(list) {
            val lastIdx = list.lastIndex

            if (lastIdx >= 0) {
                val lastCandle = list[lastIdx]
                if (lastCandle.timestamp == incoming.timestamp) {
                    // Update the currently forming candle
                    list[lastIdx] = incoming
                } else if (incoming.timestamp > lastCandle.timestamp) {
                    // Previous candle has closed; append new forming candle
                    list.add(incoming)
                    // Keep memory buffer reasonable
                    if (list.size > 500) {
                        list.removeAt(0)
                    }
                }
            } else {
                list.add(incoming)
            }
        }

        // Emit price tick for live price badge and price alerts
        val sym = getSymbol(event.symbol)
        val oldPrice = sym?.currentPrice ?: incoming.open
        _priceTicks.tryEmit(
            PriceTick(
                symbol = event.symbol,
                price = incoming.close,
                previousPrice = oldPrice,
                timestamp = event.eventTime
            )
        )

        updateSymbolPrice(event.symbol, incoming.close)
    }

    private fun updateSymbolPrice(symbol: String, price: Double) {
        _symbols.value = _symbols.value.map {
            if (it.symbol == symbol) {
                val high = if (it.high24h > 0) max(it.high24h, price) else price
                val low = if (it.low24h > 0) min(it.low24h, price) else price
                it.copy(
                    currentPrice = price,
                    high24h = high,
                    low24h = low
                )
            } else it
        }
    }

    /**
     * Refreshes depth / order book for active symbol
     */
    suspend fun refreshOrderBook(symbol: String) = withContext(Dispatchers.IO) {
        val result = dataProvider.fetchOrderBook(symbol, limit = 20)
        if (result.isSuccess) {
            _orderBook.value = result.getOrNull()
        }
    }

    /**
     * Reconnects live feeds manually
     */
    fun retryConnection() {
        val sym = currentStreamSymbol ?: "BTC/USDT"
        val tf = currentStreamTimeframe ?: Timeframe.M15
        scope.launch {
            loadHistoricalCandles(sym, tf, forceRefresh = true)
        }
    }
}
