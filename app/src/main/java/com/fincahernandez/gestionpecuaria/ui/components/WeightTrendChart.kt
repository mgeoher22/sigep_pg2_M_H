package com.fincahernandez.gestionpecuaria.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

/** Punto real de una serie de peso; la etiqueta suele ser una fecha corta. */
data class WeightChartPoint(val label: String, val valuePounds: Double)

/**
 * Gráfica reutilizable que escala los valores reales, dibuja una línea de
 * tendencia y muestra fechas y métricas para que el trazo tenga contexto.
 */
@Composable
fun WeightTrendChart(
    title: String,
    subtitle: String,
    points: List<WeightChartPoint>,
    modifier: Modifier = Modifier,
    emptyMessage: String = "Registre al menos un pesaje para generar la gráfica."
) {
    val visiblePoints = points.takeLast(12)
    val primary = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val pointCenterColor = MaterialTheme.colorScheme.surface

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall)

            if (visiblePoints.isEmpty()) {
                Text(emptyMessage, style = MaterialTheme.typography.bodyMedium)
                return@Column
            }

            val minimum = visiblePoints.minOf { it.valuePounds }
            val maximum = visiblePoints.maxOf { it.valuePounds }
            val rawRange = maximum - minimum
            val padding = if (rawRange == 0.0) maximum.coerceAtLeast(1.0) * 0.08 else rawRange * 0.18
            val chartMinimum = minimum - padding
            val chartMaximum = maximum + padding
            val chartRange = (chartMaximum - chartMinimum).coerceAtLeast(1.0)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                val horizontalInset = 12.dp.toPx()
                val verticalInset = 12.dp.toPx()
                val chartWidth = size.width - horizontalInset * 2
                val chartHeight = size.height - verticalInset * 2

                repeat(5) { index ->
                    val y = verticalInset + chartHeight * index / 4f
                    drawLine(
                        color = gridColor,
                        start = Offset(horizontalInset, y),
                        end = Offset(size.width - horizontalInset, y),
                        strokeWidth = 1.dp.toPx()
                    )
                }

                val offsets = visiblePoints.mapIndexed { index, point ->
                    val x = if (visiblePoints.size == 1) {
                        size.width / 2f
                    } else {
                        horizontalInset + chartWidth * index / visiblePoints.lastIndex
                    }
                    val normalized = ((point.valuePounds - chartMinimum) / chartRange).toFloat()
                    val y = verticalInset + chartHeight * (1f - normalized)
                    Offset(x, y)
                }

                val areaPath = Path().apply {
                    moveTo(offsets.first().x, size.height - verticalInset)
                    offsets.forEachIndexed { index, point ->
                        if (index == 0) lineTo(point.x, point.y) else lineTo(point.x, point.y)
                    }
                    lineTo(offsets.last().x, size.height - verticalInset)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primary.copy(alpha = 0.32f), surfaceColor.copy(alpha = 0.05f))
                    )
                )

                val linePath = Path().apply {
                    moveTo(offsets.first().x, offsets.first().y)
                    offsets.drop(1).forEach { point -> lineTo(point.x, point.y) }
                }
                drawPath(linePath, color = primary, style = Stroke(width = 3.dp.toPx()))
                offsets.forEach { point ->
                    drawCircle(color = primary, radius = 5.dp.toPx(), center = point)
                    drawCircle(color = pointCenterColor, radius = 2.dp.toPx(), center = point)
                }
            }

            val labelIndexes = listOf(0, visiblePoints.lastIndex / 2, visiblePoints.lastIndex).distinct()
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labelIndexes.forEach { index ->
                    Text(visiblePoints[index].label, style = MaterialTheme.typography.labelSmall)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Mín. ${oneDecimal(minimum)} lb", style = MaterialTheme.typography.bodySmall)
                Text("Máx. ${oneDecimal(maximum)} lb", style = MaterialTheme.typography.bodySmall)
                Text(
                    "Cambio ${signedOneDecimal(visiblePoints.last().valuePounds - visiblePoints.first().valuePounds)} lb",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

private fun oneDecimal(value: Double): String = String.format(Locale.US, "%.1f", value)

private fun signedOneDecimal(value: Double): String =
    String.format(Locale.US, if (value > 0) "+%.1f" else "%.1f", value)
