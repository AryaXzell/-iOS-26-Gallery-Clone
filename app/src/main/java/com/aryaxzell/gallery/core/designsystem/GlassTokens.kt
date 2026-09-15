package com.aryaxzell.gallery.core.designsystem

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class GlassTier {
    Controls, // Small glass: 16-20dp radius, 12-16dp blur
    Standard, // Medium glass: 24-28dp radius, 24-32dp blur
    Utility   // Clear glass: 8-12dp blur, overlays
}

enum class Edge {
    Top,
    Bottom
}

data class TabItem(
    val id: Int,
    val title: String,
    val icon: ImageVector
)

object GlassTokens {
    fun cornerRadius(tier: GlassTier): Dp = when (tier) {
        GlassTier.Controls -> 18.dp
        GlassTier.Standard -> 26.dp
        GlassTier.Utility -> 16.dp
    }

    fun blurRadius(tier: GlassTier): Dp = when (tier) {
        GlassTier.Controls -> 14.dp
        GlassTier.Standard -> 28.dp
        GlassTier.Utility -> 10.dp
    }

    fun tintAlpha(tier: GlassTier, isDark: Boolean): Float = if (isDark) {
        when (tier) {
            GlassTier.Controls -> 0.78f
            GlassTier.Standard -> 0.82f
            GlassTier.Utility -> 0.65f
        }
    } else {
        when (tier) {
            GlassTier.Controls -> 0.82f
            GlassTier.Standard -> 0.86f
            GlassTier.Utility -> 0.70f
        }
    }

    fun edgeHighlightAlpha(isDark: Boolean): Float = if (isDark) 0.20f else 0.50f

    val edgeBorderWidth: Dp = 0.5.dp

    fun <T> defaultSpring(): FiniteAnimationSpec<T> = spring(
        dampingRatio = 0.75f,
        stiffness = Spring.StiffnessMedium
    )

    fun <T> animationSpec(reduceMotion: Boolean): FiniteAnimationSpec<T> =
        if (reduceMotion) tween(durationMillis = 0) else defaultSpring()
}

fun tokenForTier(tier: GlassTier): Dp = GlassTokens.cornerRadius(tier)
