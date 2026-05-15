package com.songapp.ktv.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// "Aurora KTV" palette — deep night sky with neon accents
val NightInkDeep = Color(0xFF0A0717)
val NightInk = Color(0xFF14102B)
val NightInkSoft = Color(0xFF1F1A3D)

val NeonViolet = Color(0xFFB388FF)
val NeonPink = Color(0xFFFF6CC4)
val NeonCyan = Color(0xFF6CE5FF)
val NeonGold = Color(0xFFFFD58E)

val OnSurfaceHigh = Color(0xFFF6F2FF)
val OnSurfaceMed = Color(0xFFC8C0E2)
val OnSurfaceDim = Color(0xFF8B82AD)

val GlassStroke = Color(0x33FFFFFF)
val GlassFill = Color(0x14FFFFFF)
val GlassFillStrong = Color(0x22FFFFFF)

fun auroraBackground(): Brush = Brush.linearGradient(
    0.0f to Color(0xFF0A0717),
    0.45f to Color(0xFF1A0F3A),
    1.0f to Color(0xFF260A2F)
)

fun primaryGradient(): Brush = Brush.linearGradient(
    listOf(NeonPink, NeonViolet, NeonCyan)
)

fun cardGradient(): Brush = Brush.linearGradient(
    listOf(Color(0x33B388FF), Color(0x1A6CE5FF))
)
