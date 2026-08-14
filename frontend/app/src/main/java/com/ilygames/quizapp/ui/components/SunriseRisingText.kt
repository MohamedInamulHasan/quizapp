package com.ilygames.quizapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Sunrise Text Effect: Animates each character rising up one by one from below
 * with a bouncy spring animation and a warm sunrise color gradient.
 */
@Composable
fun SunriseRisingText(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 32.sp,
    fontWeight: FontWeight = FontWeight.Black,
    staggerDelayMs: Long = 70L
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        text.forEachIndexed { index, char ->
            var visible by remember { mutableStateOf(false) }

            LaunchedEffect(text) {
                delay(index * staggerDelayMs)
                visible = true
            }

            val offsetY by animateFloatAsState(
                targetValue = if (visible) 0f else 40f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                label = "offset_$index"
            )

            val alpha by animateFloatAsState(
                targetValue = if (visible) 1f else 0f,
                animationSpec = tween(durationMillis = 350),
                label = "alpha_$index"
            )

            // Sunrise color gradient across letters: Red-Orange → Golden Sunrise → Sun Yellow
            val totalLen = text.length.coerceAtLeast(1)
            val factor = index.toFloat() / totalLen.toFloat()
            val letterColor = when {
                factor < 0.4f -> Color(0xFFFF512F)
                factor < 0.7f -> Color(0xFFF09819)
                else -> Color(0xFFFFD700)
            }

            Text(
                text = if (char == ' ') " " else char.toString(),
                fontSize = fontSize,
                fontWeight = fontWeight,
                color = letterColor,
                modifier = Modifier.graphicsLayer {
                    translationY = offsetY
                    this.alpha = alpha
                }
            )
        }
    }
}
