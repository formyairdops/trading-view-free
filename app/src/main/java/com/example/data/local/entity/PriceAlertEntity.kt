package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.AlertCondition
import com.example.model.PriceAlert

@Entity(tableName = "price_alerts")
data class PriceAlertEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val symbol: String,
    val targetPrice: Double,
    val condition: String,
    val note: String,
    val isOnlyOnce: Boolean,
    val isActive: Boolean,
    val createdAt: Long,
    val triggeredAt: Long?,
    val triggeredPrice: Double?
) {
    fun toDomain(): PriceAlert {
        return PriceAlert(
            id = id,
            symbol = symbol,
            targetPrice = targetPrice,
            condition = try { AlertCondition.valueOf(condition) } catch (e: Exception) { AlertCondition.CROSSING },
            note = note,
            isOnlyOnce = isOnlyOnce,
            isActive = isActive,
            createdAt = createdAt,
            triggeredAt = triggeredAt,
            triggeredPrice = triggeredPrice
        )
    }

    companion object {
        fun fromDomain(domain: PriceAlert): PriceAlertEntity {
            return PriceAlertEntity(
                id = domain.id,
                symbol = domain.symbol,
                targetPrice = domain.targetPrice,
                condition = domain.condition.name,
                note = domain.note,
                isOnlyOnce = domain.isOnlyOnce,
                isActive = domain.isActive,
                createdAt = domain.createdAt,
                triggeredAt = domain.triggeredAt,
                triggeredPrice = domain.triggeredPrice
            )
        }
    }
}
