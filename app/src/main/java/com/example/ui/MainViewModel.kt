package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.AlertRepository
import com.example.data.repository.MarketRepository
import com.example.model.ChartStyle
import com.example.model.IndicatorSettings
import com.example.model.PriceAlert
import com.example.model.SymbolInfo
import com.example.model.Timeframe
import com.example.ui.components.AlertTriggerEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val alertRepository = AlertRepository(database.priceAlertDao(), viewModelScope)
    private val marketRepository = MarketRepository(viewModelScope)

    val symbols: StateFlow<List<SymbolInfo>> = marketRepository.symbols

    val allAlerts: StateFlow<List<PriceAlert>> = alertRepository.allAlerts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSymbolCode = MutableStateFlow("BTC/USDT")
    val selectedSymbolCode: StateFlow<String> = _selectedSymbolCode.asStateFlow()

    private val _selectedTimeframe = MutableStateFlow(Timeframe.M15)
    val selectedTimeframe: StateFlow<Timeframe> = _selectedTimeframe.asStateFlow()

    private val _chartStyle = MutableStateFlow(ChartStyle.CANDLES)
    val chartStyle: StateFlow<ChartStyle> = _chartStyle.asStateFlow()

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

    private val _latestTriggerEvent = MutableStateFlow<AlertTriggerEvent?>(null)
    val latestTriggerEvent: StateFlow<AlertTriggerEvent?> = _latestTriggerEvent.asStateFlow()

    // Derived current selected SymbolInfo
    val currentSymbolInfo: StateFlow<SymbolInfo> = combine(symbols, _selectedSymbolCode) { list, code ->
        list.find { it.symbol == code } ?: list.firstOrNull() ?: SymbolInfo(
            symbol = "BTC/USDT",
            name = "Bitcoin",
            category = com.example.model.AssetCategory.CRYPTO,
            currentPrice = 67450.0,
            previousClose = 66120.0,
            high24h = 68150.0,
            low24h = 65890.0,
            volume24h = 42890000000.0
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        SymbolInfo(
            symbol = "BTC/USDT",
            name = "Bitcoin",
            category = com.example.model.AssetCategory.CRYPTO,
            currentPrice = 67450.0,
            previousClose = 66120.0,
            high24h = 68150.0,
            low24h = 65890.0,
            volume24h = 42890000000.0
        )
    )

    init {
        // Monitor price ticks and evaluate alerts
        viewModelScope.launch {
            marketRepository.priceTicks.collect { tick ->
                checkAlertTriggers(tick.symbol, tick.price, tick.previousPrice)
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
                // Trigger alert in DB
                alertRepository.markAlertTriggered(alert, currentPrice)

                // Emit event for UI notification & haptics
                _latestTriggerEvent.value = AlertTriggerEvent(
                    alert = alert,
                    triggerPrice = currentPrice
                )
            }
        }
    }

    fun getCandlesForCurrentSymbol(): List<com.example.model.Candle> {
        return marketRepository.getCandles(_selectedSymbolCode.value, _selectedTimeframe.value)
    }

    fun selectSymbol(symbol: String) {
        _selectedSymbolCode.value = symbol
    }

    fun selectTimeframe(timeframe: Timeframe) {
        _selectedTimeframe.value = timeframe
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

    fun triggerPriceShock(percent: Double) {
        marketRepository.triggerPriceShock(_selectedSymbolCode.value, percent)
    }

    fun dismissTriggerEvent() {
        _latestTriggerEvent.value = null
    }
}
