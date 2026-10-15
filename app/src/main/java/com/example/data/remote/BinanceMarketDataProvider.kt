package com.example.data.remote

import android.util.Log
import com.example.model.Candle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min

class BinanceMarketDataProvider(
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : MarketDataProvider {

    override val name: String = "Binance"

    private val _connectionStatus = MutableStateFlow<ConnectionStatus>(ConnectionStatus.Disconnected)
    override val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _lastUpdateTimestamp = MutableStateFlow<Long?>(null)
    override val lastUpdateTimestamp: StateFlow<Long?> = _lastUpdateTimestamp.asStateFlow()

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    // Host list with public mirrors to handle any regional blocks
    private val restHosts = listOf(
        "https://api.binance.com",
        "https://data-api.binance.vision",
        "https://api1.binance.com",
        "https://api3.binance.com"
    )

    private val wsHosts = listOf(
        "wss://stream.binance.com:9443/ws",
        "wss://data-stream.binance.vision/ws"
    )

    private var activeWebSocket: WebSocket? = null
    private var streamJob: Job? = null
    private var currentStreamSymbol: String? = null
    private var currentStreamInterval: String? = null

    private val _klineStreamFlow = MutableSharedFlow<KlineStreamEvent>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    companion object {
        private const val TAG = "BinanceProvider"
        private const val MAX_BACKOFF_MS = 30000L
        private const val BASE_BACKOFF_MS = 1000L

        fun cleanSymbol(rawSymbol: String): String {
            return rawSymbol.replace("/", "").replace("-", "").replace("_", "").uppercase()
        }
    }

    override suspend fun fetchHistoricalCandles(
        symbol: String,
        interval: String,
        limit: Int,
        startTime: Long?,
        endTime: Long?
    ): Result<List<Candle>> = withContext(Dispatchers.IO) {
        val formattedSymbol = cleanSymbol(symbol)
        var lastException: Exception? = null

        for (host in restHosts) {
            try {
                val urlBuilder = StringBuilder("$host/api/v3/klines?")
                urlBuilder.append("symbol=$formattedSymbol")
                urlBuilder.append("&interval=$interval")
                urlBuilder.append("&limit=${limit.coerceIn(10, 1000)}")
                if (startTime != null && startTime > 0) {
                    urlBuilder.append("&startTime=$startTime")
                }
                if (endTime != null && endTime > 0) {
                    urlBuilder.append("&endTime=$endTime")
                }

                val request = Request.Builder()
                    .url(urlBuilder.toString())
                    .addHeader("User-Agent", "TradeAlert/1.0 (Android)")
                    .addHeader("Accept", "application/json")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val body = response.body?.string() ?: ""
                        throw IOException("HTTP ${response.code} from $host: $body")
                    }

                    val jsonString = response.body?.string()
                        ?: throw IOException("Empty response body from $host")

                    val jsonArray = JSONArray(jsonString)
                    val candles = ArrayList<Candle>(jsonArray.length())

                    for (i in 0 until jsonArray.length()) {
                        val item = jsonArray.getJSONArray(i)
                        // Binance kline structure:
                        // 0: Open time (Long)
                        // 1: Open (String)
                        // 2: High (String)
                        // 3: Low (String)
                        // 4: Close (String)
                        // 5: Volume (String)
                        val timestamp = item.getLong(0)
                        val open = item.getString(1).toDoubleOrNull() ?: 0.0
                        val high = item.getString(2).toDoubleOrNull() ?: 0.0
                        val low = item.getString(3).toDoubleOrNull() ?: 0.0
                        val close = item.getString(4).toDoubleOrNull() ?: 0.0
                        val volume = item.getString(5).toDoubleOrNull() ?: 0.0

                        candles.add(
                            Candle(
                                timestamp = timestamp,
                                open = open,
                                high = high,
                                low = low,
                                close = close,
                                volume = volume
                            )
                        )
                    }

                    _lastUpdateTimestamp.value = System.currentTimeMillis()
                    return@withContext Result.success(candles)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed fetch historical klines from $host for $symbol: ${e.message}")
                lastException = e
            }
        }

        Result.failure(lastException ?: IOException("All Binance hosts failed to respond"))
    }

    override suspend fun fetch24hTicker(symbol: String): Result<Binance24hTicker> = withContext(Dispatchers.IO) {
        val formattedSymbol = cleanSymbol(symbol)
        var lastException: Exception? = null

        for (host in restHosts) {
            try {
                val url = "$host/api/v3/ticker/24hr?symbol=$formattedSymbol"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "TradeAlert/1.0 (Android)")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code} from $host")
                    val body = response.body?.string() ?: throw IOException("Empty body")
                    val obj = JSONObject(body)

                    val ticker = Binance24hTicker(
                        symbol = symbol,
                        lastPrice = obj.optString("lastPrice", "0").toDoubleOrNull() ?: 0.0,
                        priceChange = obj.optString("priceChange", "0").toDoubleOrNull() ?: 0.0,
                        priceChangePercent = obj.optString("priceChangePercent", "0").toDoubleOrNull() ?: 0.0,
                        highPrice = obj.optString("highPrice", "0").toDoubleOrNull() ?: 0.0,
                        lowPrice = obj.optString("lowPrice", "0").toDoubleOrNull() ?: 0.0,
                        volume = obj.optString("volume", "0").toDoubleOrNull() ?: 0.0,
                        quoteVolume = obj.optString("quoteVolume", "0").toDoubleOrNull() ?: 0.0
                    )
                    _lastUpdateTimestamp.value = System.currentTimeMillis()
                    return@withContext Result.success(ticker)
                }
            } catch (e: Exception) {
                lastException = e
            }
        }
        Result.failure(lastException ?: IOException("Failed to fetch 24h ticker for $symbol"))
    }

    override suspend fun fetchAll24hTickers(): Result<List<Binance24hTicker>> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        for (host in restHosts) {
            try {
                val url = "$host/api/v3/ticker/24hr"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "TradeAlert/1.0 (Android)")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code} from $host")
                    val body = response.body?.string() ?: throw IOException("Empty body")
                    val arr = JSONArray(body)
                    val result = ArrayList<Binance24hTicker>(arr.length())

                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val sym = obj.optString("symbol")
                        result.add(
                            Binance24hTicker(
                                symbol = sym,
                                lastPrice = obj.optString("lastPrice", "0").toDoubleOrNull() ?: 0.0,
                                priceChange = obj.optString("priceChange", "0").toDoubleOrNull() ?: 0.0,
                                priceChangePercent = obj.optString("priceChangePercent", "0").toDoubleOrNull() ?: 0.0,
                                highPrice = obj.optString("highPrice", "0").toDoubleOrNull() ?: 0.0,
                                lowPrice = obj.optString("lowPrice", "0").toDoubleOrNull() ?: 0.0,
                                volume = obj.optString("volume", "0").toDoubleOrNull() ?: 0.0,
                                quoteVolume = obj.optString("quoteVolume", "0").toDoubleOrNull() ?: 0.0
                            )
                        )
                    }
                    _lastUpdateTimestamp.value = System.currentTimeMillis()
                    return@withContext Result.success(result)
                }
            } catch (e: Exception) {
                lastException = e
            }
        }
        Result.failure(lastException ?: IOException("Failed to fetch 24h tickers"))
    }

    override suspend fun fetchOrderBook(symbol: String, limit: Int): Result<OrderBookData> = withContext(Dispatchers.IO) {
        val formattedSymbol = cleanSymbol(symbol)
        var lastException: Exception? = null

        for (host in restHosts) {
            try {
                val url = "$host/api/v3/depth?symbol=$formattedSymbol&limit=${limit.coerceIn(5, 100)}"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("User-Agent", "TradeAlert/1.0 (Android)")
                    .get()
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("HTTP ${response.code} from $host")
                    val body = response.body?.string() ?: throw IOException("Empty body")
                    val obj = JSONObject(body)

                    val bidsArray = obj.getJSONArray("bids")
                    val asksArray = obj.getJSONArray("asks")

                    val bids = ArrayList<OrderBookEntry>(bidsArray.length())
                    for (i in 0 until bidsArray.length()) {
                        val entry = bidsArray.getJSONArray(i)
                        val price = entry.getString(0).toDoubleOrNull() ?: 0.0
                        val qty = entry.getString(1).toDoubleOrNull() ?: 0.0
                        bids.add(OrderBookEntry(price = price, quantity = qty))
                    }

                    val asks = ArrayList<OrderBookEntry>(asksArray.length())
                    for (i in 0 until asksArray.length()) {
                        val entry = asksArray.getJSONArray(i)
                        val price = entry.getString(0).toDoubleOrNull() ?: 0.0
                        val qty = entry.getString(1).toDoubleOrNull() ?: 0.0
                        asks.add(OrderBookEntry(price = price, quantity = qty))
                    }

                    val orderBook = OrderBookData(
                        symbol = symbol,
                        bids = bids,
                        asks = asks,
                        lastUpdateId = obj.optLong("lastUpdateId", 0L),
                        timestamp = System.currentTimeMillis()
                    )
                    _lastUpdateTimestamp.value = System.currentTimeMillis()
                    return@withContext Result.success(orderBook)
                }
            } catch (e: Exception) {
                lastException = e
            }
        }
        Result.failure(lastException ?: IOException("Failed to fetch depth for $symbol"))
    }

    override fun startKlineStream(symbol: String, interval: String): Flow<KlineStreamEvent> {
        val cleanSym = cleanSymbol(symbol).lowercase()

        // If already connected to this exact stream, return existing flow
        if (currentStreamSymbol == cleanSym && currentStreamInterval == interval && activeWebSocket != null) {
            return _klineStreamFlow.asSharedFlow()
        }

        stopKlineStream()
        currentStreamSymbol = cleanSym
        currentStreamInterval = interval

        streamJob = scope.launch {
            var attempt = 0
            while (isActive) {
                val streamPath = "${cleanSym}@kline_${interval}"
                val host = wsHosts[attempt % wsHosts.size]
                val wsUrl = "$host/$streamPath"

                _connectionStatus.value = if (attempt == 0) {
                    ConnectionStatus.Connecting
                } else {
                    val delayMs = min(MAX_BACKOFF_MS, BASE_BACKOFF_MS * (1L shl min(attempt, 5)))
                    ConnectionStatus.Reconnecting(attempt, delayMs)
                }

                val connectedSuccessfully = connectWebSocket(wsUrl, symbol, interval)

                if (!isActive) break

                if (!connectedSuccessfully) {
                    attempt++
                    val backoff = min(MAX_BACKOFF_MS, BASE_BACKOFF_MS * (1L shl min(attempt, 5)))
                    Log.d(TAG, "WebSocket stream failed, reconnecting in ${backoff}ms (attempt $attempt)")
                    delay(backoff)
                } else {
                    // Reset attempt counter after stable connection
                    attempt = 0
                }
            }
        }

        return _klineStreamFlow.asSharedFlow()
    }

    private suspend fun connectWebSocket(url: String, symbol: String, interval: String): Boolean {
        return kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
            val request = Request.Builder()
                .url(url)
                .addHeader("User-Agent", "TradeAlert/1.0 (Android)")
                .build()

            var isResumed = false

            val listener = object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    Log.i(TAG, "WebSocket opened: $url")
                    activeWebSocket = webSocket
                    _connectionStatus.value = ConnectionStatus.Connected
                    _lastUpdateTimestamp.value = System.currentTimeMillis()
                    if (!isResumed) {
                        isResumed = true
                        continuation.resume(true) {}
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val root = JSONObject(text)
                        val eventType = root.optString("e")
                        if (eventType == "kline") {
                            val klineObj = root.getJSONObject("k")
                            val openTime = klineObj.getLong("t")
                            val open = klineObj.getString("o").toDoubleOrNull() ?: 0.0
                            val close = klineObj.getString("c").toDoubleOrNull() ?: 0.0
                            val high = klineObj.getString("h").toDoubleOrNull() ?: 0.0
                            val low = klineObj.getString("l").toDoubleOrNull() ?: 0.0
                            val volume = klineObj.getString("v").toDoubleOrNull() ?: 0.0
                            val isClosed = klineObj.getBoolean("x")

                            val candle = Candle(
                                timestamp = openTime,
                                open = open,
                                high = high,
                                low = low,
                                close = close,
                                volume = volume
                            )

                            _lastUpdateTimestamp.value = System.currentTimeMillis()
                            _klineStreamFlow.tryEmit(
                                KlineStreamEvent(
                                    symbol = symbol,
                                    interval = interval,
                                    candle = candle,
                                    isClosed = isClosed,
                                    eventTime = root.optLong("E", System.currentTimeMillis())
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing WS message: ${e.message}")
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.w(TAG, "WebSocket failure: ${t.message}")
                    _connectionStatus.value = ConnectionStatus.Error(t.message ?: "Connection lost")
                    if (!isResumed) {
                        isResumed = true
                        continuation.resume(false) {}
                    }
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    Log.i(TAG, "WebSocket closed: $code / $reason")
                    _connectionStatus.value = ConnectionStatus.Disconnected
                }
            }

            val ws = client.newWebSocket(request, listener)
            activeWebSocket = ws

            continuation.invokeOnCancellation {
                ws.cancel()
            }
        }
    }

    override fun stopKlineStream() {
        streamJob?.cancel()
        streamJob = null
        activeWebSocket?.close(1000, "Client switched symbol/timeframe")
        activeWebSocket = null
        currentStreamSymbol = null
        currentStreamInterval = null
        _connectionStatus.value = ConnectionStatus.Disconnected
    }
}
