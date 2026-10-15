package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.OrderBookData
import com.example.model.SymbolInfo
import com.example.ui.theme.TvBackground
import com.example.ui.theme.TvBearRed
import com.example.ui.theme.TvBullGreen
import com.example.ui.theme.TvDivider
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvTextWhite
import kotlin.math.max
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderBookBottomSheet(
    symbolInfo: SymbolInfo,
    orderBook: OrderBookData?,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = TvSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "${symbolInfo.symbol} Order Book",
                        color = TvTextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                    Text(
                        text = "Source: Binance Public Spot L2 Order Book",
                        color = TvTextSecondary,
                        fontSize = 11.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TvTextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 24h Stats summary row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TvSurfaceVariant, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("24h High", color = TvTextSecondary, fontSize = 10.sp)
                    Text(symbolInfo.formatPrice(symbolInfo.high24h), color = TvTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("24h Low", color = TvTextSecondary, fontSize = 10.sp)
                    Text(symbolInfo.formatPrice(symbolInfo.low24h), color = TvTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("24h Vol (USDT)", color = TvTextSecondary, fontSize = 10.sp)
                    Text(symbolInfo.formatVolume(), color = TvTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (orderBook == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Loading real order book from Binance...", color = TvTextSecondary, fontSize = 12.sp)
                }
            } else {
                // Spread Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Spread: ${symbolInfo.formatPrice(orderBook.spread)} (${String.format("%.3f", orderBook.spreadPercent)}%)",
                        color = TvTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Bid Qty", color = TvTextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("Bid (USDT)", color = TvBullGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Ask (USDT)", color = TvBearRed, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Ask Qty", color = TvTextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                }

                Divider(color = TvDivider, thickness = 1.dp)

                val rowCount = min(12, max(orderBook.bids.size, orderBook.asks.size))
                val maxQty = max(
                    orderBook.bids.take(rowCount).maxOfOrNull { it.quantity } ?: 1.0,
                    orderBook.asks.take(rowCount).maxOfOrNull { it.quantity } ?: 1.0
                )

                for (i in 0 until rowCount) {
                    val bid = orderBook.bids.getOrNull(i)
                    val ask = orderBook.asks.getOrNull(i)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Bid Quantity
                        Text(
                            text = if (bid != null) String.format("%.4f", bid.quantity) else "-",
                            color = TvTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )

                        // Bid Price
                        Text(
                            text = if (bid != null) symbolInfo.formatPrice(bid.price) else "-",
                            color = TvBullGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )

                        // Ask Price
                        Text(
                            text = if (ask != null) symbolInfo.formatPrice(ask.price) else "-",
                            color = TvBearRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )

                        // Ask Quantity
                        Text(
                            text = if (ask != null) String.format("%.4f", ask.quantity) else "-",
                            color = TvTextSecondary,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
