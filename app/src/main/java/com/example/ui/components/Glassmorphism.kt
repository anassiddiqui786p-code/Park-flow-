package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp, // Premium 24.dp matching rounded-3xl
    borderWidth: Dp = 1.dp,
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)

    // Real frosted glass transparency: bg-white/40 (light mode) and bg-white/12 (dark mode)
    val glassBg = if (isDark) {
        Color(0x1EFFFFFF) // Translucent dark (12% alpha white overlay)
    } else {
        Color(0x59FFFFFF) // Translucent light (35% alpha white overlay)
    }

    val borderBrush = Brush.linearGradient(
        colors = if (isDark) {
            listOf(Color(0x26FFFFFF), Color(0x0AFFFFFF)) // Soft translucent white border
        } else {
            listOf(Color(0x80FFFFFF), Color(0x1AFFFFFF)) // Light translucent border (50% to 10% alpha)
        }
    )

    Box(
        modifier = modifier
            .shadow(
                elevation = 12.dp, // Modern subtle elevation
                shape = RoundedCornerShape(cornerRadius),
                clip = false,
                ambientColor = Color.Black.copy(alpha = 0.04f),
                spotColor = Color.Black.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(glassBg)
            .border(borderWidth, borderBrush, RoundedCornerShape(cornerRadius))
            .padding(16.dp)
    ) {
        content()
    }
}

@Composable
fun PremiumBackground(
    content: @Composable () -> Unit
) {
    val isDark = MaterialTheme.colorScheme.background == Color(0xFF0F172A)

    // Real-time gradient brushes matching the Frosted Glass mockup diagonal linear flow
    val gradient = if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0F172A), // Deep Slate Navy
                Color(0xFF1E1B4B), // Deep Purple-Indigo (indigo-950)
                Color(0xFF0F172A)
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFFE3F2FD), // Soft pastel blue (from-[#E3F2FD])
                Color(0xFFF3E5F5), // Soft pastel purple (via-[#F3E5F5])
                Color(0xFFE8EAF6)  // Soft pastel indigo-grey (to-[#E8EAF6])
            ),
            start = Offset(0f, 0f),
            end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradient)
    ) {
        content()
    }
}
