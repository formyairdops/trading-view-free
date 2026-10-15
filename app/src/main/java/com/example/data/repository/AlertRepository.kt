package com.example.data.repository

import com.example.data.local.dao.PriceAlertDao
import com.example.data.local.entity.PriceAlertEntity
import com.example.model.AlertCondition
import com.example.model.PriceAlert
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlertRepository(
    private val alertDao: PriceAlertDao,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    val allAlerts: Flow<List<PriceAlert>> = alertDao.getAllAlerts().map { list ->
        list.map { it.toDomain() }
    }

    init {
        // Pre-populate default alerts if empty
        scope.launch {
            val existing = alertDao.getAllAlerts().first()
            if (existing.isEmpty()) {
                seedInitialAlerts()
            }
        }
    }

    private suspend fun seedInitialAlerts() {
        val initialAlerts = listOf(
            PriceAlert(
                symbol = "BTC/USDT",
                targetPrice = 68000.00,
                condition = AlertCondition.CROSSING_UP,
                note = "Breakout above $68k resistance",
                isOnlyOnce = true,
                isActive = true
            ),
            PriceAlert(
                symbol = "BTC/USDT",
                targetPrice = 66500.00,
                condition = AlertCondition.CROSSING_DOWN,
                note = "Dip buy support check",
                isOnlyOnce = false,
                isActive = true
            ),
            PriceAlert(
                symbol = "ETH/USDT",
                targetPrice = 3600.00,
                condition = AlertCondition.GREATER_THAN,
                note = "Target 1 hit take-profit",
                isOnlyOnce = true,
                isActive = true
            ),
            PriceAlert(
                symbol = "NVDA",
                targetPrice = 140.00,
                condition = AlertCondition.CROSSING_UP,
                note = "All-time high watch",
                isOnlyOnce = true,
                isActive = true
            )
        )

        initialAlerts.forEach { alert ->
            alertDao.insertAlert(PriceAlertEntity.fromDomain(alert))
        }
    }

    fun getActiveAlertsForSymbol(symbol: String): Flow<List<PriceAlert>> {
        return alertDao.getActiveAlertsForSymbol(symbol).map { list ->
            list.map { it.toDomain() }
        }
    }

    suspend fun createAlert(alert: PriceAlert): Long = withContext(Dispatchers.IO) {
        alertDao.insertAlert(PriceAlertEntity.fromDomain(alert))
    }

    suspend fun updateAlert(alert: PriceAlert) = withContext(Dispatchers.IO) {
        alertDao.updateAlert(PriceAlertEntity.fromDomain(alert))
    }

    suspend fun toggleAlertActive(alert: PriceAlert) = withContext(Dispatchers.IO) {
        alertDao.updateAlert(PriceAlertEntity.fromDomain(alert.copy(isActive = !alert.isActive)))
    }

    suspend fun markAlertTriggered(alert: PriceAlert, triggeredPrice: Double) = withContext(Dispatchers.IO) {
        val updated = alert.copy(
            isActive = !alert.isOnlyOnce, // Deactivate if Only Once
            triggeredAt = System.currentTimeMillis(),
            triggeredPrice = triggeredPrice
        )
        alertDao.updateAlert(PriceAlertEntity.fromDomain(updated))
    }

    suspend fun deleteAlert(alertId: Long) = withContext(Dispatchers.IO) {
        alertDao.deleteAlertById(alertId)
    }

    suspend fun clearTriggeredHistory() = withContext(Dispatchers.IO) {
        alertDao.clearTriggeredHistory()
    }
}
