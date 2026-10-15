package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.ConnectionStatus
import com.example.model.BollingerBandPoint
import com.example.model.Candle
import com.example.model.ChartDrawing
import com.example.model.ChartPoint
import com.example.model.ChartStyle
import com.example.model.DrawingToolType
import com.example.model.IndicatorSettings
import com.example.model.MacdPoint
import com.example.model.PriceAlert
import com.example.model.StochRsiPoint
import com.example.model.SupertrendPoint
import com.example.model.SymbolInfo
import com.example.model.TechnicalIndicatorCalculator
import com.example.model.Timeframe
import com.example.ui.theme.TvAlertAmber
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
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvTextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun TradingChart(
    symbolInfo: SymbolInfo,
    candles: List<Candle>,
    timeframe: Timeframe,
    chartStyle: ChartStyle,
    indicatorSettings: IndicatorSettings,
    activeAlerts: List<PriceAlert>,
    activeDrawingTool: DrawingToolType,
    drawings: List<ChartDrawing>,
    connectionStatus: ConnectionStatus,
    lastUpdateTimestamp: Long?,
    isLoading: Boolean,
    errorMessage: String?,
    onAddDrawing: (ChartDrawing) -> Unit,
    onDeleteDrawing: (String) -> Unit,
    onAddAlertAtPrice: (Double) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading && candles.isEmpty()) {
        Box(
            modifier = modifier.background(TvBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = TvBrandBlue, modifier = Modifier.size(36.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Connecting to ${symbolInfo.symbol} market data...",
                    color = TvTextSecondary,
                    fontSize = 13.sp
                )
            }
        }
        return
    }

    if (errorMessage != null && candles.isEmpty()) {
        Box(
            modifier = modifier.background(TvBackground),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "⚠️ Unable to load market data",
                    color = TvBearRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = errorMessage,
                    color = TvTextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TvBrandBlue,
                    onClick = onRetry
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Retry", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry Connection", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
        return
    }

    if (candles.isEmpty()) {
        Box(modifier = modifier.background(TvBackground), contentAlignment = Alignment.Center) {
            Text(text = "No candles available for ${symbolInfo.symbol}", color = TvTextSecondary, fontSize = 13.sp)
        }
        return
    }

    val view = LocalView.current

    // Viewport State: zoom level & horizontal scroll offset
    var visibleCandleCount by remember { mutableFloatStateOf(45f) }
    var scrollOffsetIndex by remember { mutableIntStateOf(0) } // 0 = at rightmost/latest candle

    // Scrub / Crosshair state
    var isScrubbing by remember { mutableStateOf(false) }
    var scrubIndexInVisible by remember { mutableIntStateOf(-1) }
    var scrubTouchY by remember { mutableFloatStateOf(0f) }

    // Drawing tool in-progress points
    var pendingPoints by remember { mutableStateOf<List<ChartPoint>>(emptyList()) }

    // Pulsing price dot animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    // Calculate Indicators for full dataset
    val sma = remember(candles, indicatorSettings.showSma, indicatorSettings.smaPeriod) {
        if (indicatorSettings.showSma) TechnicalIndicatorCalculator.calculateSma(candles, indicatorSettings.smaPeriod) else emptyList()
    }
    val ema20 = remember(candles, indicatorSettings.showEma20, indicatorSettings.ema20Period) {
        if (indicatorSettings.showEma20) TechnicalIndicatorCalculator.calculateEma(candles, indicatorSettings.ema20Period) else emptyList()
    }
    val ema50 = remember(candles, indicatorSettings.showEma50, indicatorSettings.ema50Period) {
        if (indicatorSettings.showEma50) TechnicalIndicatorCalculator.calculateEma(candles, indicatorSettings.ema50Period) else emptyList()
    }
    val vwap = remember(candles, indicatorSettings.showVwap) {
        if (indicatorSettings.showVwap) TechnicalIndicatorCalculator.calculateVwap(candles) else emptyList()
    }
    val bollingerBands = remember(candles, indicatorSettings.showBollingerBands) {
        if (indicatorSettings.showBollingerBands) TechnicalIndicatorCalculator.calculateBollingerBands(candles, indicatorSettings.bbPeriod, indicatorSettings.bbStdDev) else emptyList()
    }
    val supertrend = remember(candles, indicatorSettings.showSupertrend) {
        if (indicatorSettings.showSupertrend) TechnicalIndicatorCalculator.calculateSupertrend(candles, indicatorSettings.supertrendAtrPeriod, indicatorSettings.supertrendMultiplier) else emptyList()
    }
    val rsi = remember(candles, indicatorSettings.showRsi) {
        if (indicatorSettings.showRsi) TechnicalIndicatorCalculator.calculateRsi(candles, indicatorSettings.rsiPeriod) else emptyList()
    }
    val macd = remember(candles, indicatorSettings.showMacd) {
        if (indicatorSettings.showMacd) TechnicalIndicatorCalculator.calculateMacd(candles, indicatorSettings.macdFastPeriod, indicatorSettings.macdSlowPeriod, indicatorSettings.macdSignalPeriod) else emptyList()
    }
    val atr = remember(candles, indicatorSettings.showAtr) {
        if (indicatorSettings.showAtr) TechnicalIndicatorCalculator.calculateAtr(candles, indicatorSettings.atrPeriod) else emptyList()
    }
    val stochRsi = remember(candles, indicatorSettings.showStochRsi) {
        if (indicatorSettings.showStochRsi) TechnicalIndicatorCalculator.calculateStochRsi(candles, indicatorSettings.stochRsiPeriod) else emptyList()
    }

    // Determine slice of visible candles based on zoom & pan
    val count = visibleCandleCount.roundToInt().coerceIn(15, min(candles.size, 180))
    val maxScrollOffset = max(0, candles.size - count)
    val clampedScrollOffset = scrollOffsetIndex.coerceIn(0, maxScrollOffset)

    val startIndex = max(0, candles.size - clampedScrollOffset - count)
    val endIndex = min(candles.size, startIndex + count)
    val visibleCandles = candles.subList(startIndex, endIndex)

    val displayedGlobalIdx = if (isScrubbing && scrubIndexInVisible in visibleCandles.indices) {
        startIndex + scrubIndexInVisible
    } else {
        candles.lastIndex
    }

    val displayedCandle = candles.getOrNull(displayedGlobalIdx) ?: candles.last()

    Column(modifier = modifier.background(TvBackground)) {
        // Status Bar: Connection Status & Real-time timestamp
        ConnectionHudBar(
            connectionStatus = connectionStatus,
            lastUpdateTimestamp = lastUpdateTimestamp,
            onRefresh = onRetry
        )

        // Top OHLCV HUD bar
        OhlcvHudBar(
            candle = displayedCandle,
            timeframe = timeframe,
            symbolInfo = symbolInfo,
            indicatorSettings = indicatorSettings,
            ema20Val = ema20.getOrNull(displayedGlobalIdx),
            ema50Val = ema50.getOrNull(displayedGlobalIdx),
            vwapVal = vwap.getOrNull(displayedGlobalIdx),
            rsiVal = rsi.getOrNull(displayedGlobalIdx),
            macdVal = macd.getOrNull(displayedGlobalIdx),
            supertrendVal = supertrend.getOrNull(displayedGlobalIdx)
        )

        // Main Chart Canvas with multi-touch zoom, pan, scrub, and drawing
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val density = LocalDensity.current
            val chartWidth = constraints.maxWidth.toFloat()
            val chartHeight = constraints.maxHeight.toFloat()
            val rightAxisWidth = with(density) { 65.dp.toPx() }
            val bottomTimeAxisHeight = with(density) { 26.dp.toPx() }
            val usableWidth = chartWidth - rightAxisWidth
            val mainHeight = chartHeight - bottomTimeAxisHeight

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(visibleCandles.size, activeDrawingTool) {
                        // Pinch to zoom & drag to pan
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (activeDrawingTool == DrawingToolType.NONE && !isScrubbing) {
                                // Zoom
                                if (zoom != 1f) {
                                    val newZoom = (visibleCandleCount / zoom).coerceIn(15f, 150f)
                                    visibleCandleCount = newZoom
                                }
                                // Pan
                                if (pan.x != 0f) {
                                    val candleStep = usableWidth / visibleCandles.size
                                    val deltaCandles = (pan.x / candleStep).roundToInt()
                                    scrollOffsetIndex = (scrollOffsetIndex + deltaCandles).coerceIn(0, max(0, candles.size - visibleCandleCount.roundToInt()))
                                }
                            }
                        }
                    }
                    .pointerInput(visibleCandles.size, activeDrawingTool) {
                        detectTapGestures(
                            onTap = { offset ->
                                isScrubbing = false
                                if (activeDrawingTool != DrawingToolType.NONE && offset.x < usableWidth) {
                                    val candleStep = usableWidth / visibleCandles.size
                                    val idxInVis = (offset.x / candleStep).toInt().coerceIn(0, visibleCandles.lastIndex)
                                    val globalIdx = startIndex + idxInVis
                                    val targetCandle = candles[globalIdx]
                                    val (minPrice, maxPrice) = calculateVisiblePriceRange(visibleCandles, activeAlerts)
                                    val priceSpan = max(0.0001, maxPrice - minPrice)
                                    val norm = (offset.y - 10f) / (mainHeight - 20f)
                                    val price = maxPrice - (norm * priceSpan)

                                    val newPt = ChartPoint(timestamp = targetCandle.timestamp, price = price)
                                    val updatedPts = pendingPoints + newPt

                                    val requiredPts = when (activeDrawingTool) {
                                        DrawingToolType.HORIZONTAL_LINE, DrawingToolType.VERTICAL_LINE -> 1
                                        else -> 2
                                    }

                                    if (updatedPts.size >= requiredPts) {
                                        onAddDrawing(
                                            ChartDrawing(
                                                symbol = symbolInfo.symbol,
                                                type = activeDrawingTool,
                                                points = updatedPts,
                                                colorHex = if (activeDrawingTool == DrawingToolType.FIBONACCI_RETRACEMENT) "#FF9800" else "#2962FF"
                                            )
                                        )
                                        pendingPoints = emptyList()
                                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    } else {
                                        pendingPoints = updatedPts
                                        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                    }
                                }
                            },
                            onLongPress = { offset ->
                                if (activeDrawingTool == DrawingToolType.NONE) {
                                    isScrubbing = true
                                    scrubTouchY = offset.y
                                    val candleStep = usableWidth / visibleCandles.size
                                    scrubIndexInVisible = (offset.x / candleStep).toInt().coerceIn(0, visibleCandles.lastIndex)
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                }
                            }
                        )
                    }
                    .pointerInput(visibleCandles.size, activeDrawingTool) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                if (activeDrawingTool == DrawingToolType.NONE) {
                                    isScrubbing = true
                                    scrubTouchY = offset.y
                                    val candleStep = usableWidth / visibleCandles.size
                                    scrubIndexInVisible = (offset.x / candleStep).toInt().coerceIn(0, visibleCandles.lastIndex)
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                }
                            },
                            onDrag = { change, _ ->
                                if (activeDrawingTool == DrawingToolType.NONE && isScrubbing) {
                                    change.consume()
                                    scrubTouchY = change.position.y
                                    val candleStep = usableWidth / visibleCandles.size
                                    val newIdx = (change.position.x / candleStep).toInt().coerceIn(0, visibleCandles.lastIndex)
                                    if (newIdx != scrubIndexInVisible) {
                                        scrubIndexInVisible = newIdx
                                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    }
                                }
                            },
                            onDragEnd = { /* keep crosshair until dismissed */ },
                            onDragCancel = { isScrubbing = false }
                        )
                    }
            ) {
                drawTradingChartContent(
                    allCandles = candles,
                    visibleCandles = visibleCandles,
                    startIndex = startIndex,
                    chartStyle = chartStyle,
                    timeframe = timeframe,
                    symbolInfo = symbolInfo,
                    indicatorSettings = indicatorSettings,
                    sma = sma,
                    ema20 = ema20,
                    ema50 = ema50,
                    vwap = vwap,
                    bollingerBands = bollingerBands,
                    supertrend = supertrend,
                    rsi = rsi,
                    macd = macd,
                    atr = atr,
                    stochRsi = stochRsi,
                    activeAlerts = activeAlerts,
                    drawings = drawings,
                    pendingPoints = pendingPoints,
                    activeDrawingTool = activeDrawingTool,
                    isScrubbing = isScrubbing,
                    scrubIndexInVisible = scrubIndexInVisible,
                    scrubTouchY = scrubTouchY,
                    pulseAlpha = pulseAlpha
                )
            }

            // "Return to Latest Candle" Button (TradingView signature)
            if (clampedScrollOffset > 0) {
                Surface(
                    shape = CircleShape,
                    color = TvSurfaceVariant,
                    shadowElevation = 4.dp,
                    onClick = {
                        scrollOffsetIndex = 0
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 36.dp, end = 72.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("Latest", color = TvTextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Scroll to latest",
                            tint = TvBrandBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Set Alert at Crosshair Button
            if (isScrubbing && scrubTouchY > 30f && scrubTouchY < chartHeight - 50f) {
                val (minPrice, maxPrice) = calculateVisiblePriceRange(visibleCandles, activeAlerts)
                val priceSpan = max(0.0001, maxPrice - minPrice)
                val priceAtTouch = maxPrice - ((scrubTouchY - 10f) / (mainHeight - 20f)) * priceSpan

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = TvAlertAmber,
                    shadowElevation = 6.dp,
                    onClick = { onAddAlertAtPrice(priceAtTouch) },
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

            // Drawing Tool Instruction banner if tool is active
            if (activeDrawingTool != DrawingToolType.NONE) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TvBrandBlue.copy(alpha = 0.9f),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = if (pendingPoints.isEmpty()) "Tap chart to set 1st point for ${activeDrawingTool.label}" else "Tap chart to set 2nd point",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private fun calculateVisiblePriceRange(candles: List<Candle>, activeAlerts: List<PriceAlert>): Pair<Double, Double> {
    var minPrice = candles.minOfOrNull { it.low } ?: 0.0
    var maxPrice = candles.maxOfOrNull { it.high } ?: 100.0

    activeAlerts.forEach { alert ->
        if (alert.targetPrice > 0) {
            val margin = (maxPrice - minPrice) * 0.25
            if (alert.targetPrice in (minPrice - margin)..(maxPrice + margin)) {
                minPrice = min(minPrice, alert.targetPrice)
                maxPrice = max(maxPrice, alert.targetPrice)
            }
        }
    }

    val padding = max(0.0001, (maxPrice - minPrice) * 0.08)
    return Pair(minPrice - padding, maxPrice + padding)
}

private fun DrawScope.drawTradingChartContent(
    allCandles: List<Candle>,
    visibleCandles: List<Candle>,
    startIndex: Int,
    chartStyle: ChartStyle,
    timeframe: Timeframe,
    symbolInfo: SymbolInfo,
    indicatorSettings: IndicatorSettings,
    sma: List<Double?>,
    ema20: List<Double?>,
    ema50: List<Double?>,
    vwap: List<Double?>,
    bollingerBands: List<BollingerBandPoint?>,
    supertrend: List<SupertrendPoint?>,
    rsi: List<Double?>,
    macd: List<MacdPoint?>,
    atr: List<Double?>,
    stochRsi: List<StochRsiPoint?>,
    activeAlerts: List<PriceAlert>,
    drawings: List<ChartDrawing>,
    pendingPoints: List<ChartPoint>,
    activeDrawingTool: DrawingToolType,
    isScrubbing: Boolean,
    scrubIndexInVisible: Int,
    scrubTouchY: Float,
    pulseAlpha: Float
) {
    val totalWidth = size.width
    val totalHeight = size.height

    val rightAxisWidth = 65.dp.toPx()
    val bottomTimeAxisHeight = 24.dp.toPx()
    val usableWidth = totalWidth - rightAxisWidth

    // Split heights for oscillator sub-panes (RSI or MACD)
    val hasSubPane = indicatorSettings.showRsi || indicatorSettings.showMacd || indicatorSettings.showStochRsi || indicatorSettings.showAtr
    val subPaneHeight = if (hasSubPane) totalHeight * 0.22f else 0f
    val subPaneTop = totalHeight - bottomTimeAxisHeight - subPaneHeight

    val mainChartHeight = totalHeight - bottomTimeAxisHeight - subPaneHeight
    val volumeHeight = if (indicatorSettings.showVolume) mainChartHeight * 0.18f else 0f

    val (minPrice, maxPrice) = calculateVisiblePriceRange(visibleCandles, activeAlerts)
    val priceSpan = max(0.0001, maxPrice - minPrice)

    fun priceToY(price: Double): Float {
        val norm = (maxPrice - price) / priceSpan
        return (norm * (mainChartHeight - 20f) + 10f).toFloat().coerceIn(0f, mainChartHeight)
    }

    fun yToPrice(y: Float): Double {
        val norm = (y - 10f) / (mainChartHeight - 20f)
        return maxPrice - (norm * priceSpan)
    }

    val candleStep = usableWidth / visibleCandles.size
    val candleWidth = max(2.5f, candleStep * 0.72f)

    // 1. Horizontal Price Gridlines & Right Axis Labels
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

        val priceVal = yToPrice(y)
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#787B86")
                textSize = 10.sp.toPx()
                textAlign = android.graphics.Paint.Align.LEFT
                isAntiAlias = true
            }
            drawText(symbolInfo.formatPrice(priceVal), usableWidth + 6f, y + 4.dp.toPx(), paint)
        }
    }

    // 2. Vertical Time Gridlines & Bottom Axis Labels
    val timeSteps = 4
    for (i in 1..timeSteps) {
        val localIdx = (i * (visibleCandles.size - 1) / (timeSteps + 1)).coerceIn(0, visibleCandles.lastIndex)
        val x = localIdx * candleStep + candleStep / 2f
        drawLine(
            color = TvDivider,
            start = Offset(x, 0f),
            end = Offset(x, mainChartHeight),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
        )

        val c = visibleCandles[localIdx]
        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.parseColor("#787B86")
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawText(c.formattedTime(timeframe), x, totalHeight - 6.dp.toPx(), paint)
        }
    }

    // Axes Divider Lines
    drawLine(color = TvSurfaceVariant, start = Offset(usableWidth, 0f), end = Offset(usableWidth, totalHeight), strokeWidth = 1.5f)
    drawLine(color = TvSurfaceVariant, start = Offset(0f, mainChartHeight), end = Offset(totalWidth, mainChartHeight), strokeWidth = 1.5f)

    // 3. Volume Bars
    if (indicatorSettings.showVolume) {
        val maxVolume = max(1.0, visibleCandles.maxOfOrNull { it.volume } ?: 1.0)
        val volumeBaseY = mainChartHeight

        visibleCandles.forEachIndexed { i, candle ->
            val barHeight = ((candle.volume / maxVolume) * volumeHeight).toFloat()
            val barTop = volumeBaseY - barHeight
            val barX = i * candleStep + (candleStep - candleWidth) / 2f
            val color = if (candle.isBullish) TvBullGreen.copy(alpha = 0.35f) else TvBearRed.copy(alpha = 0.35f)

            drawRect(color = color, topLeft = Offset(barX, barTop), size = Size(candleWidth, barHeight))
        }
    }

    // 4. Bollinger Bands Overlay
    if (indicatorSettings.showBollingerBands && bollingerBands.isNotEmpty()) {
        val upperPath = Path()
        val lowerPath = Path()
        val fillPath = Path()
        var started = false

        visibleCandles.forEachIndexed { i, _ ->
            val globalIdx = startIndex + i
            val pt = bollingerBands.getOrNull(globalIdx) ?: return@forEachIndexed
            val x = i * candleStep + candleStep / 2f
            val yUpper = priceToY(pt.upper)
            val yLower = priceToY(pt.lower)

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

        if (started) {
            for (i in visibleCandles.indices.reversed()) {
                val globalIdx = startIndex + i
                val pt = bollingerBands.getOrNull(globalIdx) ?: continue
                val x = i * candleStep + candleStep / 2f
                val yLower = priceToY(pt.lower)
                fillPath.lineTo(x, yLower)
            }
            fillPath.close()

            drawPath(fillPath, color = TvIndicatorCyan.copy(alpha = 0.08f))
            drawPath(upperPath, color = TvIndicatorCyan.copy(alpha = 0.65f), style = Stroke(width = 1.2f))
            drawPath(lowerPath, color = TvIndicatorCyan.copy(alpha = 0.65f), style = Stroke(width = 1.2f))
        }
    }

    // 5. Candlesticks OR Line / Area
    when (chartStyle) {
        ChartStyle.CANDLES -> {
            visibleCandles.forEachIndexed { i, candle ->
                val centerX = i * candleStep + candleStep / 2f
                val openY = priceToY(candle.open)
                val closeY = priceToY(candle.close)
                val highY = priceToY(candle.high)
                val lowY = priceToY(candle.low)

                val candleColor = if (candle.isBullish) TvBullGreen else TvBearRed

                // Wick
                drawLine(
                    color = candleColor,
                    start = Offset(centerX, highY),
                    end = Offset(centerX, lowY),
                    strokeWidth = 1.5f,
                    cap = StrokeCap.Round
                )

                // Body
                val bodyTop = min(openY, closeY)
                val bodyHeight = max(2f, abs(closeY - openY))
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

            visibleCandles.forEachIndexed { i, candle ->
                val x = i * candleStep + candleStep / 2f
                val y = priceToY(candle.close)
                if (i == 0) {
                    linePath.moveTo(x, y)
                    areaPath.moveTo(x, mainChartHeight)
                    areaPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, y)
                    areaPath.lineTo(x, y)
                }
            }

            if (visibleCandles.isNotEmpty()) {
                val lastX = (visibleCandles.size - 1) * candleStep + candleStep / 2f
                areaPath.lineTo(lastX, mainChartHeight)
                areaPath.close()

                if (chartStyle == ChartStyle.AREA) {
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(TvBrandBlue.copy(alpha = 0.35f), TvBrandBlue.copy(alpha = 0.02f)),
                            startY = 0f,
                            endY = mainChartHeight
                        )
                    )
                }
                drawPath(path = linePath, color = TvBrandBlue, style = Stroke(width = 2.2f, cap = StrokeCap.Round))
            }
        }
    }

    // 6. Indicator Overlays: SMA, EMA20, EMA50, VWAP, Supertrend
    if (indicatorSettings.showSma && sma.isNotEmpty()) {
        drawIndicatorLineSlice(sma, startIndex, visibleCandles.size, candleStep, ::priceToY, Color(0xFF26A69A))
    }
    if (indicatorSettings.showEma20 && ema20.isNotEmpty()) {
        drawIndicatorLineSlice(ema20, startIndex, visibleCandles.size, candleStep, ::priceToY, TvIndicatorYellow)
    }
    if (indicatorSettings.showEma50 && ema50.isNotEmpty()) {
        drawIndicatorLineSlice(ema50, startIndex, visibleCandles.size, candleStep, ::priceToY, TvIndicatorOrange)
    }
    if (indicatorSettings.showVwap && vwap.isNotEmpty()) {
        drawIndicatorLineSlice(vwap, startIndex, visibleCandles.size, candleStep, ::priceToY, Color(0xFFE91E63))
    }

    // Supertrend Overlay
    if (indicatorSettings.showSupertrend && supertrend.isNotEmpty()) {
        visibleCandles.forEachIndexed { i, _ ->
            val globalIdx = startIndex + i
            val pt = supertrend.getOrNull(globalIdx) ?: return@forEachIndexed
            val prevPt = if (globalIdx > 0) supertrend.getOrNull(globalIdx - 1) else null
            if (prevPt != null && prevPt.isBullish == pt.isBullish) {
                val x1 = (i - 1) * candleStep + candleStep / 2f
                val y1 = priceToY(prevPt.value)
                val x2 = i * candleStep + candleStep / 2f
                val y2 = priceToY(pt.value)
                val color = if (pt.isBullish) TvBullGreen else TvBearRed
                drawLine(color = color, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = 2.2f)
            }
        }
    }

    // 7. Chart Drawings (Trendlines, Horizontal lines, Vertical lines, Rays, Boxes, Fibonacci)
    drawings.forEach { drawing ->
        drawChartDrawing(drawing, visibleCandles, startIndex, candleStep, ::priceToY, usableWidth, mainChartHeight)
    }

    // Draw active drawing in progress
    if (pendingPoints.isNotEmpty()) {
        pendingPoints.forEach { pt ->
            // Find candle index for timestamp
            val candleIdx = visibleCandles.indexOfFirst { it.timestamp == pt.timestamp }
            if (candleIdx >= 0) {
                val x = candleIdx * candleStep + candleStep / 2f
                val y = priceToY(pt.price)
                drawCircle(color = TvBrandBlue, radius = 6.dp.toPx(), center = Offset(x, y))
                drawCircle(color = Color.White, radius = 3.dp.toPx(), center = Offset(x, y))
            }
        }
    }

    // 8. Active Alerts Horizontal Lines on Chart
    activeAlerts.forEach { alert ->
        val alertY = priceToY(alert.targetPrice)
        if (alertY in 0f..mainChartHeight) {
            drawLine(
                color = TvAlertAmber,
                start = Offset(0f, alertY),
                end = Offset(usableWidth, alertY),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
            )

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
                drawText("🔔 " + symbolInfo.formatPrice(alert.targetPrice), tagLeft + tagWidth / 2f, tagTop + 12.dp.toPx(), paint)
            }
        }
    }

    // 9. Live Current Price Line & Pulsing Badge
    val currentPrice = symbolInfo.currentPrice
    if (currentPrice > 0) {
        val currentPriceY = priceToY(currentPrice)
        val priceBadgeColor = if (symbolInfo.isPositive) TvBullGreen else TvBearRed

        drawLine(
            color = priceBadgeColor.copy(alpha = 0.85f),
            start = Offset(0f, currentPriceY),
            end = Offset(usableWidth, currentPriceY),
            strokeWidth = 1.2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
        )

        // Pulsing dot at rightmost visible candle
        val latestVisibleIdx = visibleCandles.lastIndex
        val lastX = latestVisibleIdx * candleStep + candleStep / 2f
        drawCircle(color = priceBadgeColor.copy(alpha = pulseAlpha * 0.4f), radius = 8.dp.toPx(), center = Offset(lastX, currentPriceY))
        drawCircle(color = priceBadgeColor, radius = 3.5.dp.toPx(), center = Offset(lastX, currentPriceY))

        // Badge on right Y-axis
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
            drawText(symbolInfo.formatPrice(currentPrice), badgeLeft + badgeWidth / 2f, badgeTop + 14.dp.toPx(), paint)
        }
    }

    // 10. Oscillator Sub-Pane (RSI, MACD, ATR, or Stoch RSI)
    if (hasSubPane) {
        drawLine(color = TvSurfaceVariant, start = Offset(0f, subPaneTop), end = Offset(totalWidth, subPaneTop), strokeWidth = 1.5f)

        if (indicatorSettings.showRsi && rsi.isNotEmpty()) {
            drawRsiSubPane(rsi, startIndex, visibleCandles.size, candleStep, usableWidth, subPaneTop, subPaneHeight)
        } else if (indicatorSettings.showMacd && macd.isNotEmpty()) {
            drawMacdSubPane(macd, startIndex, visibleCandles.size, candleStep, candleWidth, usableWidth, subPaneTop, subPaneHeight)
        }
    }

    // 11. Crosshair & Scrub Readout
    if (isScrubbing && scrubIndexInVisible in visibleCandles.indices) {
        val crossX = scrubIndexInVisible * candleStep + candleStep / 2f
        val boundedY = scrubTouchY.coerceIn(0f, mainChartHeight)

        drawLine(color = TvTextPrimary.copy(alpha = 0.65f), start = Offset(crossX, 0f), end = Offset(crossX, totalHeight - bottomTimeAxisHeight), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
        drawLine(color = TvTextPrimary.copy(alpha = 0.65f), start = Offset(0f, boundedY), end = Offset(usableWidth, boundedY), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))

        val scrubbedPrice = yToPrice(boundedY)
        val pillHeight = 20.dp.toPx()
        val pillWidth = rightAxisWidth - 4f
        val pillTop = boundedY - pillHeight / 2f

        drawRoundRect(color = TvTextPrimary, topLeft = Offset(usableWidth + 2f, pillTop), size = Size(pillWidth, pillHeight), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()))

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                textSize = 9.sp.toPx()
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawText(symbolInfo.formatPrice(scrubbedPrice), usableWidth + 2f + pillWidth / 2f, pillTop + 13.dp.toPx(), paint)
        }

        val selectedCandle = visibleCandles[scrubIndexInVisible]
        val timePillWidth = 75.dp.toPx()
        val timePillHeight = 18.dp.toPx()
        val timePillLeft = (crossX - timePillWidth / 2f).coerceIn(4f, usableWidth - timePillWidth - 4f)
        val timePillTop = totalHeight - bottomTimeAxisHeight + 4f

        drawRoundRect(color = TvSurfaceVariant, topLeft = Offset(timePillLeft, timePillTop), size = Size(timePillWidth, timePillHeight), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx(), 4.dp.toPx()))

        drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 9.sp.toPx()
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
            }
            drawText(selectedCandle.formattedTime(timeframe), timePillLeft + timePillWidth / 2f, timePillTop + 12.dp.toPx(), paint)
        }
    }
}

private fun DrawScope.drawChartDrawing(
    drawing: ChartDrawing,
    visibleCandles: List<Candle>,
    startIndex: Int,
    candleStep: Float,
    priceToY: (Double) -> Float,
    usableWidth: Float,
    mainChartHeight: Float
) {
    val color = try {
        Color(android.graphics.Color.parseColor(drawing.colorHex))
    } catch (e: Exception) {
        TvBrandBlue
    }

    when (drawing.type) {
        DrawingToolType.HORIZONTAL_LINE -> {
            val pt = drawing.points.firstOrNull() ?: return
            val y = priceToY(pt.price)
            drawLine(color = color, start = Offset(0f, y), end = Offset(usableWidth, y), strokeWidth = drawing.strokeWidth)
        }
        DrawingToolType.VERTICAL_LINE -> {
            val pt = drawing.points.firstOrNull() ?: return
            val candleIdx = visibleCandles.indexOfFirst { it.timestamp >= pt.timestamp }
            if (candleIdx >= 0) {
                val x = candleIdx * candleStep + candleStep / 2f
                drawLine(color = color, start = Offset(x, 0f), end = Offset(x, mainChartHeight), strokeWidth = drawing.strokeWidth)
            }
        }
        DrawingToolType.TREND_LINE, DrawingToolType.RAY -> {
            if (drawing.points.size < 2) return
            val pt1 = drawing.points[0]
            val pt2 = drawing.points[1]

            val idx1 = visibleCandles.indexOfFirst { it.timestamp >= pt1.timestamp }
            val idx2 = visibleCandles.indexOfFirst { it.timestamp >= pt2.timestamp }

            val x1 = if (idx1 >= 0) idx1 * candleStep + candleStep / 2f else 0f
            val y1 = priceToY(pt1.price)
            val x2 = if (idx2 >= 0) idx2 * candleStep + candleStep / 2f else usableWidth
            val y2 = priceToY(pt2.price)

            if (drawing.type == DrawingToolType.RAY) {
                val dx = x2 - x1
                val dy = y2 - y1
                val factor = if (dx != 0f) (usableWidth - x1) / dx else 1f
                val extendedX = x1 + dx * factor
                val extendedY = y1 + dy * factor
                drawLine(color = color, start = Offset(x1, y1), end = Offset(extendedX, extendedY), strokeWidth = drawing.strokeWidth)
            } else {
                drawLine(color = color, start = Offset(x1, y1), end = Offset(x2, y2), strokeWidth = drawing.strokeWidth)
            }
        }
        DrawingToolType.RECTANGLE -> {
            if (drawing.points.size < 2) return
            val pt1 = drawing.points[0]
            val pt2 = drawing.points[1]

            val idx1 = visibleCandles.indexOfFirst { it.timestamp >= pt1.timestamp }
            val idx2 = visibleCandles.indexOfFirst { it.timestamp >= pt2.timestamp }

            val x1 = if (idx1 >= 0) idx1 * candleStep + candleStep / 2f else 0f
            val x2 = if (idx2 >= 0) idx2 * candleStep + candleStep / 2f else usableWidth
            val y1 = priceToY(pt1.price)
            val y2 = priceToY(pt2.price)

            val left = min(x1, x2)
            val top = min(y1, y2)
            val width = abs(x2 - x1)
            val height = abs(y2 - y1)

            drawRect(color = color.copy(alpha = 0.15f), topLeft = Offset(left, top), size = Size(width, height))
            drawRect(color = color, topLeft = Offset(left, top), size = Size(width, height), style = Stroke(width = drawing.strokeWidth))
        }
        DrawingToolType.FIBONACCI_RETRACEMENT -> {
            if (drawing.points.size < 2) return
            val highPrice = max(drawing.points[0].price, drawing.points[1].price)
            val lowPrice = min(drawing.points[0].price, drawing.points[1].price)
            val diff = highPrice - lowPrice

            ChartDrawing.FIBONACCI_LEVELS.forEach { (ratio, colorHex) ->
                val levelPrice = highPrice - (diff * ratio)
                val y = priceToY(levelPrice)
                val fibColor = try { Color(android.graphics.Color.parseColor(colorHex)) } catch (e: Exception) { Color.Gray }

                drawLine(
                    color = fibColor,
                    start = Offset(0f, y),
                    end = Offset(usableWidth, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                )

                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        this.color = android.graphics.Color.parseColor(colorHex)
                        textSize = 8.sp.toPx()
                        textAlign = android.graphics.Paint.Align.LEFT
                        isAntiAlias = true
                    }
                    drawText("${(ratio * 100).toInt()}% (${String.format("%.2f", levelPrice)})", 6f, y - 3f, paint)
                }
            }
        }
        else -> {}
    }
}

private fun DrawScope.drawIndicatorLineSlice(
    values: List<Double?>,
    startIndex: Int,
    visibleCount: Int,
    candleStep: Float,
    priceToY: (Double) -> Float,
    color: Color
) {
    val path = Path()
    var started = false

    for (i in 0 until visibleCount) {
        val globalIdx = startIndex + i
        val v = values.getOrNull(globalIdx) ?: continue
        val x = i * candleStep + candleStep / 2f
        val y = priceToY(v)

        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
    }

    if (started) {
        drawPath(path, color = color, style = Stroke(width = 1.6f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawRsiSubPane(
    rsi: List<Double?>,
    startIndex: Int,
    visibleCount: Int,
    candleStep: Float,
    usableWidth: Float,
    subPaneTop: Float,
    subPaneHeight: Float
) {
    fun rsiToY(v: Double): Float {
        val norm = (100.0 - v) / 100.0
        return (subPaneTop + norm * subPaneHeight).toFloat().coerceIn(subPaneTop, subPaneTop + subPaneHeight)
    }

    val y70 = rsiToY(70.0)
    val y30 = rsiToY(30.0)

    drawRect(color = TvIndicatorPurple.copy(alpha = 0.05f), topLeft = Offset(0f, y70), size = Size(usableWidth, y30 - y70))
    drawLine(color = TvIndicatorPurple.copy(alpha = 0.4f), start = Offset(0f, y70), end = Offset(usableWidth, y70), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))
    drawLine(color = TvIndicatorPurple.copy(alpha = 0.4f), start = Offset(0f, y30), end = Offset(usableWidth, y30), strokeWidth = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f))

    val path = Path()
    var started = false
    for (i in 0 until visibleCount) {
        val globalIdx = startIndex + i
        val v = rsi.getOrNull(globalIdx) ?: continue
        val x = i * candleStep + candleStep / 2f
        val y = rsiToY(v)
        if (!started) {
            path.moveTo(x, y)
            started = true
        } else {
            path.lineTo(x, y)
        }
    }
    drawPath(path, color = TvIndicatorPurple, style = Stroke(width = 1.8f))

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#787B86")
            textSize = 8.sp.toPx()
            textAlign = android.graphics.Paint.Align.LEFT
            isAntiAlias = true
        }
        drawText("RSI (14)", 8f, subPaneTop + 13.dp.toPx(), paint)
        drawText("70", usableWidth + 6f, y70 + 3.dp.toPx(), paint)
        drawText("30", usableWidth + 6f, y30 + 3.dp.toPx(), paint)
    }
}

private fun DrawScope.drawMacdSubPane(
    macd: List<MacdPoint?>,
    startIndex: Int,
    visibleCount: Int,
    candleStep: Float,
    candleWidth: Float,
    usableWidth: Float,
    subPaneTop: Float,
    subPaneHeight: Float
) {
    var maxVal = 0.0001
    for (i in 0 until visibleCount) {
        val pt = macd.getOrNull(startIndex + i) ?: continue
        maxVal = max(maxVal, max(abs(pt.macd), max(abs(pt.signal), abs(pt.histogram))))
    }

    val zeroY = subPaneTop + (subPaneHeight / 2f)

    fun macdToY(v: Double): Float {
        val norm = -v / maxVal // positive is up
        return (zeroY + norm * (subPaneHeight / 2f - 4f)).toFloat()
    }

    // Zero line
    drawLine(color = TvDivider, start = Offset(0f, zeroY), end = Offset(usableWidth, zeroY), strokeWidth = 1f)

    // Histogram bars
    for (i in 0 until visibleCount) {
        val pt = macd.getOrNull(startIndex + i) ?: continue
        val x = i * candleStep + (candleStep - candleWidth) / 2f
        val y = macdToY(pt.histogram)
        val color = if (pt.histogram >= 0) TvBullGreen.copy(alpha = 0.7f) else TvBearRed.copy(alpha = 0.7f)
        val top = min(zeroY, y)
        val height = max(1f, abs(zeroY - y))
        drawRect(color = color, topLeft = Offset(x, top), size = Size(candleWidth, height))
    }

    // MACD line & Signal line
    val macdPath = Path()
    val signalPath = Path()
    var macdStarted = false

    for (i in 0 until visibleCount) {
        val pt = macd.getOrNull(startIndex + i) ?: continue
        val x = i * candleStep + candleStep / 2f
        val yMacd = macdToY(pt.macd)
        val ySignal = macdToY(pt.signal)

        if (!macdStarted) {
            macdPath.moveTo(x, yMacd)
            signalPath.moveTo(x, ySignal)
            macdStarted = true
        } else {
            macdPath.lineTo(x, yMacd)
            signalPath.lineTo(x, ySignal)
        }
    }

    drawPath(macdPath, color = TvBrandBlue, style = Stroke(width = 1.6f))
    drawPath(signalPath, color = TvIndicatorOrange, style = Stroke(width = 1.6f))

    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#787B86")
            textSize = 8.sp.toPx()
            textAlign = android.graphics.Paint.Align.LEFT
            isAntiAlias = true
        }
        drawText("MACD (12, 26, 9)", 8f, subPaneTop + 13.dp.toPx(), paint)
    }
}

@Composable
private fun ConnectionHudBar(
    connectionStatus: ConnectionStatus,
    lastUpdateTimestamp: Long?,
    onRefresh: () -> Unit
) {
    val statusColor = when (connectionStatus) {
        is ConnectionStatus.Connected -> TvBullGreen
        is ConnectionStatus.Connecting -> TvIndicatorYellow
        is ConnectionStatus.Reconnecting -> TvIndicatorOrange
        is ConnectionStatus.Disconnected -> TvTextSecondary
        is ConnectionStatus.Error -> TvBearRed
    }

    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    val lastUpdateText = if (lastUpdateTimestamp != null && lastUpdateTimestamp > 0) {
        "Updated ${timeFormat.format(Date(lastUpdateTimestamp))}"
    } else {
        "Connecting..."
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TvSurfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(statusColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${connectionStatus.label} • Binance Public Feed",
                color = statusColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = lastUpdateText,
                color = TvTextSecondary,
                fontSize = 10.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh Feed",
                tint = TvTextSecondary,
                modifier = Modifier
                    .size(13.dp)
                    .clickable { onRefresh() }
            )
        }
    }
}

@Composable
private fun OhlcvHudBar(
    candle: Candle,
    timeframe: Timeframe,
    symbolInfo: SymbolInfo,
    indicatorSettings: IndicatorSettings,
    ema20Val: Double?,
    ema50Val: Double?,
    vwapVal: Double?,
    rsiVal: Double?,
    macdVal: MacdPoint?,
    supertrendVal: SupertrendPoint?
) {
    val changeColor = if (candle.isBullish) TvBullGreen else TvBearRed
    val sign = if (candle.change >= 0) "+" else ""

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(TvSurface)
            .padding(horizontal = 12.dp, vertical = 5.dp)
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

            Spacer(modifier = Modifier.width(10.dp))

            Text("O", color = TvTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(" ${symbolInfo.formatPrice(candle.open)}", color = TvTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

            Spacer(modifier = Modifier.width(6.dp))

            Text("H", color = TvTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(" ${symbolInfo.formatPrice(candle.high)}", color = TvTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

            Spacer(modifier = Modifier.width(6.dp))

            Text("L", color = TvTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(" ${symbolInfo.formatPrice(candle.low)}", color = TvTextPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)

            Spacer(modifier = Modifier.width(6.dp))

            Text("C", color = TvTextSecondary, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
            Text(" ${symbolInfo.formatPrice(candle.close)}", color = changeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

            Spacer(modifier = Modifier.width(8.dp))

            Text("$sign${String.format("%.2f", candle.changePercent)}%", color = changeColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        // Active indicator values readout
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
        ) {
            if (indicatorSettings.showVolume) {
                Text("Vol: ", color = TvTextSecondary, fontSize = 10.sp)
                Text(String.format("%.2f", candle.volume), color = changeColor, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (indicatorSettings.showEma20 && ema20Val != null) {
                Text("EMA 20: ", color = TvIndicatorYellow, fontSize = 10.sp)
                Text(symbolInfo.formatPrice(ema20Val), color = TvTextPrimary, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (indicatorSettings.showEma50 && ema50Val != null) {
                Text("EMA 50: ", color = TvIndicatorOrange, fontSize = 10.sp)
                Text(symbolInfo.formatPrice(ema50Val), color = TvTextPrimary, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (indicatorSettings.showVwap && vwapVal != null) {
                Text("VWAP: ", color = Color(0xFFE91E63), fontSize = 10.sp)
                Text(symbolInfo.formatPrice(vwapVal), color = TvTextPrimary, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (indicatorSettings.showRsi && rsiVal != null) {
                Text("RSI: ", color = TvIndicatorPurple, fontSize = 10.sp)
                Text(String.format("%.1f", rsiVal), color = TvTextPrimary, fontSize = 10.sp)
                Spacer(modifier = Modifier.width(8.dp))
            }

            if (indicatorSettings.showSupertrend && supertrendVal != null) {
                Text("Supertrend: ", color = if (supertrendVal.isBullish) TvBullGreen else TvBearRed, fontSize = 10.sp)
                Text(symbolInfo.formatPrice(supertrendVal.value), color = TvTextPrimary, fontSize = 10.sp)
            }
        }
    }
}
