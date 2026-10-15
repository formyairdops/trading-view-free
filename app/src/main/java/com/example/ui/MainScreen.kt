package com.example.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CandlestickChart
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.CandlestickChart
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.SymbolPickerBottomSheet
import com.example.ui.components.TriggeredAlertBanner
import com.example.ui.screens.AlertsScreen
import com.example.ui.screens.ChartScreen
import com.example.ui.screens.WatchlistScreen
import com.example.ui.theme.TvAlertAmber
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvBrandBlue
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvTextWhite

enum class MainTab(val label: String) {
    CHART("Chart"),
    ALERTS("Alerts"),
    WATCHLIST("Watchlist")
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(MainTab.CHART) }
    var showSymbolPicker by remember { mutableStateOf(false) }

    val currentSymbol by viewModel.currentSymbolInfo.collectAsStateWithLifecycle()
    val allSymbols by viewModel.symbols.collectAsStateWithLifecycle()
    val allAlerts by viewModel.allAlerts.collectAsStateWithLifecycle()
    val timeframe by viewModel.selectedTimeframe.collectAsStateWithLifecycle()
    val chartStyle by viewModel.chartStyle.collectAsStateWithLifecycle()
    val indicatorSettings by viewModel.indicatorSettings.collectAsStateWithLifecycle()
    val latestTriggerEvent by viewModel.latestTriggerEvent.collectAsStateWithLifecycle()
    val currentCandles by viewModel.currentCandles.collectAsStateWithLifecycle()
    val connectionStatus by viewModel.connectionStatus.collectAsStateWithLifecycle()
    val lastUpdateTimestamp by viewModel.lastUpdateTimestamp.collectAsStateWithLifecycle()
    val isLoadingCandles by viewModel.isLoadingCandles.collectAsStateWithLifecycle()
    val candleLoadError by viewModel.candleLoadError.collectAsStateWithLifecycle()
    val activeDrawingTool by viewModel.activeDrawingTool.collectAsStateWithLifecycle()
    val currentDrawings by viewModel.currentSymbolDrawings.collectAsStateWithLifecycle()
    val orderBook by viewModel.orderBook.collectAsStateWithLifecycle()

    val activeAlertsCount = allAlerts.count { it.isActive && !it.isTriggered }
    val currentSymbolAlerts = allAlerts.filter { it.symbol == currentSymbol.symbol && it.isActive && !it.isTriggered }

    // Trigger haptic vibration on alert
    LaunchedEffect(latestTriggerEvent) {
        if (latestTriggerEvent != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator?.vibrate(
                        VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 200), -1)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(250, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(250)
                    }
                }
            } catch (e: Exception) {
                // Ignore vibration errors if restricted
            }
        }
    }

    // Handle back button when not on Chart tab
    BackHandler(enabled = selectedTab != MainTab.CHART) {
        selectedTab = MainTab.CHART
    }

    Scaffold(
        containerColor = TvBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar(
                containerColor = TvSurface,
                contentColor = TvTextPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                // Chart Tab
                NavigationBarItem(
                    selected = selectedTab == MainTab.CHART,
                    onClick = { selectedTab = MainTab.CHART },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == MainTab.CHART) Icons.Filled.CandlestickChart else Icons.Outlined.CandlestickChart,
                            contentDescription = "Chart",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Chart",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == MainTab.CHART) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TvTextWhite,
                        selectedTextColor = TvBrandBlue,
                        indicatorColor = TvBrandBlue,
                        unselectedIconColor = TvTextSecondary,
                        unselectedTextColor = TvTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_chart_tab")
                )

                // Alerts Tab with Badge
                NavigationBarItem(
                    selected = selectedTab == MainTab.ALERTS,
                    onClick = { selectedTab = MainTab.ALERTS },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeAlertsCount > 0) {
                                    Badge(
                                        containerColor = TvAlertAmber,
                                        contentColor = Color.Black
                                    ) {
                                        Text("$activeAlertsCount", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (selectedTab == MainTab.ALERTS) Icons.Filled.NotificationsActive else Icons.Outlined.Notifications,
                                contentDescription = "Alerts",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    label = {
                        Text(
                            text = "Alerts",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == MainTab.ALERTS) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = TvAlertAmber,
                        indicatorColor = TvAlertAmber,
                        unselectedIconColor = TvTextSecondary,
                        unselectedTextColor = TvTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_alerts_tab")
                )

                // Watchlist Tab
                NavigationBarItem(
                    selected = selectedTab == MainTab.WATCHLIST,
                    onClick = { selectedTab = MainTab.WATCHLIST },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == MainTab.WATCHLIST) Icons.Filled.FormatListBulleted else Icons.Outlined.FormatListBulleted,
                            contentDescription = "Watchlist",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = "Watchlist",
                            fontSize = 11.sp,
                            fontWeight = if (selectedTab == MainTab.WATCHLIST) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = TvTextWhite,
                        selectedTextColor = TvBrandBlue,
                        indicatorColor = TvBrandBlue,
                        unselectedIconColor = TvTextSecondary,
                        unselectedTextColor = TvTextSecondary
                    ),
                    modifier = Modifier.testTag("nav_watchlist_tab")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                MainTab.CHART -> {
                    ChartScreen(
                        symbolInfo = currentSymbol,
                        candles = currentCandles,
                        timeframe = timeframe,
                        chartStyle = chartStyle,
                        indicatorSettings = indicatorSettings,
                        symbolAlerts = currentSymbolAlerts,
                        activeDrawingTool = activeDrawingTool,
                        drawings = currentDrawings,
                        connectionStatus = connectionStatus,
                        lastUpdateTimestamp = lastUpdateTimestamp,
                        isLoadingCandles = isLoadingCandles,
                        candleLoadError = candleLoadError,
                        orderBook = orderBook,
                        onSelectTimeframe = viewModel::selectTimeframe,
                        onChartStyleChange = viewModel::setChartStyle,
                        onIndicatorSettingsChange = viewModel::updateIndicatorSettings,
                        onSelectDrawingTool = viewModel::setActiveDrawingTool,
                        onAddDrawing = viewModel::addDrawing,
                        onDeleteDrawing = viewModel::deleteDrawing,
                        onClearDrawings = viewModel::clearCurrentDrawings,
                        onToggleFavorite = viewModel::toggleFavorite,
                        onOpenSymbolPicker = { showSymbolPicker = true },
                        onSaveAlert = viewModel::createAlert,
                        onDeleteAlert = viewModel::deleteAlert,
                        onRetryConnection = viewModel::refreshChartData,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                MainTab.ALERTS -> {
                    AlertsScreen(
                        alerts = allAlerts,
                        symbols = allSymbols,
                        selectedSymbol = currentSymbol,
                        onToggleAlertActive = viewModel::toggleAlertActive,
                        onDeleteAlert = viewModel::deleteAlert,
                        onClearHistory = viewModel::clearTriggeredHistory,
                        onSelectSymbolAndNavigateToChart = { sym ->
                            viewModel.selectSymbol(sym)
                            selectedTab = MainTab.CHART
                        },
                        onSaveNewAlert = viewModel::createAlert,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                MainTab.WATCHLIST -> {
                    WatchlistScreen(
                        symbols = allSymbols,
                        activeAlerts = allAlerts,
                        onSelectSymbol = { sym ->
                            viewModel.selectSymbol(sym)
                            selectedTab = MainTab.CHART
                        },
                        onToggleFavorite = viewModel::toggleFavorite,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Top popdown banner for triggered alerts
            TriggeredAlertBanner(
                event = latestTriggerEvent,
                onDismiss = viewModel::dismissTriggerEvent,
                onViewAlert = { alert ->
                    viewModel.selectSymbol(alert.symbol)
                    selectedTab = MainTab.CHART
                    viewModel.dismissTriggerEvent()
                },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
            )
        }

        if (showSymbolPicker) {
            SymbolPickerBottomSheet(
                symbols = allSymbols,
                selectedSymbolCode = currentSymbol.symbol,
                onSelectSymbol = viewModel::selectSymbol,
                onDismiss = { showSymbolPicker = false }
            )
        }
    }
}
