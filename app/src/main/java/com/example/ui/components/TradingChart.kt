package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BollingerBandPoint
import com.example.model.Candle
import com.example.model.ChartStyle
import com.example.model.IndicatorSettings
import com.example.model.PriceAlert
import com.example.model.SymbolInfo
import com.example.model.TechnicalIndicatorCalculator
import com.example.model.Timeframe
import com.example.ui.theme.TvAlertAmber
import com.example.ui.theme.TvAlertAmberBg
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvBearRed
import com.example.ui.theme.TvBrandBlue
import com.example.ui.theme.TvBullGreen
import com.example.ui.theme.TvDivider
import com.example.ui.theme.TvIndicatorCyan
import com.example.ui.theme.TvIndicatorOrange
import com.example.ui.theme.TvIndicatorPurple
import com.example.ui.theme.TvIndicatorYellow
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextMuted
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvTextWhite
import kotlin.math.max
import kotlin.math.min

@Composable
fun TradingChart(
    symbolInfo: SymbolInfo,
    candles: List<Candle>,
    timeframe: Timeframe,
    chartStyle: ChartStyle,
    indicatorSettings: IndicatorSettings,
    activeAlerts: List<PriceAlert>,
    onAddAlertAtPrice: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    if (candles.isEmpty()) {
        Box(
            modifier = modifier.background(TvBackground),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Loading chart data...",
                color = TvTextSecondary,
                fontSize = 14.sp
            )
        }
        return
    }

    val view = LocalView.current

    // Scrub state
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubIndex by remember { mutableIntStateOf(-1) }
    var scrubTouchY by remember { mutableStateOf(0f) }

    // Pulsing animation for live price dot
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Calculate indicators
    val ema20 = remember(candles, indicatorSettings.showEma20) {
        if (indicatorSettings.showEma20) TechnicalIndicatorCalculator.calculateEma(candles, 20) else emptyList()
    }
    val ema50 = remember(candles, indicatorSettings.showEma50) {
        if (indicatorSettings.showEma50) TechnicalIndicatorCalculator.calculateEma(candles, 50) else emptyList()
    }
    val bollingerBands = remember(candles, indicatorSettings.showBollingerBands) {
        if (indicatorSettings.showBollingerBands) TechnicalIndicatorCalculator.calculateBollingerBands(candles) else emptyList()
    }
    val rsi = remember(candles, indicatorSettings.showRsi) {
        if (indicatorSettings.showRsi) TechnicalIndicatorCalculator.calculateRsi(candles, 14) else emptyList()
    }

    val displayedCandle = if (isScrubbing && scrubIndex in candles.indices) {
        candles[scrubIndex]
    } else {
        candles.lastOrNull() ?: candles.first()
    }

    Column(modifier = modifier.background(TvBackground)) {
        // Top OHLCV Legend / HUD Bar
        OhlcvLegendBar(
            candle = displayedCandle,
            timeframe = timeframe,
            symbolInfo = symbolInfo,
            isScrubbing = isScrubbing,
            indicatorSettings = indicatorSettings,
            ema20Val = if (isScrubbing && scrubIndex in ema20.indices) ema20[scrubIndex] else ema20.lastOrNull(),
            ema50Val = if (isScrubbing && scrubIndex in ema50.indices) ema50[scrubIndex] else ema50.lastOrNull(),
            rsiVal = if (isScrubbing && scrubIndex in rsi.indices) rsi[scrubIndex] else rsi.lastOrNull()
        )

        // Main Chart Canvas with gesture detection
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val chartWidth = constraints.maxWidth.toFloat()
            val chartHeight = constraints.maxHeight.toFloat()

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(candles.size) {
                        detectTapGestures(
                            onTap = { offset ->
                                isScrubbing = false
                            },
                            onLongPress = { offset ->
                                isScrubbing = true
                                scrubTouchY = offset.y
                                val rightAxisWidth = 65.dp.toPx()
                                val usableWidth = chartWidth - rightAxisWidth
                                val candleWidth = usableWidth / candles.size
                                val idx = (offset.x / candleWidth).toInt().coerceIn(0, candles.lastIndex)
                                if (idx != scrubIndex) {
                                    scrubIndex = idx
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                            }
                        )
                    }
                    .pointerInput(candles.size) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                isScrubbing = true
                                scrubTouchY = offset.y
                                val rightAxisWidth = 65.dp.toPx()
                                val usableWidth = chartWidth - rightAxisWidth
                                val candleWidth = usableWidth / candles.size
                                val idx = (offset.x / candleWidth).toInt().coerceIn(0, candles.lastIndex)
                                scrubIndex = idx
                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                            },
                            onDrag = { change, _ ->
                                change.consume()
                                scrubTouchY = change.position.y
                                val rightAxisWidth = 65.dp.toPx()
                                val usableWidth = chartWidth - rightAxisWidth
                                val candleWidth = usableWidth / candles.size
                                val idx = (change.position.x / candleWidth).toInt().coerceIn(0, candles.lastIndex)
                                if (idx != scrubIndex) {
                                    scrubIndex = idx
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            },
                            onDragEnd = {
                                // Keep crosshair active briefly or tap to dismiss
                            },
                            onDragCancel = {
                                isScrubbing = false
                            }
                        )
                    }
            ) {
                drawTradingChart(
                    candles = candles,
                    chartStyle = chartStyle,
                    timeframe = timeframe,
                    symbolInfo = symbolInfo,
                    indicatorSettings = indicatorSettings,
                    ema20 = ema20,
                    ema50 = ema50,
                    bollingerBands = bollingerBands,
                    rsi = rsi,
                    activeAlerts = activeAlerts,
                    isScrubbing = isScrubbing,
                    scrubIndex = scrubIndex,
                    scrubTouchY = scrubTouchY,
                    pulseAlpha = pulseAlpha
                )
            }

            // Quick Floating "Add Alert at Crosshair" Button if scrubbing
            if (isScrubbing && scrubTouchY > 40f && scrubTouchY < chartHeight - 60f) {
                val usableHeight = if (indicatorSettings.showRsi) chartHeight * 0.72f else chartHeight - 40f
                val (minPrice, maxPrice) = calculatePriceRange(candles, activeAlerts)
                val priceAtTouch = maxPrice - (scrubTouchY / usableHeight) * (maxPrice - minPrice)

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = TvAlertAmber,
                    shadowElevation = 6.dp,
                    onClick = {
                        onAddAlertAtPrice(priceAtTouch)
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 74.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "🔔 Set Alert @ ${symbolInfo.formatPrice(priceAtTouch)}",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}

private fun calculatePriceRange(candles: List<Candle>, activeAlerts: List<PriceAlert>): Pair<Double, Double> {
    var minPrice = candles.minOfOrNull { it.low } ?: 0.0
    var maxPrice = candles.maxOfOrNull { it.high } ?: 100.0

    // Include alerts in price scale if reasonable
    activeAlerts.forEach { alert ->
        if (alert.targetPrice > 0) {
            val margin = (maxPrice - minPrice) * 0.3
            if (alert.targetPrice >= minPrice - margin && alert.targetPrice <= maxPrice + margin) {
                minPrice = min(minPrice, alert.targetPrice)
                maxPrice = max(maxPrice, alert.targetPrice)
            }
        }
    }

    val padding = (maxPrice - minPrice) * 0.08
    return Pair(minPrice - padding, maxPrice + padding)
}

private fun DrawScope.drawTradingChart(
    candles: List<Candle>,
    chartStyle: ChartStyle,
    timeframe: Timeframe,
    symbolInfo: SymbolInfo,
    indicatorSettings: IndicatorSettings,
    ema20: List<Double?>,
    ema50: List<Double?>,
    bollingerBands: List<BollingerBandPoint?>,
    rsi: List<Double?>,
    activeAlerts: List<PriceAlert>,
    isScrubbing: Boolean,
    scrubIndex: Int,
    scrubTouchY: Float,
    pulseAlpha: Float
) {
    val totalWidth = size.width
    val totalHeight = size.height

    val rightAxisWidth = 65.dp.toPx()
    val bottomTimeAxisHeight = 26.dp.toPx()
    val usableWidth = totalWidth - rightAxisWidth

    // Split heights for main price area and RSI sub-pane if active
    val hasRsi = indicatorSettings.showRsi && rsi.isNotEmpty()
    val rsiPaneHeight = if (hasRsi) totalHeight * 0.22f else 0f
    val rsiTop = totalHeight - bottomTimeAxisHeight - rsiPaneHeight

    val mainChartHeight = totalHeight - bottomTimeAxisHeight - rsiPaneHeight
    val volumeHeight = if (indicatorSettings.showVolume) mainChartHeight * 0.18f else 0f
    val priceAreaHeight = mainChartHeight - (if (indicatorSettings.showVolume) volumeHeight * 0.5f else 0f)

    val (minPrice, maxPrice) = calculatePriceRange(candles, activeAlerts)
    val priceSpan = max(0.0001, maxPrice - minPrice)

    fun priceToY(price: Double): Float {
        val normalized = (maxPrice - price) / priceSpan
        return (normalized * (mainChartHeight - 20f) + 10f).toFloat().coerceIn(0f, mainChartHeight)
    }

    fun yToPrice(y: Float): Double {
        val norm = (y - 10f) / (mainChartHeight - 20f)
        return maxPrice - (norm * priceSpan)
    }

    val candleStep = usableWidth / candles.size
    val candleWidth = max(2f, candleStep * 0.72f)

    // 1. Draw Grid Lines
    val gridLinesCount = 5
    for (i in 0..gridLinesCount) {
        val y = 15f + i * (mainChartHeight - 30f) / gridLinesCount
        drawLine(
            color = TvDivider,
            start = Offset(0f, y),
            end = Offset(usableWidth, y),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
        )

        // Draw Right Axis Price Label
        val priceVal = yToPrice(y)
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#787B86")
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.LEFT
                isAntiAlias = true
            }
            drawText(
                symbolInfo.formatPrice(priceVal),
                usableWidth + 8f,
                y + 4.dp.toPx(),
                paint
            )
        }
    }

    // Vertical time gridlines
    val timeSteps = 4
    for (i in 1..timeSteps) {
        val candleIdx = (i * (candles.size - 1) / (timeSteps + 1)).coerceIn(0, candles.lastIndex)
        val x = candleIdx * candleStep + candleStep / 2f
        drawLine(
            color = TvDivider,
            start = Offset(x, 0f),
            end = Offset(x, mainChartHeight),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
        )

        // Bottom time label
        val c = candles[candleIdx]
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#787B86")
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawText(
                c.formattedTime(timeframe),
                x,
                totalHeight - 8.dp.toPx(),
                paint
            )
        }
    }

    // Divider line between chart and right price axis
    drawLine(
        color = TvSurfaceVariant,
        start = Offset(usableWidth, 0f),
        end = Offset(usableWidth, totalHeight),
        strokeWidth = 1.5f
    )

    // Divider line between main chart and bottom axis
    drawLine(
        color = TvSurfaceVariant,
        start = Offset(0f, mainChartHeight),
        end = Offset(totalWidth, mainChartHeight),
        strokeWidth = 1.5f
    )

    // 2. Draw Volume Bars at bottom of main chart
    if (indicatorSettings.showVolume) {
        val maxVolume = max(1.0, candles.maxOfOrNull { it.volume } ?: 1.0)
        val volumeBaseY = mainChartHeight

        candles.forEachIndexed { i, candle ->
            val barHeight = ((candle.volume / maxVolume) * volumeHeight).toFloat()
            val barTop = volumeBaseY - barHeight
            val barX = i * candleStep + (candleStep - candleWidth) / 2f
            val color = if (candle.isBullish) TvBullGreen.copy(alpha = 0.35f) else TvBearRed.copy(alpha = 0.35f)

            drawRect(
                color = color,
                topLeft = Offset(barX, barTop),
                size = Size(candleWidth, barHeight)
            )
        }
    }

    // 3. Draw Bollinger Bands
    if (indicatorSettings.showBollingerBands && bollingerBands.isNotEmpty()) {
        val upperPath = Path()
        val lowerPath = Path()
        val fillPath = Path()
        var started = false

        for (i in candles.indices) {
            val point = bollingerBands.getOrNull(i) ?: continue
            val x = i * candleStep + candleStep / 2f
            val yUpper = priceToY(point.upper)
            val yLower = priceToY(point.lower)

            if (!started) {
                upperPath.moveTo(x, yUpper)
                lowerPath.moveTo(x, yLower)
                fillPath.moveTo(x, yUpper)
                started = true
            } else {
                upperPath.lineTo(x, yUpper)
                lowerPath.lineTo(x, yLower)
                fillPath.lineTo(x, yUpper)
            }
        }

        // Complete fill path backwards
        for (i in candles.indices.reversed()) {
            val point = bollingerBands.getOrNull(i) ?: continue
            val x = i * candleStep + candleStep / 2f
            val yLower = priceToY(point.lower)
            fillPath.lineTo(x, yLower)
        }
        fillPath.close()

        drawPath(fillPath, color = TvIndicatorCyan.copy(alpha = 0.08f))
        drawPath(upperPath, color = TvIndicatorCyan.copy(alpha = 0.6f), style = Stroke(width = 1.2f))
        drawPath(lowerPath, color = TvIndicatorCyan.copy(alpha = 0.6f), style = Stroke(width = 1.2f))
    }

    // 4. Draw Price Data: Candlesticks OR Line / Area
    when (chartStyle) {
        ChartStyle.CANDLES -> {
            candles.forEachIndexed { i, candle ->
                val centerX = i * candleStep + candleStep / 2f
                val openY = priceToY(candle.open)
                val closeY = priceToY(candle.close)
                val highY = priceToY(candle.high)
                val lowY = priceToY(candle.low)

                val candleColor = if (candle.isBullish) TvBullGreen else TvBearRed

                // Draw Wick
                drawLine(
                    color = candleColor,
                    start = Offset(centerX, highY),
                    end = Offset(centerX, lowY),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round
                )

                // Draw Candle Body
                val bodyTop = min(openY, closeY)
                val bodyHeight = max(2f, kotlin.math.abs(closeY - openY))
                val bodyLeft = centerX - (candleWidth / 2f)

                drawRect(
                    color = candleColor,
                    topLeft = Offset(bodyLeft, bodyTop),
                    size = Size(candleWidth, bodyHeight)
                )
            }
        }
        ChartStyle.LINE, ChartStyle.AREA -> {
            val linePath = Path()
            val areaPath = Path()
            var startX = 0f

            candles.forEachIndexed { i, candle ->
                val x = i * candleStep + candleStep / 2f
                val y = priceToY(candle.close)
                if (i == 0) {
                    startX = x
                    linePath.moveTo(x, y)
                    areaPath.moveTo(x, mainChartHeight)
                    areaPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    areaPath.lineTo(x, y)
                }
            }

            if (candles.isNotEmpty()) {
                val lastX = (candles.size - 1) * candleStep + candleStep / 2f
                areaPath.lineTo(lastX, mainChartHeight)
                areaPath.close()

                if (chartStyle == ChartStyle.AREA) {
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                TvBrandBlue.copy(alpha = 0.35f),
                                TvBrandBlue.copy(alpha = 0.02f)
                            ),
                            startY = 0f,
                            endY = mainChartHeight
                        )
                    )
                }

                drawPath(
                    path = linePath,
                    color = TvBrandBlue,
                    style = Stroke(width = 2.2f, cap = StrokeCap.Round)
                )
            }
        }
    }

    // 5. Draw EMA Overlays
    if (indicatorSettings.showEma20 && ema20.isNotEmpty()) {
        drawIndicatorLine(ema20, candleStep, ::priceToY, TvIndicatorYellow)
    }
    if (indicatorSettings.showEma50 && ema50.isNotEmpty()) {
        drawIndicatorLine(ema50, candleStep, ::priceToY, TvIndicatorOrange)
    }

    // 6. Draw Horizontal Active Alert Lines (TradingView signature feature!)
    activeAlerts.forEach { alert ->
        val alertY = priceToY(alert.targetPrice)
        if (alertY in 0f..mainChartHeight) {
            // Dashed horizontal alert line across chart
            drawLine(
                color = TvAlertAmber,
                start = Offset(0f, alertY),
                end = Offset(usableWidth, alertY),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )

            // Alert Badge on Right Y-Axis
            val tagHeight = 18.dp.toPx()
            val tagWidth = rightAxisWidth - 6f
            val tagTop = alertY - tagHeight / 2f
            val tagLeft = usableWidth + 2f

            drawRoundRect(
                color = TvAlertAmber,
                topLeft = Offset(tagLeft, tagTop),
                size = Size(tagWidth, tagHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            drawContext.canvas.nativeCanvas.apply {
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.BLACK
                    textSize = 9.sp.toPx()
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawText(
                    "🔔 " + symbolInfo.formatPrice(alert.targetPrice),
                    tagLeft + tagWidth / 2f,
                    tagTop + 12.dp.toPx(),
                    paint
                )
            }
        }
    }

    // 7. Draw Live Current Price Line & Right Badge
    val currentPrice = symbolInfo.currentPrice
    val currentPriceY = priceToY(currentPrice)
    val priceBadgeColor = if (symbolInfo.isPositive) TvBullGreen else TvBearRed

    // Horizontal dashed live price line
    drawLine(
        color = priceBadgeColor.copy(alpha = 0.85f),
        start = Offset(0f, currentPriceY),
        end = Offset(usableWidth, currentPriceY),
        strokeWidth = 1.2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
    )

    // Pulsing price dot at the right end of chart
    val lastCandleX = (candles.size - 1) * candleStep + candleStep / 2f
    drawCircle(
        color = priceBadgeColor.copy(alpha = pulseAlpha * 0.4f),
        radius = 8.dp.toPx(),
        center = Offset(lastCandleX, currentPriceY)
    )
    drawCircle(
        color = priceBadgeColor,
        radius = 3.5.dp.toPx(),
        center = Offset(lastCandleX, currentPriceY)
    )

    // Current Price Badge on Right Y-Axis
    val badgeHeight = 20.dp.toPx()
    val badgeWidth = rightAxisWidth - 4f
    val badgeTop = currentPriceY - badgeHeight / 2f
    val badgeLeft = usableWidth + 2f

    drawRoundRect(
        color = priceBadgeColor,
        topLeft = Offset(badgeLeft, badgeTop),
        size = Size(badgeWidth, badgeHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
    )

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = 10.sp.toPx()
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
        drawText(
            symbolInfo.formatPrice(currentPrice),
            badgeLeft + badgeWidth / 2f,
            badgeTop + 14.dp.toPx(),
            paint
        )
    }

    // 8. Draw RSI Sub-Pane if active
    if (hasRsi) {
        drawLine(
            color = TvSurfaceVariant,
            start = Offset(0f, rsiTop),
            end = Offset(totalWidth, rsiTop),
            strokeWidth = 1.5f
        )

        // RSI 70 overbought and 30 oversold reference lines
        fun rsiValToY(rsiVal: Double): Float {
            val norm = (100.0 - rsiVal) / 100.0
            return (rsiTop + norm * rsiPaneHeight).toFloat().coerceIn(rsiTop, rsiTop + rsiPaneHeight)
        }

        val y70 = rsiValToY(70.0)
        val y30 = rsiValToY(30.0)

        // Shaded region between 30 and 70
        drawRect(
            color = TvIndicatorPurple.copy(alpha = 0.05f),
            topLeft = Offset(0f, y70),
            size = Size(usableWidth, y30 - y70)
        )

        drawLine(
            color = TvIndicatorPurple.copy(alpha = 0.4f),
            start = Offset(0f, y70),
            end = Offset(usableWidth, y70),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
        )
        drawLine(
            color = TvIndicatorPurple.copy(alpha = 0.4f),
            start = Offset(0f, y30),
            end = Offset(usableWidth, y30),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
        )

        // Draw RSI line
        val rsiPath = Path()
        var rsiStarted = false
        candles.forEachIndexed { i, _ ->
            val valRsi = rsi.getOrNull(i) ?: return@forEachIndexed
            val x = i * candleStep + candleStep / 2f
            val y = rsiValToY(valRsi)
            if (!rsiStarted) {
                rsiPath.moveTo(x, y)
                rsiStarted = true
            } else {
                rsiPath.lineTo(x, y)
            }
        }
        drawPath(rsiPath, color = TvIndicatorPurple, style = Stroke(width = 1.8f))

        // Axis labels for 70 and 30
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#787B86")
                textSize = 8.sp.toPx()
                textAlign = android.graphics.Paint.Align.LEFT
                isAntiAlias = true
            }
            drawText("70", usableWidth + 6f, y70 + 3.dp.toPx(), paint)
            drawText("30", usableWidth + 6f, y30 + 3.dp.toPx(), paint)
            drawText("RSI (14)", 10f, rsiTop + 14.dp.toPx(), paint)
        }
    }

    // 9. Draw Crosshair and Scrub Tag
    if (isScrubbing && scrubIndex in candles.indices) {
        val crossX = scrubIndex * candleStep + candleStep / 2f
        val boundedY = scrubTouchY.coerceIn(0f, mainChartHeight)

        // Vertical crosshair line
        drawLine(
            color = TvTextPrimary.copy(alpha = 0.65f),
            start = Offset(crossX, 0f),
            end = Offset(crossX, totalHeight - bottomTimeAxisHeight),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
        )

        // Horizontal crosshair line
        drawLine(
            color = TvTextPrimary.copy(alpha = 0.65f),
            start = Offset(0f, boundedY),
            end = Offset(usableWidth, boundedY),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
        )

        // Price pill at crosshair Y on right axis
        val scrubbedPrice = yToPrice(boundedY)
        val pillHeight = 20.dp.toPx()
        val pillWidth = rightAxisWidth - 4f
        val pillTop = boundedY - pillHeight / 2f

        drawRoundRect(
            color = TvTextPrimary,
            topLeft = Offset(usableWidth + 2f, pillTop),
            size = Size(pillWidth, pillHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 9.sp.toPx()
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawText(
                symbolInfo.formatPrice(scrubbedPrice),
                usableWidth + 2f + pillWidth / 2f,
                pillTop + 13.dp.toPx(),
                paint
            )
        }

        // Time pill at bottom
        val selectedCandle = candles[scrubIndex]
        val timePillWidth = 60.dp.toPx()
        val timePillHeight = 18.dp.toPx()
        val timePillLeft = (crossX - timePillWidth / 2f).coerceIn(4f, usableWidth - timePillWidth - 4f)
        val timePillTop = totalHeight - bottomTimeAxisHeight + 4f

        drawRoundRect(
            color = TvSurfaceVariant,
            topLeft = Offset(timePillLeft, timePillTop),
            size = Size(timePillWidth, timePillHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx())
        )

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawText(
                selectedCandle.formattedTime(timeframe),
                timePillLeft + timePillWidth / 2f,
                timePillTop + 12.dp.toPx(),
                paint
            )
        }
    }
}

private fun DrawScope.drawIndicatorLine(
    values: List<Double?>,
    candleStep: Float,
    priceToY: (Double) -> Float,
    color: Color
) {
    val path = Path()
    var started = false

    values.forEachIndexed { i, value ->
        if (value != null) {
            val x = i * candleStep + candleStep / 2f
            val y = priceToY(value)
            if (!started) {
                path.moveTo(x, y)
                started = true
            } else {
                path.lineTo(x, y)
            }
        }
    }

    if (started) {
        drawPath(path, color = color, style = Stroke(width = 1.6f, cap = StrokeCap.Round))
    }
}

@Composable
private fun OhlcvLegendBar(
    candle: Candle,
    timeframe: Timeframe,
    symbolInfo: SymbolInfo,
    isScrubbing: Boolean,
    indicatorSettings: IndicatorSettings,
    ema20Val: Double?,
    ema50Val: Double?,
    rsiVal: Double?
) {
    val changeColor = if (candle.isBullish) TvBullGreen else TvBearRed
    val sign = if (candle.change >= 0) "+" else ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TvSurface)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "${symbolInfo.symbol} • ${timeframe.label}",
                color = TvTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "O",
                color = TvTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = " ${symbolInfo.formatPrice(candle.open)}",
                color = TvTextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "H",
                color = TvTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = " ${symbolInfo.formatPrice(candle.high)}",
                color = TvTextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "L",
                color = TvTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = " ${symbolInfo.formatPrice(candle.low)}",
                color = TvTextPrimary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(6.dp))

            Text(
                text = "C",
                color = TvTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = " ${symbolInfo.formatPrice(candle.close)}",
                color = changeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "$sign${String.format("%.2f", candle.changePercent)}%",
                color = changeColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Second row: Active indicator readouts (TradingView style)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        ) {
            if (indicatorSettings.showVolume) {
                Text(
                    text = "Vol: ",
                    color = TvTextSecondary,
                    fontSize = 10.sp
                )
                Text(
                    text = String.format("%.1fK", candle.volume / 1000.0),
                    color = changeColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            if (indicatorSettings.showEma20 && ema20Val != null) {
                Text(
                    text = "EMA 20: ",
                    color = TvIndicatorYellow,
                    fontSize = 10.sp
                )
                Text(
                    text = symbolInfo.formatPrice(ema20Val),
                    color = TvTextPrimary,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            if (indicatorSettings.showEma50 && ema50Val != null) {
                Text(
                    text = "EMA 50: ",
                    color = TvIndicatorOrange,
                    fontSize = 10.sp
                )
                Text(
                    text = symbolInfo.formatPrice(ema50Val),
                    color = TvTextPrimary,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.width(10.dp))
            }

            if (indicatorSettings.showRsi && rsiVal != null) {
                Text(
                    text = "RSI 14: ",
                    color = TvIndicatorPurple,
                    fontSize = 10.sp
                )
                Text(
                    text = String.format("%.1f", rsiVal),
                    color = TvTextPrimary,
                    fontSize = 10.sp
                )
            }
        }
    }
}
