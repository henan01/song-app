package com.songapp.ktv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.songapp.ktv.ui.theme.NeonCyan
import com.songapp.ktv.ui.theme.NeonPink
import com.songapp.ktv.ui.theme.NeonViolet
import com.songapp.ktv.ui.theme.NightInkDeep
import com.songapp.ktv.ui.theme.auroraBackground
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AuroraBackground(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(NightInkDeep)) {
        Box(modifier = Modifier.fillMaxSize().background(auroraBackground()))
        AuroraBlobs()
        content()
    }
}

@Composable
private fun AuroraBlobs() {
    val transition = rememberInfiniteTransition(label = "aurora")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 18000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aurora-t"
    )
    Canvas(modifier = Modifier.fillMaxSize().blur(80.dp)) {
        val w = size.width
        val h = size.height
        val twoPi = (2.0 * Math.PI).toFloat()
        val cx1 = w * (0.25f + 0.15f * sin((t * twoPi).toDouble()).toFloat())
        val cy1 = h * (0.20f + 0.10f * cos((t * twoPi).toDouble()).toFloat())
        drawCircle(
            brush = Brush.radialGradient(
                listOf(NeonPink.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(cx1, cy1),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(cx1, cy1)
        )
        val cx2 = w * (0.85f - 0.20f * sin((t * twoPi * 0.7f).toDouble()).toFloat())
        val cy2 = h * (0.55f + 0.18f * cos((t * twoPi * 0.7f).toDouble()).toFloat())
        drawCircle(
            brush = Brush.radialGradient(
                listOf(NeonViolet.copy(alpha = 0.55f), Color.Transparent),
                center = Offset(cx2, cy2),
                radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = Offset(cx2, cy2)
        )
        val cx3 = w * (0.50f + 0.35f * cos((t * twoPi * 0.4f).toDouble()).toFloat())
        val cy3 = h * (0.85f - 0.10f * sin((t * twoPi * 0.4f).toDouble()).toFloat())
        drawCircle(
            brush = Brush.radialGradient(
                listOf(NeonCyan.copy(alpha = 0.40f), Color.Transparent),
                center = Offset(cx3, cy3),
                radius = w * 0.60f
            ),
            radius = w * 0.60f,
            center = Offset(cx3, cy3)
        )
    }
}

@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
    ) { content() }
}
