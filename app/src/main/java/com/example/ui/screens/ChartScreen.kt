package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Candle
import com.example.model.ChartStyle
import com.example.model.IndicatorSettings
import com.example.model.PriceAlert
import com.example.model.SymbolInfo
import com.example.model.Timeframe
import com.example.ui.components.AlertCreateDialog
import com.example.ui.components.ChartControlsBar
import com.example.ui.components.TimeframeSelector
import com.example.ui.components.TradingChart
import com.example.ui.theme.TvAlertAmber
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvBearRed
import com.example.ui.theme.TvBullGreen
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvTextWhite

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChartScreen(
    symbolInfo: SymbolInfo,
    candles: List<Candle>,
    timeframe: Timeframe,
    chartStyle: ChartStyle,
    indicatorSettings: IndicatorSettings,
    symbolAlerts: List<PriceAlert>,
    onSelectTimeframe: (Timeframe) -> Unit,
    onChartStyleChange: (ChartStyle) -> Unit,
    onIndicatorSettingsChange: (IndicatorSettings) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenSymbolPicker: () -> Unit,
    onSaveAlert: (PriceAlert) -> Unit,
    onDeleteAlert: (Long) -> Unit,
    onPriceShock: (percent: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showCreateAlertDialog by remember { mutableStateOf(false) }
    var alertInitialPrice by remember { mutableStateOf<Double?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
    ) {
        // Top Symbol Header Bar
        TopSymbolHeader(
            symbolInfo = symbolInfo,
            onOpenSymbolPicker = onOpenSymbolPicker,
            onToggleFavorite = { onToggleFavorite(symbolInfo.symbol) }
        )

        // Timeframe Selector
        TimeframeSelector(
            selectedTimeframe = timeframe,
            onSelectTimeframe = onSelectTimeframe,
            modifier = Modifier.fillMaxWidth()
        )

        // Chart Toolbar / Controls
        ChartControlsBar(
            chartStyle = chartStyle,
            onChartStyleChange = onChartStyleChange,
            indicatorSettings = indicatorSettings,
            onIndicatorSettingsChange = onIndicatorSettingsChange,
            onOpenCreateAlert = {
                alertInitialPrice = null
                showCreateAlertDialog = true
            },
            onPriceShock = onPriceShock,
            modifier = Modifier.fillMaxWidth()
        )

        // Interactive TradingView Chart Canvas
        TradingChart(
            symbolInfo = symbolInfo,
            candles = candles,
            timeframe = timeframe,
            chartStyle = chartStyle,
            indicatorSettings = indicatorSettings,
            activeAlerts = symbolAlerts,
            onAddAlertAtPrice = { price ->
                alertInitialPrice = price
                showCreateAlertDialog = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )

        // Active Alerts for this Symbol Section
        if (symbolAlerts.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvSurface)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = "Alerts",
                        tint = TvAlertAmber,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Active Alerts on Chart (${symbolAlerts.size})",
                        color = TvTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    symbolAlerts.forEach { alert ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = TvSurfaceVariant,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "🔔 ${alert.condition.label} ${symbolInfo.formatPrice(alert.targetPrice)}",
                                    color = TvAlertAmber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove alert",
                                    tint = TvTextSecondary,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { onDeleteAlert(alert.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateAlertDialog) {
        AlertCreateDialog(
            symbolInfo = symbolInfo,
            initialTargetPrice = alertInitialPrice,
            onDismiss = { showCreateAlertDialog = false },
            onSaveAlert = { newAlert ->
                onSaveAlert(newAlert)
                showCreateAlertDialog = false
            }
        )
    }
}

@Composable
private fun TopSymbolHeader(
    symbolInfo: SymbolInfo,
    onOpenSymbolPicker: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val changeColor = if (symbolInfo.isPositive) TvBullGreen else TvBearRed

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(TvSurface)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Symbol + Dropdown Trigger
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onOpenSymbolPicker() }
                .padding(vertical = 4.dp)
                .testTag("symbol_picker_trigger")
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = symbolInfo.symbol,
                        color = TvTextWhite,
                        fontWeight = FontWeight.Black,
                        fontSize = 17.sp
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "Switch Symbol",
                        tint = TvTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = "${symbolInfo.name} • ${symbolInfo.category.label}",
                    color = TvTextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Price + 24h Change & Favorite Button
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = symbolInfo.formatPrice(),
                    color = TvTextWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (symbolInfo.isPositive) TvBullGreen else TvBearRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${if (symbolInfo.isPositive) "+" else ""}${String.format("%.2f", symbolInfo.changePercent)}%",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (symbolInfo.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (symbolInfo.isFavorite) TvAlertAmber else TvTextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
