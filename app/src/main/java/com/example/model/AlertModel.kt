package com.example.model

enum class AlertCondition(val label: String, val shortDesc: String) {
    CROSSING("Crossing", "Price crosses target"),
    CROSSING_UP("Crossing Up", "Price moves up past target"),
    CROSSING_DOWN("Crossing Down", "Price drops below target"),
    GREATER_THAN("Greater Than", "Price is above target"),
    LESS_THAN("Less Than", "Price is below target")
}

data class PriceAlert(
    val id: Long = 0,
    val symbol: String,
    val targetPrice: Double,
    val condition: AlertCondition,
    val note: String = "",
    val isOnlyOnce: Boolean = true,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val triggeredAt: Long? = null,
    val triggeredPrice: Double? = null
) {
    val isTriggered: Boolean get() = triggeredAt != null

    /**
     * Checks if current price update triggers this alert given the previous price.
     */
    fun isConditionMet(currentPrice: Double, previousPrice: Double): Boolean {
        if (!isActive) return false
        return when (condition) {
            AlertCondition.CROSSING -> {
                (previousPrice < targetPrice && currentPrice >= targetPrice) ||
                (previousPrice > targetPrice && currentPrice <= targetPrice)
            }
            AlertCondition.CROSSING_UP -> {
                previousPrice < targetPrice && currentPrice >= targetPrice
            }
            AlertCondition.CROSSING_DOWN -> {
                previousPrice > targetPrice && currentPrice <= targetPrice
            }
            AlertCondition.GREATER_THAN -> {
                currentPrice >= targetPrice
            }
            AlertCondition.LESS_THAN -> {
                currentPrice <= targetPrice
            }
        }
    }
}
