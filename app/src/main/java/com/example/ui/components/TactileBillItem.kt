package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bill
import com.example.ui.theme.ObsidianTokens
import com.example.ui.theme.bounceClick
import com.example.ui.theme.specularBorder
import com.example.viewmodel.UpcomingPaymentItem
import java.util.Locale

/**
 * Tactile Bill / Subscription Item Card
 * Features isolated obsidian glass styling, squircle icon badge,
 * kinetic urgency semantic states, pulsing micro-dots, tabular numbers,
 * and elastic micro-physics.
 */
@Composable
fun TactileBillItem(
    item: UpcomingPaymentItem,
    isPaidMode: Boolean = false,
    onPayClick: (() -> Unit)? = null,
    onSkipClick: (() -> Unit)? = null,
    onCardClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isBill = item.itemType == "BILL"
    val isOverdue = item.isOverdue && !isPaidMode && !item.isSkipped
    val isUrgent48h = !isPaidMode && !item.isSkipped && !isOverdue && item.daysRemaining <= 2

    val parentItem = item.parentItem
    val isVariableBill = isBill && parentItem is Bill && parentItem.isVariable

    // Determine Semantic Urgency Accent
    val urgencyAccent = when {
        item.isSkipped -> ObsidianTokens.TextMuted
        isPaidMode -> ObsidianTokens.AccentMint
        isOverdue -> ObsidianTokens.AccentCrimson
        isUrgent48h -> ObsidianTokens.AccentTangerine
        else -> ObsidianTokens.AccentMint
    }

    // Squircle Category Accent Color
    val squircleColor = when {
        isPaidMode -> ObsidianTokens.AccentMint
        isOverdue -> ObsidianTokens.AccentCrimson
        isBill -> ObsidianTokens.AccentBlue
        else -> ObsidianTokens.AccentViolet
    }

    // Pulsing micro-dot animation for critical / due-soon alerts
    val infiniteTransition = rememberInfiniteTransition(label = "micro_dot_pulse")
    val dotScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_scale"
    )
    val dotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(ObsidianTokens.GlassCard)
            .specularBorder(
                shape = RoundedCornerShape(18.dp),
                borderWidth = 1.25.dp,
                alphaTop = if (isOverdue) 0.55f else if (isUrgent48h) 0.45f else 0.35f,
                alphaBottom = if (isOverdue) 0.25f else 0.16f
            )
            .bounceClick(scaleDown = 0.98f) {
                onCardClick?.invoke()
            }
            .testTag("upcoming_payment_row_${item.itemType}_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SQUIRCLE BRAND / CATEGORY BADGE (RoundedCornerShape 14.dp)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(squircleColor.copy(alpha = 0.16f))
                    .specularBorder(
                        shape = RoundedCornerShape(14.dp),
                        borderWidth = 1.dp,
                        alphaTop = 0.35f,
                        alphaBottom = 0.14f
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        item.isSkipped -> Icons.Default.Close
                        isPaidMode -> Icons.Default.CheckCircle
                        isOverdue -> Icons.Default.Warning
                        isBill -> Icons.Default.Receipt
                        else -> Icons.Default.CreditCard
                    },
                    contentDescription = item.itemType,
                    tint = squircleColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // TITLE & METADATA DETAILS
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = ObsidianTokens.TabularDigits.copy(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ObsidianTokens.TextPrimary
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Type Capsule (Bill or Sub)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                (if (isBill) ObsidianTokens.AccentBlue else ObsidianTokens.AccentViolet).copy(alpha = 0.15f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isBill) "BILL" else "SUB",
                            style = ObsidianTokens.MicroLabel.copy(
                                fontSize = 9.sp,
                                letterSpacing = 0.5.sp,
                                color = if (isBill) ObsidianTokens.AccentBlue else ObsidianTokens.AccentViolet
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Subtitle / Extra Information
                Text(
                    text = item.extraInfo,
                    style = ObsidianTokens.MicroLabel.copy(
                        fontSize = 11.sp,
                        letterSpacing = 0.4.sp,
                        fontWeight = FontWeight.Normal,
                        color = ObsidianTokens.TextSecondary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // DURATION & URGENCIES STATUS ROW (with pulsing micro-dot if urgent)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isOverdue || isUrgent48h) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .scale(dotScale)
                                .clip(CircleShape)
                                .background(urgencyAccent.copy(alpha = dotAlpha))
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    } else if (isPaidMode) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(ObsidianTokens.AccentMint)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                    }

                    val dueStatusText = when {
                        item.isSkipped -> "Skipped this cycle"
                        isPaidMode -> "Payment Recorded"
                        isOverdue -> {
                            val parent = item.parentItem
                            if (isBill && parent is Bill) "Overdue (Due Day ${parent.dueDay})"
                            else "Overdue!"
                        }
                        item.daysRemaining == 0 -> "Due today!"
                        item.daysRemaining == 1 -> "Due tomorrow"
                        isBill && item.parentItem is Bill -> {
                            val parent = item.parentItem as Bill
                            "Due in ${item.daysRemaining} days (Day ${parent.dueDay})"
                        }
                        else -> "Due in ${item.daysRemaining} days"
                    }

                    Text(
                        text = dueStatusText,
                        style = ObsidianTokens.MicroLabel.copy(
                            fontSize = 10.sp,
                            letterSpacing = 0.4.sp,
                            fontWeight = if (isOverdue || isUrgent48h || isPaidMode) FontWeight.Bold else FontWeight.Medium,
                            color = urgencyAccent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // RIGHT SECTION: TABULAR AMOUNT + ACTION BUTTONS
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                // Tabular Amount display
                val costDisplay = when {
                    item.isSkipped -> "Skipped"
                    isPaidMode -> "₹${String.format(Locale.getDefault(), "%,.2f", item.amount)}"
                    isVariableBill && item.amount <= 0.0 -> "Variable"
                    else -> "₹${String.format(Locale.getDefault(), "%,.2f", item.amount)}"
                }

                Text(
                    text = costDisplay,
                    style = ObsidianTokens.TabularDigits.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isSkipped) ObsidianTokens.TextMuted else ObsidianTokens.TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Action Buttons Row
                if (!isPaidMode && !item.isSkipped) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Skip button (Bills only)
                        if (isBill && onSkipClick != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(ObsidianTokens.GlassSurface)
                                    .specularBorder(
                                        shape = RoundedCornerShape(100.dp),
                                        borderWidth = 1.dp,
                                        alphaTop = 0.35f,
                                        alphaBottom = 0.12f
                                    )
                                    .bounceClick(scaleDown = 0.90f) { onSkipClick() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("skip_button_${item.itemType}_${item.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Skip",
                                        tint = ObsidianTokens.TextSecondary,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Skip",
                                        style = ObsidianTokens.MicroLabel.copy(
                                            fontSize = 9.sp,
                                            letterSpacing = 0.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = ObsidianTokens.TextSecondary
                                        )
                                    )
                                }
                            }
                        }

                        // Pay Action Pill Button
                        if (onPayClick != null) {
                            val actionColor = if (isBill) ObsidianTokens.AccentMint else ObsidianTokens.AccentViolet
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(actionColor.copy(alpha = 0.18f))
                                    .specularBorder(
                                        shape = RoundedCornerShape(100.dp),
                                        borderWidth = 1.dp,
                                        alphaTop = 0.45f,
                                        alphaBottom = 0.15f
                                    )
                                    .bounceClick(scaleDown = 0.90f) { onPayClick() }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("action_button_${item.itemType}_${item.id}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Pay",
                                        tint = actionColor,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isBill) "Pay" else "Renew",
                                        style = ObsidianTokens.MicroLabel.copy(
                                            fontSize = 10.sp,
                                            letterSpacing = 0.8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = actionColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                } else {
                    if (isBill && onPayClick != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(ObsidianTokens.GlassSurface)
                                .specularBorder(
                                    shape = RoundedCornerShape(100.dp),
                                    borderWidth = 1.dp,
                                    alphaTop = 0.35f,
                                    alphaBottom = 0.12f
                                )
                                .bounceClick(scaleDown = 0.90f) {
                                    if (item.isSkipped) {
                                        onSkipClick?.invoke()
                                    } else {
                                        onPayClick()
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                .testTag("unpay_button_${item.itemType}_${item.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (item.isSkipped) "Unskip" else "Unpay",
                                style = ObsidianTokens.MicroLabel.copy(
                                    fontSize = 9.sp,
                                    letterSpacing = 0.5.sp,
                                    color = if (item.isSkipped) ObsidianTokens.TextMuted else ObsidianTokens.AccentCrimson
                                )
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(ObsidianTokens.AccentMint.copy(alpha = 0.12f))
                                .padding(horizontal = 8.dp, vertical = 3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "SETTLED",
                                style = ObsidianTokens.MicroLabel.copy(
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp,
                                    color = ObsidianTokens.AccentMint
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
