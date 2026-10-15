package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.remote.ConnectionStatus
import com.example.data.remote.OrderBookData
import com.example.data.repository.AlertRepository
import com.example.data.repository.MarketRepository
import com.example.model.ChartDrawing
import com.example.model.ChartStyle
import com.example.model.DrawingToolType
import com.example.model.IndicatorSettings
import com.example.model.PriceAlert
import com.example.model.SymbolInfo
import com.example.model.Timeframe
import com.example.ui.components.AlertTriggerEvent
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val alertRepository = AlertRepository(database.priceAlertDao(), viewModelScope)
    private val marketRepository = MarketRepository(viewModelScope)

    val symbols: StateFlow<List<SymbolInfo>> = marketRepository.symbols
    val connectionStatus: StateFlow<ConnectionStatus> = marketRepository.connectionStatus
    val lastUpdateTimestamp: StateFlow<Long?> = marketRepository.lastUpdateTimestamp
    val isLoadingCandles: StateFlow<Boolean> = marketRepository.isLoadingCandles
    val candleLoadError: StateFlow<String?> = marketRepository.candleLoadError
    val orderBook: StateFlow<OrderBookData?> = marketRepository.orderBook

    val allAlerts: StateFlow<List<PriceAlert>> = alertRepository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSymbolCode = MutableStateFlow("BTC/USDT")
    val selectedSymbolCode: StateFlow<String> = _selectedSymbolCode.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow(Timeframe.M15)
    val selectedTimeframe: StateFlow<Timeframe> = _selectedTimeframe.asStateFlow()

    private val _chartStyle = MutableStateFlow(ChartStyle.CANDLES)
    val chartStyle: StateFlow<ChartStyle> = _chartStyle.asStateFlow()

    private val _currentCandles = MutableStateFlow<List<com.example.model.Candle>>(emptyList())
    val currentCandles: StateFlow<List<com.example.model.Candle>> = _currentCandles.asStateFlow()

    private val _indicatorSettings = MutableStateFlow(
        IndicatorSettings(
            showEma20 = true,
            showEma50 = false,
            showBollingerBands = false,
            showRsi = false,
            showVolume = true
        )
    )
    val indicatorSettings: StateFlow<IndicatorSettings> = _indicatorSettings.asStateFlow()

    // Chart Drawing Tools state
    private val _activeDrawingTool = MutableStateFlow(DrawingToolType.NONE)
    val activeDrawingTool: StateFlow<DrawingToolType> = _activeDrawingTool.asStateFlow()

    private val _drawings = MutableStateFlow<Map<String, List<ChartDrawing>>>(emptyMap())

    val currentSymbolDrawings: StateFlow<List<ChartDrawing>> = combine(
        _drawings,
        _selectedSymbolCode
    ) { map, sym ->
        map[sym].orEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _latestTriggerEvent = MutableStateFlow<AlertTriggerEvent?>(null)
    val latestTriggerEvent: StateFlow<AlertTriggerEvent?> = _latestTriggerEvent.asStateFlow()

    private var loadCandlesJob: Job? = null
    private var orderBookPollingJob: Job? = null

    // Derived current selected SymbolInfo
    val currentSymbolInfo: StateFlow<SymbolInfo> = combine(symbols, _selectedSymbolCode) { list, code ->
        list.find { it.symbol == code } ?: list.firstOrNull() ?: SymbolInfo(
            symbol = "BTC/USDT",
            name = "Bitcoin",
            category = com.example.model.AssetCategory.CRYPTO,
            currentPrice = 0.0,
            previousClose = 0.0,
            high24h = 0.0,
            low24h = 0.0,
            volume24h = 0.0
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SymbolInfo(
            symbol = "BTC/USDT",
            name = "Bitcoin",
            category = com.example.model.AssetCategory.CRYPTO,
            currentPrice = 0.0,
            previousClose = 0.0,
            high24h = 0.0,
            low24h = 0.0,
            volume24h = 0.0
        )
    )

    init {
        // Initial load of BTC/USDT historical candles from real Binance public market data
        loadCurrentCandles()

        // Monitor incoming price ticks to evaluate alerts and update active candle in UI
        viewModelScope.launch {
            marketRepository.priceTicks.collect { tick ->
                if (tick.symbol == _selectedSymbolCode.value) {
                    // Update currentCandles from cache
                    val updated = marketRepository.getCandles(tick.symbol, _selectedTimeframe.value)
                    if (updated.isNotEmpty()) {
                        _currentCandles.value = updated
                    }
                }
                checkAlertTriggers(tick.symbol, tick.price, tick.previousPrice)
            }
        }

        // Start Order Book polling
        startOrderBookPolling()
    }

    private fun startOrderBookPolling() {
        orderBookPollingJob?.cancel()
        orderBookPollingJob = viewModelScope.launch {
            while (isActive) {
                marketRepository.refreshOrderBook(_selectedSymbolCode.value)
                delay(3000) // update order book every 3 seconds
            }
        }
    }

    private fun loadCurrentCandles(forceRefresh: Boolean = false) {
        loadCandlesJob?.cancel()
        val symbol = _selectedSymbolCode.value
        val tf = _selectedTimeframe.value

        loadCandlesJob = viewModelScope.launch {
            val result = marketRepository.loadHistoricalCandles(symbol, tf, forceRefresh)
            if (result.isSuccess) {
                _currentCandles.value = result.getOrNull().orEmpty()
            }
        }
    }

    private suspend fun checkAlertTriggers(symbol: String, currentPrice: Double, previousPrice: Double) {
        val currentAlerts = allAlerts.value
        val activeForSymbol = currentAlerts.filter {
            it.symbol == symbol && it.isActive && !it.isTriggered
        }

        for (alert in activeForSymbol) {
            if (alert.isConditionMet(currentPrice, previousPrice)) {
                alertRepository.markAlertTriggered(alert, currentPrice)
                _latestTriggerEvent.value = AlertTriggerEvent(
                    alert = alert,
                    triggerPrice = currentPrice
                )
            }
        }
    }

    fun selectSymbol(symbol: String) {
        if (_selectedSymbolCode.value != symbol) {
            _selectedSymbolCode.value = symbol
            loadCurrentCandles()
            startOrderBookPolling()
        }
    }

    fun selectTimeframe(timeframe: Timeframe) {
        if (_selectedTimeframe.value != timeframe) {
            _selectedTimeframe.value = timeframe
            loadCurrentCandles()
        }
    }

    fun refreshChartData() {
        loadCurrentCandles(forceRefresh = true)
        marketRepository.retryConnection()
    }

    fun setChartStyle(style: ChartStyle) {
        _chartStyle.value = style
    }

    fun updateIndicatorSettings(settings: IndicatorSettings) {
        _indicatorSettings.value = settings
    }

    fun toggleFavorite(symbol: String) {
        marketRepository.toggleFavorite(symbol)
    }

    fun setActiveDrawingTool(tool: DrawingToolType) {
        _activeDrawingTool.value = tool
    }

    fun addDrawing(drawing: ChartDrawing) {
        val current = _drawings.value.toMutableMap()
        val list = current[drawing.symbol].orEmpty().toMutableList()
        list.add(drawing)
        current[drawing.symbol] = list
        _drawings.value = current
    }

    fun deleteDrawing(id: String) {
        val sym = _selectedSymbolCode.value
        val current = _drawings.value.toMutableMap()
        val list = current[sym].orEmpty().filter { it.id != id }
        current[sym] = list
        _drawings.value = current
    }

    fun clearCurrentDrawings() {
        val sym = _selectedSymbolCode.value
        val current = _drawings.value.toMutableMap()
        current.remove(sym)
        _drawings.value = current
    }

    fun createAlert(alert: PriceAlert) {
        viewModelScope.launch {
            alertRepository.createAlert(alert)
        }
    }

    fun toggleAlertActive(alert: PriceAlert) {
        viewModelScope.launch {
            alertRepository.toggleAlertActive(alert)
        }
    }

    fun deleteAlert(alertId: Long) {
        viewModelScope.launch {
            alertRepository.deleteAlert(alertId)
        }
    }

    fun clearTriggeredHistory() {
        viewModelScope.launch {
            alertRepository.clearTriggeredHistory()
        }
    }

    fun dismissTriggerEvent() {
        _latestTriggerEvent.value = null
    }
}
