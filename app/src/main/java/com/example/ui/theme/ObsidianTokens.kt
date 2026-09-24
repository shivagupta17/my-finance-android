package com.example.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LocalThemeIsDark = compositionLocalOf { true }

object ObsidianDarkTokens {
    val Canvas = Color(0xFF0F1219) // Balanced deep titanium slate
    val GlassCard = Color(0xFF1A202C) // Elevated dark card surface
    val GlassSurface = Color(0xFF232A38) // Secondary surface for inner sections and chips
    val NavSurface = Color(0xFF171D27) // Floating navigation surface
    val GlassSubtle = Color(0xFF2A3446) // Interactive elevated elements
    val CardSeparator = Color(0xFF2D374A) // Crisp divider for card sections
    val CardBorder = Color(0xFF38445A) // Crisp card perimeter stroke
    val Outline = Color(0xFF38445A) // Theme outline
    val OutlineVariant = Color(0xFF2A3345) // Theme outline variant
    val AccentMint = Color(0xFF00F5A0) // Velocity, Safe Status & Gains
    val AccentTangerine = Color(0xFFFF8A3D) // Upcoming <= 48 hours
    val AccentCrimson = Color(0xFFFF2E5B) // Overdue / Critical
    val AccentBlue = Color(0xFF4E7BFF) // Category: Core Blue
    val AccentViolet = Color(0xFF9D62FF) // Category: Violet
    val AccentAmber = Color(0xFFFFC043) // Category: Amber / Subscriptions
    val TextPrimary = Color(0xFFF7F9FC)
    val TextSecondary = Color(0xFFA5B0C2)
    val TextMuted = Color(0xFF758195)
}

object ObsidianLightTokens {
    val Canvas = Color(0xFFF3F5F9) // Crisp, clean light canvas
    val GlassCard = Color(0xFFFFFFFF) // Pure white card
    val GlassSurface = Color(0xFFE8ECF3) // Soft light grey-blue surface for chips
    val NavSurface = Color(0xFFFFFFFF) // Crisp white nav pill
    val GlassSubtle = Color(0xFFDCE2EC)
    val CardSeparator = Color(0xFFDDE3EC) // Crisp separator
    val CardBorder = Color(0xFFCBD5E1) // Distinct perimeter border
    val Outline = Color(0xFF94A3B8)
    val OutlineVariant = Color(0xFFCBD5E1)
    val AccentMint = Color(0xFF00875A) // High-contrast emerald green for light canvas
    val AccentTangerine = Color(0xFFDD6B20)
    val AccentCrimson = Color(0xFFDC2626)
    val AccentBlue = Color(0xFF2563EB)
    val AccentViolet = Color(0xFF7C3AED)
    val AccentAmber = Color(0xFFD97706)
    val TextPrimary = Color(0xFF0F172A) // Deep slate black text for high readability
    val TextSecondary = Color(0xFF475569)
    val TextMuted = Color(0xFF64748B)
}

/**
 * Obsidian Glass Design System Tokens
 * Dynamically resolves between Dark and Light mode
 */
object ObsidianTokens {
    val Canvas: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.Canvas else ObsidianLightTokens.Canvas

    val GlassCard: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.GlassCard else ObsidianLightTokens.GlassCard

    val GlassSurface: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.GlassSurface else ObsidianLightTokens.GlassSurface

    val NavSurface: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.NavSurface else ObsidianLightTokens.NavSurface

    val GlassSubtle: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.GlassSubtle else ObsidianLightTokens.GlassSubtle

    val CardSeparator: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.CardSeparator else ObsidianLightTokens.CardSeparator

    val CardBorder: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.CardBorder else ObsidianLightTokens.CardBorder

    val Outline: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.Outline else ObsidianLightTokens.Outline

    val OutlineVariant: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.OutlineVariant else ObsidianLightTokens.OutlineVariant

    val AccentMint: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.AccentMint else ObsidianLightTokens.AccentMint

    val AccentTangerine: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.AccentTangerine else ObsidianLightTokens.AccentTangerine

    val AccentCrimson: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.AccentCrimson else ObsidianLightTokens.AccentCrimson

    val AccentBlue: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.AccentBlue else ObsidianLightTokens.AccentBlue

    val AccentViolet: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.AccentViolet else ObsidianLightTokens.AccentViolet

    val AccentAmber: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.AccentAmber else ObsidianLightTokens.AccentAmber

    val TextPrimary: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.TextPrimary else ObsidianLightTokens.TextPrimary

    val TextSecondary: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.TextSecondary else ObsidianLightTokens.TextSecondary

    val TextMuted: Color
        @Composable get() = if (LocalThemeIsDark.current) ObsidianDarkTokens.TextMuted else ObsidianLightTokens.TextMuted

    @Composable
    fun specularBrush(
        alphaTop: Float = 0.40f,
        alphaBottom: Float = 0.16f
    ): Brush {
        val isDark = LocalThemeIsDark.current
        val top = if (isDark) Color.White.copy(alpha = alphaTop) else Color(0xFF64748B).copy(alpha = (alphaTop * 0.7f).coerceIn(0.18f, 0.45f))
        val bottom = if (isDark) Color.White.copy(alpha = alphaBottom) else Color(0xFF94A3B8).copy(alpha = (alphaBottom * 0.7f).coerceIn(0.10f, 0.30f))
        return Brush.verticalGradient(listOf(top, bottom))
    }

    val MicroLabel: TextStyle
        @Composable get() = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.8.sp,
            color = if (LocalThemeIsDark.current) Color.White.copy(alpha = 0.75f) else Color(0xFF0F172A).copy(alpha = 0.85f)
        )

    val TabularDigits: TextStyle = TextStyle(
        fontFeatureSettings = "tnum"
    )
}

/**
 * Directional Specular Border Modifier
 * Replaces flat 1dp borders with a directional light gradient
 */
fun Modifier.specularBorder(
    shape: Shape = RoundedCornerShape(20.dp),
    borderWidth: Dp = 1.dp,
    alphaTop: Float = 0.40f,
    alphaBottom: Float = 0.16f
): Modifier = composed {
    val isDark = LocalThemeIsDark.current
    val topColor = if (isDark) Color.White.copy(alpha = alphaTop) else Color(0xFF64748B).copy(alpha = (alphaTop * 0.7f).coerceIn(0.18f, 0.45f))
    val bottomColor = if (isDark) Color.White.copy(alpha = alphaBottom) else Color(0xFF94A3B8).copy(alpha = (alphaBottom * 0.7f).coerceIn(0.10f, 0.30f))
    this.border(
        width = borderWidth,
        brush = Brush.verticalGradient(listOf(topColor, bottomColor)),
        shape = shape
    )
}

/**
 * Atmospheric Halo (Radial Glow) Modifier
 * Renders a soft atmospheric glow behind key hero components
 */
fun Modifier.atmosphericHalo(
    color: Color? = null,
    alpha: Float = 0.12f,
    radiusRatio: Float = 0.85f
): Modifier = composed {
    val activeColor = color ?: ObsidianTokens.AccentMint
    val isDark = LocalThemeIsDark.current
    val activeAlpha = if (isDark) alpha else alpha * 0.6f
    this.drawBehind {
        val radius = size.maxDimension * radiusRatio
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    activeColor.copy(alpha = activeAlpha),
                    activeColor.copy(alpha = activeAlpha * 0.4f),
                    Color.Transparent
                ),
                radius = radius,
                center = center
            ),
            radius = radius
        )
    }
}

/**
 * Tactile Elastic Bounce Click Modifier
 * Simulates high-precision mechanical response with spring physics while using
 * standard Compose clickable for bulletproof touch, mouse, and emulator event handling.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.96f,
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "obsidian_bounce_scale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClick = onClick
        )
}
