package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ObsidianTokens
import com.example.ui.theme.bounceClick
import com.example.ui.theme.specularBorder

data class NavDestination(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val testTag: String
)

val ObsidianNavDestinations = listOf(
    NavDestination("Home", "Home", Icons.Default.Home, "tab_home"),
    NavDestination("Dashboard", "Analytics", Icons.Default.Dashboard, "tab_dashboard"),
    NavDestination("Bills", "Bills", Icons.AutoMirrored.Filled.ReceiptLong, "tab_bills"),
    NavDestination("Subscriptions", "Subs", Icons.Default.Autorenew, "tab_subscriptions"),
    NavDestination("Expenses", "Spend", Icons.Default.ShoppingCart, "tab_expenses")
)

/**
 * Floating Glass Island Navigation Capsule
 * Floating pill-shaped bottom bar docked above window insets with specular gradient
 * border and spring-animated active destination pills.
 */
@Composable
fun FloatingGlassNavBar(
    currentTab: String,
    onTabSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("bottom_nav_bar"),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(100.dp),
                    spotColor = Color.Black.copy(alpha = 0.22f),
                    ambientColor = Color.Black.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(100.dp))
                .background(ObsidianTokens.NavSurface)
                .specularBorder(
                    shape = RoundedCornerShape(100.dp),
                    borderWidth = 1.dp,
                    alphaTop = 0.45f,
                    alphaBottom = 0.18f
                )
                .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ObsidianNavDestinations.forEach { dest ->
                val isSelected = currentTab == dest.id

                val activePillBg by animateColorAsState(
                    targetValue = if (isSelected) {
                        ObsidianTokens.AccentMint.copy(alpha = 0.14f)
                    } else {
                        Color.Transparent
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    label = "nav_pill_bg"
                )

                val activeIconTint by animateColorAsState(
                    targetValue = if (isSelected) {
                        ObsidianTokens.AccentMint
                    } else {
                        ObsidianTokens.TextMuted
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "nav_icon_tint"
                )

                val activeTextColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        ObsidianTokens.TextPrimary
                    } else {
                        ObsidianTokens.TextMuted
                    },
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "nav_text_color"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(100.dp))
                        .background(activePillBg)
                        .then(
                            if (isSelected) {
                                Modifier.specularBorder(
                                    shape = RoundedCornerShape(100.dp),
                                    borderWidth = 1.dp,
                                    alphaTop = 0.45f,
                                    alphaBottom = 0.15f
                                )
                            } else Modifier
                        )
                        .bounceClick(scaleDown = 0.92f) {
                            onTabSelected(dest.id)
                        }
                        .testTag(dest.testTag),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = dest.label,
                            tint = activeIconTint,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = dest.label,
                            style = ObsidianTokens.MicroLabel.copy(
                                fontSize = 10.sp,
                                letterSpacing = 0.8.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = activeTextColor
                            ),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
