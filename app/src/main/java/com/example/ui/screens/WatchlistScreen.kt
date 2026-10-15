package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssetCategory
import com.example.model.PriceAlert
import com.example.model.SymbolInfo
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

@Composable
fun WatchlistScreen(
    symbols: List<SymbolInfo>,
    activeAlerts: List<PriceAlert>,
    onSelectSymbol: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(AssetCategory.ALL) }
    var showOnlyFavorites by remember { mutableStateOf(false) }

    val filteredSymbols = symbols.filter { sym ->
        val matchesQuery = searchQuery.isBlank() ||
                sym.symbol.contains(searchQuery, ignoreCase = true) ||
                sym.name.contains(searchQuery, ignoreCase = true)
        val matchesCategory = selectedCategory == AssetCategory.ALL || sym.category == selectedCategory
        val matchesFav = !showOnlyFavorites || sym.isFavorite

        matchesQuery && matchesCategory && matchesFav
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TvBackground)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TvSurface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Markets & Watchlist",
                color = TvTextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search symbols (e.g. BTC, NVDA, EUR)", color = TvTextMuted, fontSize = 13.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = TvTextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = TvTextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = TvSurfaceVariant,
                unfocusedContainerColor = TvSurfaceVariant,
                focusedBorderColor = TvBrandBlue,
                unfocusedBorderColor = TvBorder,
                focusedTextColor = TvTextWhite,
                unfocusedTextColor = TvTextPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .testTag("symbol_search_input")
        )

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Favorites filter pill
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (showOnlyFavorites) TvAlertAmber else TvSurfaceVariant)
                    .clickable { showOnlyFavorites = !showOnlyFavorites }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (showOnlyFavorites) Color.Black else TvTextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Favorites",
                        color = if (showOnlyFavorites) Color.Black else TvTextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AssetCategory.entries.forEach { cat ->
                val isSelected = cat == selectedCategory && !showOnlyFavorites
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) TvBrandBlue else TvSurfaceVariant)
                        .clickable {
                            selectedCategory = cat
                            showOnlyFavorites = false
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cat.label,
                        color = if (isSelected) TvTextWhite else TvTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        // Symbol List
        if (filteredSymbols.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No matching symbols found",
                    color = TvTextSecondary,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredSymbols, key = { it.symbol }) { sym ->
                    val alertsCount = activeAlerts.count { it.symbol == sym.symbol && it.isActive && !it.isTriggered }
                    WatchlistItemCard(
                        symbolInfo = sym,
                        activeAlertsCount = alertsCount,
                        onClick = { onSelectSymbol(sym.symbol) },
                        onToggleFavorite = { onToggleFavorite(sym.symbol) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WatchlistItemCard(
    symbolInfo: SymbolInfo,
    activeAlertsCount: Int,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TvSurface),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("symbol_card_${symbolInfo.symbol}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Symbol + Name + Alert Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1.1f)
            ) {
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (symbolInfo.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (symbolInfo.isFavorite) TvAlertAmber else TvTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = symbolInfo.symbol,
                            color = TvTextWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )

                        if (activeAlertsCount > 0) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(TvAlertAmber.copy(alpha = 0.2f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = TvAlertAmber,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(
                                        text = "$activeAlertsCount",
                                        color = TvAlertAmber,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = symbolInfo.name,
                        color = TvTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }

            // Middle: Mini Sparkline
            Box(
                modifier = Modifier
                    .width(75.dp)
                    .height(30.dp)
                    .padding(horizontal = 4.dp)
            ) {
                MiniSparkline(
                    points = symbolInfo.sparkline,
                    isPositive = symbolInfo.isPositive
                )
            }

            // Right: Price & 24h Change Badge
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(0.9f)
            ) {
                Text(
                    text = symbolInfo.formatPrice(),
                    color = TvTextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

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
        }
    }
}

@Composable
private fun MiniSparkline(
    points: List<Double>,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    val lineColor = if (isPositive) TvBullGreen else TvBearRed

    Canvas(modifier = modifier.fillMaxSize()) {
        val minP = points.minOrNull() ?: 0.0
        val maxP = points.maxOrNull() ?: 1.0
        val range = if (maxP - minP > 0) maxP - minP else 1.0

        val stepX = size.width / (points.size - 1)
        val path = Path()

        points.forEachIndexed { i, p ->
            val x = i * stepX
            val norm = (maxP - p) / range
            val y = (norm * (size.height - 4f) + 2f).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 1.8f, cap = StrokeCap.Round)
        )
    }
}
