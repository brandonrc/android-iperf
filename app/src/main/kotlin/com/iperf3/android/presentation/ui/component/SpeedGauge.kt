package com.iperf3.android.presentation.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

// Gauge tick mark definitions: value in Mbps and its label
private data class TickMark(val value: Float, val label: String)

private val TICK_MARKS = listOf(
    TickMark(0f, "0"),
    TickMark(100f, "100"),
    TickMark(250f, "250"),
    TickMark(500f, "500"),
    TickMark(1000f, "1000"),
)

// Gauge arc colors
private val GaugeGreen = Color(0xFF4CAF50)
private val GaugeYellow = Color(0xFFFFC107)
private val GaugeBlue = Color(0xFF2196F3)

private val GaugeMutedGreen = Color(0xFF2E7D32).copy(alpha = 0.35f)
private val GaugeMutedYellow = Color(0xFFF9A825).copy(alpha = 0.35f)
private val GaugeMutedBlue = Color(0xFF1565C0).copy(alpha = 0.35f)

private val NeedleColor = Color(0xFFE0E0E0)
private val NeedleActiveColor = Color(0xFFFFFFFF)

/**
 * A custom Canvas-based semicircular speedometer gauge.
 *
 * The arc spans from 180 degrees (left) to 360 degrees (right), forming
 * a bottom-half semicircle. A gradient sweeps from green (slow) through
 * yellow to blue (fast). An animated needle points to [currentSpeed].
 *
 * @param currentSpeed Current measured speed in Mbps.
 * @param maxSpeed Upper bound of the gauge scale in Mbps.
 * @param isActive Whether the test is actively running (controls brightness).
 * @param modifier Modifier applied to the gauge container.
 */
@Composable
fun SpeedGauge(
    currentSpeed: Float,
    maxSpeed: Float = 1000f,
    isActive: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val animatedSweep = remember { Animatable(0f) }

    LaunchedEffect(currentSpeed, maxSpeed) {
        val target = (currentSpeed.coerceIn(0f, maxSpeed) / maxSpeed) * 180f
        animatedSweep.animateTo(
            targetValue = target,
            animationSpec = tween(durationMillis = 600),
        )
    }

    // Format the speed text
    val (speedText, unitText) = formatSpeed(currentSpeed)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2f) // semicircle is twice as wide as it is tall
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val strokeWidth = canvasWidth * 0.045f
            val arcPadding = strokeWidth + canvasWidth * 0.06f

            val arcSize = Size(
                width = canvasWidth - arcPadding * 2,
                height = (canvasWidth - arcPadding * 2), // full circle diameter
            )
            val arcTopLeft = Offset(
                x = arcPadding,
                y = canvasHeight - arcSize.height / 2f, // position so bottom half is visible
            )

            // --- Glow layer ---
            val glowColors = if (isActive) {
                listOf(GaugeGreen.copy(alpha = 0.25f), GaugeYellow.copy(alpha = 0.25f), GaugeBlue.copy(alpha = 0.25f))
            } else {
                listOf(GaugeMutedGreen.copy(alpha = 0.10f), GaugeMutedYellow.copy(alpha = 0.10f), GaugeMutedBlue.copy(alpha = 0.10f))
            }
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to glowColors[0],
                    0.5f to glowColors[1],
                    1.0f to glowColors[2],
                ),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth * 3f, cap = StrokeCap.Round),
            )

            // --- Background arc (track) ---
            val trackColor = if (isActive) Color(0xFF37474F) else Color(0xFF263238)
            drawArc(
                color = trackColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            // --- Active arc with gradient ---
            if (animatedSweep.value > 0.1f) {
                val gradientColors = if (isActive) {
                    listOf(GaugeGreen, GaugeYellow, GaugeBlue)
                } else {
                    listOf(GaugeMutedGreen, GaugeMutedYellow, GaugeMutedBlue)
                }
                drawArc(
                    brush = Brush.sweepGradient(
                        0.0f to gradientColors[0],
                        0.5f to gradientColors[1],
                        1.0f to gradientColors[2],
                    ),
                    startAngle = 180f,
                    sweepAngle = animatedSweep.value,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
            }

            // --- Tick marks and labels ---
            val radius = arcSize.width / 2f
            val centerX = arcTopLeft.x + arcSize.width / 2f
            val centerY = arcTopLeft.y + arcSize.height / 2f

            drawTickMarks(
                ticks = TICK_MARKS,
                maxSpeed = maxSpeed,
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                strokeWidth = strokeWidth,
                isActive = isActive,
            )

            // --- Needle ---
            drawNeedle(
                sweepAngle = animatedSweep.value,
                centerX = centerX,
                centerY = centerY,
                radius = radius,
                strokeWidth = strokeWidth,
                isActive = isActive,
            )

            // --- Center dot ---
            val dotColor = if (isActive) NeedleActiveColor else NeedleColor.copy(alpha = 0.6f)
            drawCircle(
                color = dotColor,
                radius = strokeWidth * 0.7f,
                center = Offset(centerX, centerY),
            )

            // --- Speed text ---
            drawSpeedText(
                speedText = speedText,
                unitText = unitText,
                centerX = centerX,
                centerY = centerY,
                canvasWidth = canvasWidth,
                isActive = isActive,
            )
        }
    }
}

/**
 * Draws major tick marks around the arc with labels.
 */
private fun DrawScope.drawTickMarks(
    ticks: List<TickMark>,
    maxSpeed: Float,
    centerX: Float,
    centerY: Float,
    radius: Float,
    strokeWidth: Float,
    isActive: Boolean,
) {
    val tickLength = strokeWidth * 1.2f
    val labelRadius = radius + strokeWidth * 2.5f
    val tickColor = if (isActive) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.3f)
    val labelColor = if (isActive) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.35f)

    for (tick in ticks) {
        val fraction = tick.value / maxSpeed
        val angleDeg = 180f + fraction * 180f
        val angleRad = Math.toRadians(angleDeg.toDouble())

        val outerX = centerX + (radius + strokeWidth * 0.5f) * cos(angleRad).toFloat()
        val outerY = centerY + (radius + strokeWidth * 0.5f) * sin(angleRad).toFloat()
        val innerX = centerX + (radius - tickLength) * cos(angleRad).toFloat()
        val innerY = centerY + (radius - tickLength) * sin(angleRad).toFloat()

        drawLine(
            color = tickColor,
            start = Offset(innerX, innerY),
            end = Offset(outerX, outerY),
            strokeWidth = strokeWidth * 0.18f,
            cap = StrokeCap.Round,
        )

        // Label
        val labelX = centerX + labelRadius * cos(angleRad).toFloat()
        val labelY = centerY + labelRadius * sin(angleRad).toFloat()

        drawContext.canvas.nativeCanvas.drawText(
            tick.label,
            labelX,
            labelY + strokeWidth * 0.3f,
            android.graphics.Paint().apply {
                color = labelColor.hashCode()
                textSize = strokeWidth * 0.85f
                textAlign = android.graphics.Paint.Align.CENTER
                isAntiAlias = true
                typeface = android.graphics.Typeface.create(
                    android.graphics.Typeface.DEFAULT,
                    android.graphics.Typeface.NORMAL,
                )
            },
        )
    }
}

/**
 * Draws the needle pointing at the current sweep angle.
 */
private fun DrawScope.drawNeedle(
    sweepAngle: Float,
    centerX: Float,
    centerY: Float,
    radius: Float,
    strokeWidth: Float,
    isActive: Boolean,
) {
    val needleLength = radius * 0.78f
    val needleAngleDeg = 180f + sweepAngle
    val needleColor = if (isActive) NeedleActiveColor else NeedleColor.copy(alpha = 0.5f)

    rotate(degrees = needleAngleDeg, pivot = Offset(centerX, centerY)) {
        drawLine(
            color = needleColor,
            start = Offset(centerX, centerY),
            end = Offset(centerX + needleLength, centerY),
            strokeWidth = strokeWidth * 0.22f,
            cap = StrokeCap.Round,
        )
    }
}

/**
 * Draws the current speed value and unit in the center of the gauge.
 */
private fun DrawScope.drawSpeedText(
    speedText: String,
    unitText: String,
    centerX: Float,
    centerY: Float,
    canvasWidth: Float,
    isActive: Boolean,
) {
    val speedPaint = android.graphics.Paint().apply {
        color = if (isActive) {
            android.graphics.Color.WHITE
        } else {
            android.graphics.Color.argb(100, 255, 255, 255)
        }
        textSize = canvasWidth * 0.10f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
        typeface = android.graphics.Typeface.create(
            android.graphics.Typeface.DEFAULT,
            android.graphics.Typeface.BOLD,
        )
    }

    val unitPaint = android.graphics.Paint().apply {
        color = if (isActive) {
            android.graphics.Color.argb(200, 255, 255, 255)
        } else {
            android.graphics.Color.argb(80, 255, 255, 255)
        }
        textSize = canvasWidth * 0.04f
        textAlign = android.graphics.Paint.Align.CENTER
        isAntiAlias = true
    }

    val textY = centerY - canvasWidth * 0.04f
    drawContext.canvas.nativeCanvas.drawText(speedText, centerX, textY, speedPaint)
    drawContext.canvas.nativeCanvas.drawText(unitText, centerX, textY + canvasWidth * 0.06f, unitPaint)
}

/**
 * Formats a speed in Mbps into a value/unit pair, scaling to Gbps when appropriate.
 */
private fun formatSpeed(speedMbps: Float): Pair<String, String> {
    return when {
        speedMbps >= 1000f -> Pair(String.format("%.2f", speedMbps / 1000f), "Gbps")
        speedMbps >= 100f -> Pair(String.format("%.1f", speedMbps), "Mbps")
        speedMbps >= 10f -> Pair(String.format("%.2f", speedMbps), "Mbps")
        speedMbps > 0f -> Pair(String.format("%.2f", speedMbps), "Mbps")
        else -> Pair("0.00", "Mbps")
    }
}
