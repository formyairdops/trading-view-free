package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.model.PriceAlert
import com.example.model.SymbolInfo
import com.example.ui.components.AlertCreateDialog
import com.example.ui.theme.TvAlertAmber
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvBearRed
import com.example.ui.theme.TvBorder
import com.example.ui.theme.TvBrandBlue
import com.example.ui.theme.TvBullGreen
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextMuted
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvTextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Composable
fun AlertsScreen(
    alerts: List<PriceAlert>,
    symbols: List<SymbolInfo>,
    selectedSymbol: SymbolInfo,
    onToggleAlertActive: (PriceAlert) -> Unit,
    onDeleteAlert: (Long) -> Unit,
    onClearHistory: () -> Unit,
    onSelectSymbolAndNavigateToChart: (String) -> Unit,
    onSaveNewAlert: (PriceAlert) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Active, 1: History
    var showCreateDialog by remember { mutableStateOf(false) }

    val activeAlerts = alerts.filter { !it.isTriggered }
    val historyAlerts = alerts.filter { it.isTriggered }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvSurface)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Price Alerts",
                        color = TvTextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${activeAlerts.count { it.isActive }} active monitoring triggers",
                        color = TvTextSecondary,
                        fontSize = 12.sp
                    )
                }

                if (selectedTab == 1 && historyAlerts.isNotEmpty()) {
                    TextButton(
                        onClick = onClearHistory,
                        modifier = Modifier.testTag("clear_history_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.ClearAll,
                            contentDescription = "Clear",
                            tint = TvAlertAmber,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear", color = TvAlertAmber, fontSize = 12.sp)
                    }
                }
            }

            // Tabs: Active vs History
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = TvSurface,
                contentColor = TvBrandBlue,
                indicator = { tabPositions ->
                    SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = TvBrandBlue,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Active",
                                color = if (selectedTab == 0) TvTextWhite else TvTextSecondary,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (selectedTab == 0) TvBrandBlue else TvSurfaceVariant)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${activeAlerts.size}",
                                    color = TvTextWhite,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                )

                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Triggered Log",
                                color = if (selectedTab == 1) TvTextWhite else TvTextSecondary,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium
                            )
                            if (historyAlerts.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(TvAlertAmber.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${historyAlerts.size}",
                                        color = TvAlertAmber,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                )
            }

            // Tab Content
            val displayedList = if (selectedTab == 0) activeAlerts else historyAlerts

            if (displayedList.isEmpty()) {
                EmptyAlertsState(
                    isHistory = selectedTab == 1,
                    onCreateClick = { showCreateDialog = true }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayedList, key = { it.id }) { alert ->
                        val symbolInfo = symbols.find { it.symbol == alert.symbol }
                        AlertItemCard(
                            alert = alert,
                            symbolInfo = symbolInfo,
                            onToggleActive = { onToggleAlertActive(alert) },
                            onDelete = { onDeleteAlert(alert.id) },
                            onViewChart = { onSelectSymbolAndNavigateToChart(alert.symbol) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // Padding for FAB
                    }
                }
            }
        }

        // Floating Action Button
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            containerColor = TvAlertAmber,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("fab_create_alert")
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Alert")
        }
    }

    if (showCreateDialog) {
        AlertCreateDialog(
            symbolInfo = selectedSymbol,
            onDismiss = { showCreateDialog = false },
            onSaveAlert = {
                onSaveNewAlert(it)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun AlertItemCard(
    alert: PriceAlert,
    symbolInfo: SymbolInfo?,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    onViewChart: () -> Unit
) {
    val currentPrice = symbolInfo?.currentPrice ?: 0.0
    val distance = if (currentPrice > 0) {
        ((alert.targetPrice - currentPrice) / currentPrice) * 100.0
    } else 0.0

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TvSurface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Row 1: Symbol, Condition Tag, Active Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (alert.isTriggered) TvBullGreen.copy(alpha = 0.2f)
                                else if (alert.isActive) TvAlertAmber.copy(alpha = 0.2f)
                                else TvSurfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (alert.isTriggered) Icons.Default.CheckCircle
                            else if (alert.isActive) Icons.Default.NotificationsActive
                            else Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = if (alert.isTriggered) TvBullGreen
                            else if (alert.isActive) TvAlertAmber
                            else TvTextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = alert.symbol,
                            color = TvTextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = alert.condition.label,
                            color = TvBrandBlue,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (!alert.isTriggered) {
                    Switch(
                        checked = alert.isActive,
                        onCheckedChange = { onToggleActive() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = TvTextWhite,
                            checkedTrackColor = TvAlertAmber,
                            uncheckedThumbColor = TvTextSecondary,
                            uncheckedTrackColor = TvSurfaceVariant
                        )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(TvBullGreen.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "TRIGGERED",
                            color = TvBullGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Target Price vs Current Price & Distance
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(TvSurfaceVariant)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "TARGET PRICE",
                        color = TvTextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = symbolInfo?.formatPrice(alert.targetPrice) ?: String.format("%.2f", alert.targetPrice),
                        color = TvTextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                if (alert.isTriggered && alert.triggeredPrice != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "TRIGGERED AT",
                            color = TvTextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = symbolInfo?.formatPrice(alert.triggeredPrice) ?: String.format("%.2f", alert.triggeredPrice),
                            color = TvBullGreen,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (currentPrice > 0) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "DISTANCE",
                            color = TvTextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                        val sign = if (distance > 0) "+" else ""
                        Text(
                            text = "$sign${String.format("%.2f", distance)}%",
                            color = if (abs(distance) < 0.5) TvAlertAmber else TvTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            if (alert.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = alert.note,
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 3: Frequency badge, Trigger timestamp, Actions (Chart, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (alert.isTriggered && alert.triggeredAt != null) {
                        "Triggered: ${formatTimestamp(alert.triggeredAt)}"
                    } else {
                        if (alert.isOnlyOnce) "Trigger: Only Once" else "Trigger: Every Time"
                    },
                    color = TvTextMuted,
                    fontSize = 11.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = TvSurfaceVariant,
                        onClick = onViewChart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShowChart,
                                contentDescription = "View Chart",
                                tint = TvBrandBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Chart",
                                color = TvBrandBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Alert",
                            tint = TvTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyAlertsState(
    isHistory: Boolean,
    onCreateClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(TvSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isHistory) Icons.Default.CheckCircle else Icons.Default.Notifications,
                contentDescription = null,
                tint = TvAlertAmber,
                modifier = Modifier.size(36.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isHistory) "No Triggered Alerts Yet" else "No Active Alerts",
            color = TvTextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isHistory) {
                "When live market prices cross your target thresholds, triggered alerts will appear here."
            } else {
                "Stay on top of market swings. Set price alerts with custom conditions directly on the chart."
            },
            color = TvTextSecondary,
            fontSize = 13.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        if (!isHistory) {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(containerColor = TvAlertAmber),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Create Alert",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.US)
    return sdf.format(Date(timestamp))
}
