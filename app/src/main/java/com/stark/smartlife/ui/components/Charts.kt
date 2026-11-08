package com.stark.smartlife.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.stark.smartlife.R
import com.stark.smartlife.ui.theme.LocalHud

/** Bar chart of per-day totals. Draws only the values given; empty input shows "No data yet". */
@Composable
fun BarChart(values: List<Double>, labels: List<String>, color: Color = LocalHud.current.red) {
    if (values.isEmpty()) { Text(stringResource(R.string.no_data), color = MaterialTheme.colorScheme.onSurfaceVariant); return }
    val axis = LocalHud.current.line
    val max = (values.maxOrNull() ?: 1.0).coerceAtLeast(1.0)
    Column {
        Canvas(Modifier.fillMaxWidth().height(120.dp)) {
            val slot = size.width / values.size
            val bar = slot * 0.6f
            values.forEachIndexed { i, v ->
                val h = (v / max).toFloat() * size.height
                drawRoundRect(color, Offset(i * slot + (slot - bar) / 2, size.height - h), Size(bar, h), CornerRadius(4f))
            }
            drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height))
        }
        Column(Modifier.fillMaxWidth()) {
            Text(labels.firstOrNull().orEmpty() + "  →  " + labels.lastOrNull().orEmpty(),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Line chart of time-ordered values. */
@Composable
fun LineChart(values: List<Double>, color: Color = LocalHud.current.reactor) {
    if (values.size < 2) { Text(stringResource(R.string.no_data), color = MaterialTheme.colorScheme.onSurfaceVariant); return }
    val axis = LocalHud.current.line
    val lo = values.min(); val hi = values.max()
    val span = (hi - lo).takeIf { it > 0 } ?: 1.0
    Canvas(Modifier.fillMaxWidth().height(120.dp)) {
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * size.width / (values.size - 1)
            val y = size.height - ((v - lo) / span).toFloat() * size.height
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height))
        drawPath(path, color, style = Stroke(3.dp.toPx()))
    }
}
