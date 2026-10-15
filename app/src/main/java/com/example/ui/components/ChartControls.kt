package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.model.ChartStyle
import com.example.model.DrawingToolType
import com.example.model.IndicatorSettings
import com.example.ui.theme.TvAlertAmber
import com.example.ui.theme.TvBearRed
import com.example.ui.theme.TvBrandBlue
import com.example.ui.theme.TvBullGreen
import com.example.ui.theme.TvIndicatorCyan
import com.example.ui.theme.TvIndicatorOrange
import com.example.ui.theme.TvIndicatorPurple
import com.example.ui.theme.TvIndicatorYellow
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvTextWhite

@Composable
fun ChartControlsBar(
    chartStyle: ChartStyle,
    onChartStyleChange: (ChartStyle) -> Unit,
    indicatorSettings: IndicatorSettings,
    onIndicatorSettingsChange: (IndicatorSettings) -> Unit,
    activeDrawingTool: DrawingToolType,
    onSelectDrawingTool: (DrawingToolType) -> Unit,
    onClearDrawings: () -> Unit,
    onOpenOrderBook: () -> Unit,
    onOpenCreateAlert: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showIndicatorsDialog by remember { mutableStateOf(false) }
    var showDrawingsMenu by remember { mutableStateOf(false) }

    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TvSurface)
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Chart Style Switcher
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(TvSurfaceVariant)
                .padding(2.dp)
        ) {
            val isCandles = chartStyle == ChartStyle.CANDLES
            val isLine = chartStyle == ChartStyle.LINE
            val isArea = chartStyle == ChartStyle.AREA

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isCandles) TvBrandBlue else Color.Transparent)
                    .clickable { onChartStyleChange(ChartStyle.CANDLES) }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
                    .testTag("chart_style_candles")
            ) {
                Icon(
                    imageVector = Icons.Default.CandlestickChart,
                    contentDescription = "Candlesticks",
                    tint = if (isCandles) TvTextWhite else TvTextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isLine) TvBrandBlue else Color.Transparent)
                    .clickable { onChartStyleChange(ChartStyle.LINE) }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
                    .testTag("chart_style_line")
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = "Line",
                    tint = if (isLine) TvTextWhite else TvTextSecondary,
                    modifier = Modifier.size(15.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isArea) TvBrandBlue else Color.Transparent)
                    .clickable { onChartStyleChange(ChartStyle.AREA) }
                    .padding(horizontal = 7.dp, vertical = 4.dp)
                    .testTag("chart_style_area")
            ) {
                Text(
                    text = "Area",
                    color = if (isArea) TvTextWhite else TvTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Indicators Button
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = TvSurfaceVariant,
            onClick = { showIndicatorsDialog = true },
            modifier = Modifier.testTag("open_indicators_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Indicators",
                    tint = TvBrandBlue,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Indicators",
                    color = TvTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Drawing Tools Menu Button
        val isDrawingActive = activeDrawingTool != DrawingToolType.NONE
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isDrawingActive) TvBrandBlue else TvSurfaceVariant,
            onClick = { showDrawingsMenu = !showDrawingsMenu },
            modifier = Modifier.testTag("drawing_tools_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Draw",
                    tint = if (isDrawingActive) Color.White else TvTextSecondary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isDrawingActive) activeDrawingTool.label else "Draw",
                    color = if (isDrawingActive) Color.White else TvTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Order Book & Depth Button
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = TvSurfaceVariant,
            onClick = onOpenOrderBook,
            modifier = Modifier.testTag("open_orderbook_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BarChart,
                    contentDescription = "Order Book",
                    tint = TvBullGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Order Book",
                    color = TvTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // "+ Alert" Button
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = TvAlertAmber,
            onClick = onOpenCreateAlert,
            modifier = Modifier.testTag("create_alert_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddAlert,
                    contentDescription = "Add Alert",
                    tint = Color.Black,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = "+ Alert",
                    color = Color.Black,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Drawing Tools Submenu Dialog
    if (showDrawingsMenu) {
        DrawingToolsModal(
            activeTool = activeDrawingTool,
            onSelectTool = {
                onSelectDrawingTool(it)
                showDrawingsMenu = false
            },
            onClearAll = {
                onClearDrawings()
                showDrawingsMenu = false
            },
            onDismiss = { showDrawingsMenu = false }
        )
    }

    // Indicators Settings Dialog
    if (showIndicatorsDialog) {
        FullIndicatorsModal(
            settings = indicatorSettings,
            onSettingsChange = onIndicatorSettingsChange,
            onDismiss = { showIndicatorsDialog = false }
        )
    }
}

@Composable
private fun DrawingToolsModal(
    activeTool: DrawingToolType,
    onSelectTool: (DrawingToolType) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TvSurface,
        title = {
            Text(
                text = "Chart Drawing Tools",
                color = TvTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                DrawingToolType.entries.forEach { tool ->
                    val isSelected = tool == activeTool
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) TvBrandBlue.copy(alpha = 0.2f) else Color.Transparent)
                            .clickable { onSelectTool(tool) }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = tool.label,
                            color = if (isSelected) TvBrandBlue else TvTextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = tool.iconDescription,
                            color = TvTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Clear all drawings option
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = TvBearRed.copy(alpha = 0.15f),
                    onClick = onClearAll,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear", tint = TvBearRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Drawings on Chart", color = TvBearRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TvBrandBlue)
            }
        }
    )
}

@Composable
private fun FullIndicatorsModal(
    settings: IndicatorSettings,
    onSettingsChange: (IndicatorSettings) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = TvSurface,
        title = {
            Text(
                text = "Technical Indicators",
                color = TvTextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Section: Overlays
                Text(
                    text = "OVERLAYS",
                    color = TvTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                IndicatorToggleItem(
                    name = "SMA (20)",
                    color = Color(0xFF26A69A),
                    checked = settings.showSma,
                    onCheckedChange = { onSettingsChange(settings.copy(showSma = it)) }
                )

                IndicatorToggleItem(
                    name = "EMA (20)",
                    color = TvIndicatorYellow,
                    checked = settings.showEma20,
                    onCheckedChange = { onSettingsChange(settings.copy(showEma20 = it)) }
                )

                IndicatorToggleItem(
                    name = "EMA (50)",
                    color = TvIndicatorOrange,
                    checked = settings.showEma50,
                    onCheckedChange = { onSettingsChange(settings.copy(showEma50 = it)) }
                )

                IndicatorToggleItem(
                    name = "VWAP",
                    color = Color(0xFFE91E63),
                    checked = settings.showVwap,
                    onCheckedChange = { onSettingsChange(settings.copy(showVwap = it)) }
                )

                IndicatorToggleItem(
                    name = "Bollinger Bands (20, 2.0)",
                    color = TvIndicatorCyan,
                    checked = settings.showBollingerBands,
                    onCheckedChange = { onSettingsChange(settings.copy(showBollingerBands = it)) }
                )

                IndicatorToggleItem(
                    name = "Supertrend (10, 3.0)",
                    color = TvBullGreen,
                    checked = settings.showSupertrend,
                    onCheckedChange = { onSettingsChange(settings.copy(showSupertrend = it)) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Section: Oscillators & Sub-panels
                Text(
                    text = "SUB-PANEL OSCILLATORS",
                    color = TvTextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                IndicatorToggleItem(
                    name = "Volume Bars",
                    color = TvBullGreen,
                    checked = settings.showVolume,
                    onCheckedChange = { onSettingsChange(settings.copy(showVolume = it)) }
                )

                IndicatorToggleItem(
                    name = "RSI (14)",
                    color = TvIndicatorPurple,
                    checked = settings.showRsi,
                    onCheckedChange = { onSettingsChange(settings.copy(showRsi = it, showMacd = false)) }
                )

                IndicatorToggleItem(
                    name = "MACD (12, 26, 9)",
                    color = TvBrandBlue,
                    checked = settings.showMacd,
                    onCheckedChange = { onSettingsChange(settings.copy(showMacd = it, showRsi = false)) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", color = TvBrandBlue, fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun IndicatorToggleItem(
    name: String,
    color: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = name,
                color = TvTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TvTextWhite,
                checkedTrackColor = TvBrandBlue,
                uncheckedThumbColor = TvTextSecondary,
                uncheckedTrackColor = TvSurfaceVariant
            )
        )
    }
}
