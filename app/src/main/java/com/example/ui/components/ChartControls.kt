package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.Check
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
    onOpenCreateAlert: () -> Unit,
    onPriceShock: (percent: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    var showIndicatorsDialog by remember { mutableStateOf(false) }
    var showShockMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(TvSurface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Chart Style switcher
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
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("chart_style_candles")
            ) {
                Icon(
                    imageVector = Icons.Default.CandlestickChart,
                    contentDescription = "Candlesticks",
                    tint = if (isCandles) TvTextWhite else TvTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isLine) TvBrandBlue else Color.Transparent)
                    .clickable { onChartStyleChange(ChartStyle.LINE) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("chart_style_line")
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = "Line",
                    tint = if (isLine) TvTextWhite else TvTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isArea) TvBrandBlue else Color.Transparent)
                    .clickable { onChartStyleChange(ChartStyle.AREA) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("chart_style_area")
            ) {
                Text(
                    text = "Area",
                    color = if (isArea) TvTextWhite else TvTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Indicators Toggle Button
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
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Price Shock Test Pill (instantly test alert triggers!)
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = TvSurfaceVariant,
            onClick = { showShockMenu = !showShockMenu },
            modifier = Modifier.testTag("price_shock_toggle")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Simulate Jump",
                    tint = TvAlertAmber,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Test Jump",
                    color = TvTextPrimary,
                    fontSize = 11.sp
                )
            }
        }

        // Primary "+ Alert" Action Button
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = TvAlertAmber,
            onClick = onOpenCreateAlert,
            modifier = Modifier.testTag("create_alert_button")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddAlert,
                    contentDescription = "Add Alert",
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "+ Alert",
                    color = Color.Black,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Quick Test Jump Dropdown/Row
    if (showShockMenu) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TvSurfaceVariant.copy(alpha = 0.9f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Simulate Ticker Jump:",
                color = TvTextSecondary,
                fontSize = 11.sp
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(TvBullGreen)
                    .clickable {
                        onPriceShock(1.5)
                        showShockMenu = false
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "+1.5% Pump",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(TvBearRed)
                    .clickable {
                        onPriceShock(-1.5)
                        showShockMenu = false
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "-1.5% Dump",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(TvBrandBlue)
                    .clickable {
                        onPriceShock(3.0)
                        showShockMenu = false
                    }
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "+3.0% Surge",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }

    // Indicators Dialog
    if (showIndicatorsDialog) {
        IndicatorsModal(
            settings = indicatorSettings,
            onSettingsChange = onIndicatorSettingsChange,
            onDismiss = { showIndicatorsDialog = false }
        )
    }
}

@Composable
private fun IndicatorsModal(
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
                fontSize = 18.sp
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                IndicatorToggleRow(
                    name = "EMA 20",
                    description = "Exponential Moving Average (20 periods)",
                    color = TvIndicatorYellow,
                    checked = settings.showEma20,
                    onCheckedChange = { onSettingsChange(settings.copy(showEma20 = it)) }
                )
                Spacer(modifier = Modifier.height(12.dp))

                IndicatorToggleRow(
                    name = "EMA 50",
                    description = "Exponential Moving Average (50 periods)",
                    color = TvIndicatorOrange,
                    checked = settings.showEma50,
                    onCheckedChange = { onSettingsChange(settings.copy(showEma50 = it)) }
                )
                Spacer(modifier = Modifier.height(12.dp))

                IndicatorToggleRow(
                    name = "Bollinger Bands",
                    description = "20 SMA ± 2 Standard Deviations channel",
                    color = TvIndicatorCyan,
                    checked = settings.showBollingerBands,
                    onCheckedChange = { onSettingsChange(settings.copy(showBollingerBands = it)) }
                )
                Spacer(modifier = Modifier.height(12.dp))

                IndicatorToggleRow(
                    name = "RSI (14)",
                    description = "Relative Strength Index pane (30/70 bounds)",
                    color = TvIndicatorPurple,
                    checked = settings.showRsi,
                    onCheckedChange = { onSettingsChange(settings.copy(showRsi = it)) }
                )
                Spacer(modifier = Modifier.height(12.dp))

                IndicatorToggleRow(
                    name = "Volume Bars",
                    description = "Colored trading volume sub-pane",
                    color = TvBullGreen,
                    checked = settings.showVolume,
                    onCheckedChange = { onSettingsChange(settings.copy(showVolume = it)) }
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
private fun IndicatorToggleRow(
    name: String,
    description: String,
    color: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = name,
                    color = TvTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = TvTextSecondary,
                    fontSize = 11.sp
                )
            }
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
