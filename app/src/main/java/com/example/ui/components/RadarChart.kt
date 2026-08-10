package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

data class RadarMetric(
    val label: String,
    val score: Float // 0 to 100
)

@Composable
fun RadarChart(
    metrics: List<RadarMetric>,
    modifier: Modifier = Modifier.height(240.dp),
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    gridColor: Color = MaterialTheme.colorScheme.outlineVariant
) {
    val count = metrics.size.coerceAtLeast(3)

    Box(modifier = modifier.padding(16.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = (minOf(size.width, size.height) / 2f) * 0.7f

            // Draw concentric background web grids (25%, 50%, 75%, 100%)
            val levels = listOf(0.25f, 0.50f, 0.75f, 1.0f)
            for (level in levels) {
                val gridPath = Path()
                for (i in 0 until count) {
                    val angle = Math.toRadians((i * 360.0 / count) - 90.0)
                    val x = center.x + (radius * level * cos(angle)).toFloat()
                    val y = center.y + (radius * level * sin(angle)).toFloat()
                    if (i == 0) gridPath.moveTo(x, y) else gridPath.lineTo(x, y)
                }
                gridPath.close()
                drawPath(path = gridPath, color = gridColor, style = Stroke(width = 1.dp.toPx()))
            }

            // Draw axis spokes
            for (i in 0 until count) {
                val angle = Math.toRadians((i * 360.0 / count) - 90.0)
                val x = center.x + (radius * cos(angle)).toFloat()
                val y = center.y + (radius * sin(angle)).toFloat()
                drawLine(color = gridColor, start = center, end = Offset(x, y), strokeWidth = 1.dp.toPx())

                // Draw labels
                val labelX = center.x + ((radius + 32.dp.toPx()) * cos(angle)).toFloat()
                val labelY = center.y + ((radius + 16.dp.toPx()) * sin(angle)).toFloat()
                val metric = metrics.getOrNull(i)
                if (metric != null) {
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.WHITE
                            textSize = 28f
                            textAlign = android.graphics.Paint.Align.CENTER
                            isFakeBoldText = true
                        }
                        drawText("${metric.label} (${metric.score.toInt()})", labelX, labelY, paint)
                    }
                }
            }

            // Draw Score Polygon Area
            val dataPath = Path()
            for (i in 0 until count) {
                val metric = metrics.getOrNull(i) ?: RadarMetric("", 50f)
                val scoreRatio = (metric.score / 100f).coerceIn(0f, 1f)
                val angle = Math.toRadians((i * 360.0 / count) - 90.0)
                val x = center.x + (radius * scoreRatio * cos(angle)).toFloat()
                val y = center.y + (radius * scoreRatio * sin(angle)).toFloat()

                if (i == 0) dataPath.moveTo(x, y) else dataPath.lineTo(x, y)
            }
            dataPath.close()

            // Fill & stroke
            drawPath(path = dataPath, color = primaryColor.copy(alpha = 0.35f))
            drawPath(path = dataPath, color = primaryColor, style = Stroke(width = 3.dp.toPx()))

            // Points
            for (i in 0 until count) {
                val metric = metrics.getOrNull(i) ?: RadarMetric("", 50f)
                val scoreRatio = (metric.score / 100f).coerceIn(0f, 1f)
                val angle = Math.toRadians((i * 360.0 / count) - 90.0)
                val x = center.x + (radius * scoreRatio * cos(angle)).toFloat()
                val y = center.y + (radius * scoreRatio * sin(angle)).toFloat()

                drawCircle(color = secondaryColor, radius = 5.dp.toPx(), center = Offset(x, y))
            }
        }
    }
}
