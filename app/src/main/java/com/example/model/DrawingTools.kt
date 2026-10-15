package com.example.model

enum class DrawingToolType(val label: String, val iconDescription: String) {
    NONE("Cursor", "Default chart pointer"),
    TREND_LINE("Trendline", "Two-point trend line"),
    HORIZONTAL_LINE("Horizontal", "Horizontal price level"),
    VERTICAL_LINE("Vertical", "Vertical time level"),
    RAY("Ray", "Extended ray line"),
    RECTANGLE("Box", "Support / Resistance zone box"),
    FIBONACCI_RETRACEMENT("Fibonacci", "Fib retracement levels (0 to 1.0)")
}

data class ChartPoint(
    val timestamp: Long,
    val price: Double
)

data class ChartDrawing(
    val id: String = java.util.UUID.randomUUID().toString(),
    val symbol: String,
    val type: DrawingToolType,
    val points: List<ChartPoint>,
    val colorHex: String = "#2962FF",
    val strokeWidth: Float = 2.0f,
    val isLocked: Boolean = false
) {
    companion object {
        val FIBONACCI_LEVELS = listOf(
            0.0 to "#787B86",
            0.236 to "#F23645",
            0.382 to "#FF9800",
            0.5 to "#089981",
            0.618 to "#2962FF",
            0.786 to "#9C27B0",
            1.0 to "#787B86"
        )
    }
}
