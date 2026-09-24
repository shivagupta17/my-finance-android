package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ObsidianTokens
import com.example.ui.theme.atmosphericHalo
import com.example.ui.theme.bounceClick
import com.example.ui.theme.specularBorder
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Obsidian Outflow Bento Hero
 * An award-winning luxury glassmorphic card presenting burn velocity,
 * editorial typography, segmented outflow bar, and category allocation chips.
 */
@Composable
fun ObsidianSpendHero(
    selectedMonthYear: String,
    totalSpentOverall: Double,
    totalSpentBill: Double,
    totalSpentSub: Double,
    totalSpentExpense: Double,
    onMonthSelectorClick: () -> Unit,
    onViewAnalyticsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate Burn Velocity (₹/day based on current day of month)
    val calendar = Calendar.getInstance()
    val currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH).coerceAtLeast(1)
    val dailyVelocity = totalSpentOverall / currentDayOfMonth

    // Formatted Month Display (e.g., "September 2026")
    val monthDisplayName = remember(selectedMonthYear) {
        try {
            val date = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(selectedMonthYear)
            if (date != null) {
                SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(date)
            } else {
                selectedMonthYear
            }
        } catch (e: Exception) {
            selectedMonthYear
        }
    }

    // Category percentage calculations
    val (billPercent, subPercent, expensePercent) = remember(
        totalSpentOverall,
        totalSpentBill,
        totalSpentSub,
        totalSpentExpense
    ) {
        if (totalSpentOverall > 0) {
            Triple(
                ((totalSpentBill / totalSpentOverall) * 100).toInt(),
                ((totalSpentSub / totalSpentOverall) * 100).toInt(),
                ((totalSpentExpense / totalSpentOverall) * 100).toInt()
            )
        } else {
            Triple(0, 0, 0)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .atmosphericHalo(
                color = ObsidianTokens.AccentMint,
                alpha = 0.12f,
                radiusRatio = 0.95f
            )
            .clip(RoundedCornerShape(24.dp))
            .background(ObsidianTokens.GlassCard)
            .specularBorder(
                shape = RoundedCornerShape(24.dp),
                borderWidth = 1.25.dp,
                alphaTop = 0.45f,
                alphaBottom = 0.18f
            )
            .testTag("obsidian_spend_hero")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // TOP HEADER ROW: Month Selector Pill + Burn Velocity Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Frosted Month Selector Capsule
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(ObsidianTokens.GlassSurface)
                        .specularBorder(
                            shape = RoundedCornerShape(100.dp),
                            borderWidth = 1.dp,
                            alphaTop = 0.35f,
                            alphaBottom = 0.15f
                        )
                        .bounceClick(scaleDown = 0.94f) { onMonthSelectorClick() }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .testTag("hero_month_selector_pill"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Select Month",
                        tint = ObsidianTokens.TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = monthDisplayName,
                        style = ObsidianTokens.MicroLabel.copy(
                            color = ObsidianTokens.TextPrimary,
                            letterSpacing = 0.5.sp,
                            fontSize = 12.sp
                        )
                    )
                }

                // Active Burn Velocity Badge (Electric Mint)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(ObsidianTokens.AccentMint.copy(alpha = 0.16f))
                        .specularBorder(
                            shape = RoundedCornerShape(100.dp),
                            borderWidth = 1.dp,
                            alphaTop = 0.50f,
                            alphaBottom = 0.20f
                        )
                        .bounceClick(scaleDown = 0.94f) { onViewAnalyticsClick() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("hero_burn_velocity_badge"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(ObsidianTokens.AccentMint)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.TrendingUp,
                        contentDescription = null,
                        tint = ObsidianTokens.AccentMint,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "₹${String.format(Locale.getDefault(), "%,.0f", dailyVelocity)}/day",
                        style = ObsidianTokens.TabularDigits.copy(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTokens.AccentMint
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MICRO-LABEL
            Text(
                text = "TOTAL MONTHLY OUTFLOW",
                style = ObsidianTokens.MicroLabel
            )

            Spacer(modifier = Modifier.height(4.dp))

            // EDITORIAL SCALE CURRENCY DISPLAY
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewAnalyticsClick() },
                verticalAlignment = Alignment.Top
            ) {
                // Top-aligned Currency Symbol (₹)
                Text(
                    text = "₹",
                    style = ObsidianTokens.TabularDigits.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.50f)
                    ),
                    modifier = Modifier.padding(top = 4.dp, end = 2.dp)
                )

                // Editorial Scale Tabular Digits
                Text(
                    text = String.format(Locale.getDefault(), "%,.2f", totalSpentOverall),
                    style = ObsidianTokens.TabularDigits.copy(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1.5).sp,
                        color = ObsidianTokens.TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // SEGMENTED HORIZONTAL OUTFLOW BAR (Thin 4.dp gaps between slices)
            val hasOutflow = totalSpentOverall > 0
            val safeBillWeight = if (hasOutflow && totalSpentBill > 0) totalSpentBill.toFloat() else if (!hasOutflow) 1f else 0f
            val safeSubWeight = if (hasOutflow && totalSpentSub > 0) totalSpentSub.toFloat() else if (!hasOutflow) 1f else 0f
            val safeExpenseWeight = if (hasOutflow && totalSpentExpense > 0) totalSpentExpense.toFloat() else if (!hasOutflow) 1f else 0f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(ObsidianTokens.CardSeparator),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (safeBillWeight > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(safeBillWeight)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (hasOutflow) ObsidianTokens.AccentBlue else Color.White.copy(alpha = 0.08f))
                    )
                }
                if (safeSubWeight > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(safeSubWeight)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (hasOutflow) ObsidianTokens.AccentViolet else Color.White.copy(alpha = 0.08f))
                    )
                }
                if (safeExpenseWeight > 0f) {
                    Box(
                        modifier = Modifier
                            .weight(safeExpenseWeight)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (hasOutflow) ObsidianTokens.AccentMint else Color.White.copy(alpha = 0.08f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // CLEAN ALLOCATION FOOTER CHIPS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Bills Slice
                AllocationChip(
                    label = "BILLS",
                    percent = billPercent,
                    amount = totalSpentBill,
                    dotColor = ObsidianTokens.AccentBlue
                )

                // Subscriptions Slice
                AllocationChip(
                    label = "SUBS",
                    percent = subPercent,
                    amount = totalSpentSub,
                    dotColor = ObsidianTokens.AccentViolet
                )

                // Expenses Slice
                AllocationChip(
                    label = "EXPENSES",
                    percent = expensePercent,
                    amount = totalSpentExpense,
                    dotColor = ObsidianTokens.AccentMint,
                    alignEnd = true
                )
            }
        }
    }
}

@Composable
private fun AllocationChip(
    label: String,
    percent: Int,
    amount: Double,
    dotColor: Color,
    alignEnd: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "$label • $percent%",
                style = ObsidianTokens.MicroLabel.copy(
                    fontSize = 10.sp,
                    color = ObsidianTokens.TextSecondary,
                    letterSpacing = 1.2.sp
                )
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "₹${String.format(Locale.getDefault(), "%,.0f", amount)}",
            style = ObsidianTokens.TabularDigits.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = ObsidianTokens.TextPrimary
            )
        )
    }
}
