package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AlertCondition
import com.example.model.PriceAlert
import com.example.model.SymbolInfo
import com.example.ui.theme.TvAlertAmber
import com.example.ui.theme.TvBorder
import com.example.ui.theme.TvBrandBlue
import com.example.ui.theme.TvBullGreen
import com.example.ui.theme.TvSurface
import com.example.ui.theme.TvSurfaceVariant
import com.example.ui.theme.TvTextMuted
import com.example.ui.theme.TvTextPrimary
import com.example.ui.theme.TvTextSecondary
import com.example.ui.theme.TvTextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertCreateDialog(
    symbolInfo: SymbolInfo,
    initialTargetPrice: Double? = null,
    onDismiss: () -> Unit,
    onSaveAlert: (PriceAlert) -> Unit
) {
    val defaultPrice = initialTargetPrice ?: (symbolInfo.currentPrice * 1.01)
    var targetPriceText by remember {
        mutableStateOf(
            String.format(
                java.util.Locale.US,
                if (symbolInfo.priceDecimals == 4) "%.4f" else "%.2f",
                defaultPrice
            )
        )
    }

    var condition by remember {
        val cond = if (defaultPrice >= symbolInfo.currentPrice) {
            AlertCondition.CROSSING_UP
        } else {
            AlertCondition.CROSSING_DOWN
        }
        mutableStateOf(cond)
    }

    var conditionDropdownExpanded by remember { mutableStateOf(false) }
    var isOnlyOnce by remember { mutableStateOf(true) }
    var alertNote by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = TvSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(TvAlertAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Alert",
                                tint = TvAlertAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Create Price Alert",
                                color = TvTextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = "${symbolInfo.symbol} • Current: ${symbolInfo.formatPrice()}",
                                color = TvTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TvTextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Condition Selector
                Text(
                    text = "Condition",
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))

                ExposedDropdownMenuBox(
                    expanded = conditionDropdownExpanded,
                    onExpandedChange = { conditionDropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = condition.label,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = conditionDropdownExpanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = TvSurfaceVariant,
                            unfocusedContainerColor = TvSurfaceVariant,
                            focusedBorderColor = TvBrandBlue,
                            unfocusedBorderColor = TvBorder,
                            focusedTextColor = TvTextWhite,
                            unfocusedTextColor = TvTextPrimary
                        ),
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = conditionDropdownExpanded,
                        onDismissRequest = { conditionDropdownExpanded = false },
                        containerColor = TvSurfaceVariant
                    ) {
                        AlertCondition.entries.forEach { cond ->
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(cond.label, color = TvTextWhite, fontWeight = FontWeight.Bold)
                                        Text(cond.shortDesc, color = TvTextSecondary, fontSize = 11.sp)
                                    }
                                },
                                onClick = {
                                    condition = cond
                                    conditionDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Target Price Input
                Text(
                    text = "Target Price",
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = targetPriceText,
                    onValueChange = {
                        targetPriceText = it
                        errorMessage = null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                        .testTag("target_price_input")
                )

                // Quick percentage offset chips
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-2.0, -1.0, 1.0, 2.0, 5.0).forEach { pct ->
                        val sign = if (pct > 0) "+$pct%" else "$pct%"
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(TvSurfaceVariant)
                                .clickable {
                                    val newP = symbolInfo.currentPrice * (1.0 + pct / 100.0)
                                    targetPriceText = String.format(
                                        java.util.Locale.US,
                                        if (symbolInfo.priceDecimals == 4) "%.4f" else "%.2f",
                                        newP
                                    )
                                    if (pct > 0) condition = AlertCondition.CROSSING_UP else condition = AlertCondition.CROSSING_DOWN
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = sign,
                                color = if (pct > 0) TvBullGreen else TvAlertAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Trigger Frequency
                Text(
                    text = "Trigger Frequency",
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(TvSurfaceVariant)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isOnlyOnce) TvBrandBlue else Color.Transparent)
                            .clickable { isOnlyOnce = true }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Only Once",
                            color = if (isOnlyOnce) TvTextWhite else TvTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!isOnlyOnce) TvBrandBlue else Color.Transparent)
                            .clickable { isOnlyOnce = false }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Every Time",
                            color = if (!isOnlyOnce) TvTextWhite else TvTextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Alert Note / Message
                Text(
                    text = "Alert Name / Note (Optional)",
                    color = TvTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = alertNote,
                    onValueChange = { alertNote = it },
                    placeholder = { Text("e.g. Resistance breakout, Take profit", color = TvTextMuted, fontSize = 12.sp) },
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
                        .testTag("alert_note_input")
                )

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TvTextSecondary)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            val targetPrice = targetPriceText.toDoubleOrNull()
                            if (targetPrice == null || targetPrice <= 0) {
                                errorMessage = "Please enter a valid target price"
                                return@Button
                            }

                            val newAlert = PriceAlert(
                                symbol = symbolInfo.symbol,
                                targetPrice = targetPrice,
                                condition = condition,
                                note = alertNote.ifBlank { "${symbolInfo.symbol} ${condition.label} ${symbolInfo.formatPrice(targetPrice)}" },
                                isOnlyOnce = isOnlyOnce,
                                isActive = true
                            )
                            onSaveAlert(newAlert)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TvAlertAmber),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("save_alert_confirm_button")
                    ) {
                        Text(
                            text = "Save Alert",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
