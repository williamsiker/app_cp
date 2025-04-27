package com.example.lancelot.utils

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.*

data class HSVColor(
    val hue: Float,
    val saturation: Float,
    val value: Float
)

fun Color.toHexString(): String {
    val red = (red * 255).toInt()
    val green = (green * 255).toInt()
    val blue = (blue * 255).toInt()
    return String.format("#%02X%02X%02X", red, green, blue)
}

fun HSVColor.toColor(): Color {
    val h = hue * 360f
    val s = saturation
    val v = value

    val c = v * s
    val x = c * (1f - abs((h / 60f % 2f) - 1f))
    val m = v - c

    val (r, g, b) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    return Color(
        red = r + m,
        green = g + m,
        blue = b + m,
        alpha = 1f
    )
}

fun Color.toHSVColor(): HSVColor {
    val r = red
    val g = green
    val b = blue

    val cmax = maxOf(r, g, b)
    val cmin = minOf(r, g, b)
    val diff = cmax - cmin

    val hue = when (cmax) {
        cmin -> 0f
        r -> (60f * ((g - b) / diff) + 360f) % 360f
        g -> 60f * ((b - r) / diff) + 120f
        else -> 60f * ((r - g) / diff) + 240f
    }

    val saturation = if (cmax == 0f) 0f else diff / cmax
    val value = cmax

    return HSVColor(
        hue = hue / 360f,
        saturation = saturation,
        value = value
    )
}

@Composable
fun ColorWheel(
    modifier: Modifier = Modifier,
    initialColor: Color,
    onColorChanged: (HSVColor) -> Unit
) {
    var currentHSVColor by remember { mutableStateOf(initialColor.toHSVColor()) }
    var centerPoint by remember { mutableStateOf(Offset.Zero) }
    var radius by remember { mutableStateOf(0f) }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    val position = change.position
                    val vector = position - centerPoint
                    val distance = vector.getDistance()
                    
                    if (distance <= radius) {
                        val angle = atan2(vector.y, vector.x)
                        val hue = ((angle * 180f / PI.toFloat() + 180f) / 360f)
                        val saturation = (distance / radius).coerceIn(0f, 1f)
                        
                        currentHSVColor = currentHSVColor.copy(
                            hue = hue,
                            saturation = saturation
                        )
                        onColorChanged(currentHSVColor)
                    }
                }
            }
    ) {
        centerPoint = center
        radius = size.minDimension / 2f - 5.dp.toPx()

        // Dibujar el círculo de colores
        for (angle in 0..360 step 1) {
            val angleRadians = angle * PI.toFloat() / 180f
            val sweepGradient = Brush.radialGradient(
                colors = listOf(
                    HSVColor(angle / 360f, 1f, 1f).toColor(),
                    Color.White
                ),
                center = centerPoint,
                radius = radius
            )

            drawArc(
                brush = sweepGradient,
                startAngle = angle.toFloat(),
                sweepAngle = 1f,
                useCenter = true,
                alpha = 0.99f
            )
        }

        // Dibujar el punto de selección
        val selectedAngle = currentHSVColor.hue * 360f * PI.toFloat() / 180f
        val selectedRadius = currentHSVColor.saturation * radius
        val selectedPoint = Offset(
            x = centerPoint.x + cos(selectedAngle) * selectedRadius,
            y = centerPoint.y + sin(selectedAngle) * selectedRadius
        )

        drawCircle(
            color = Color.White,
            radius = 8.dp.toPx(),
            center = selectedPoint,
            style = Stroke(2.dp.toPx())
        )
    }
}