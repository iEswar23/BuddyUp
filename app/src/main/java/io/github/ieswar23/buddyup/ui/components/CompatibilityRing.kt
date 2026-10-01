package io.github.ieswar23.buddyup.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Circular progress ring that animates up to the compatibility percentage. */
@Composable
fun CompatibilityRing(
    percent: Int,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    trackColor: Color = Color.White.copy(alpha = 0.3f),
    progressColor: Color = Color.White,
    textColor: Color = Color.White,
) {
    val animated = remember(percent) { Animatable(0f) }
    LaunchedEffect(percent) {
        animated.animateTo(percent / 100f, tween(durationMillis = 900, easing = FastOutSlowInEasing))
    }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = this.size.minDimension * 0.1f, cap = StrokeCap.Round)
            drawArc(trackColor, startAngle = 0f, sweepAngle = 360f, useCenter = false, style = stroke)
            drawArc(
                progressColor,
                startAngle = -90f,
                sweepAngle = 360f * animated.value,
                useCenter = false,
                style = stroke,
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${(animated.value * 100).toInt()}%",
                color = textColor,
                fontWeight = FontWeight.ExtraBold,
                fontSize = (size.value * 0.26f).sp,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "match",
                color = textColor.copy(alpha = 0.85f),
                fontSize = (size.value * 0.13f).sp,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
