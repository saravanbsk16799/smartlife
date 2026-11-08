package com.stark.smartlife.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.stark.smartlife.ui.theme.LocalHud
import kotlin.math.cos
import kotlin.math.sin

/** Full-screen metallic backdrop with a faint HUD grid. */
@Composable
fun StarkBackground(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val hud = LocalHud.current
    Box(modifier.fillMaxSize().background(hud.background)) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 48.dp.toPx()
            var x = 0f
            while (x < size.width) { drawLine(hud.reactor.copy(alpha = 0.04f), Offset(x, 0f), Offset(x, size.height)); x += step }
            var y = 0f
            while (y < size.height) { drawLine(hud.reactor.copy(alpha = 0.04f), Offset(0f, y), Offset(size.width, y)); y += step }
        }
        content()
    }
}

/** Panel with cut corners and a thin glowing edge. */
@Composable
fun HudCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val hud = LocalHud.current
    val shape = CutCornerShape(topStart = 14.dp, bottomEnd = 14.dp)
    Column(
        modifier
            .fillMaxWidth()
            .background(hud.panel, shape)
            .border(1.dp, hud.line, shape)
            .padding(16.dp),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        modifier = modifier.padding(top = 8.dp, bottom = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = LocalHud.current.gold,
    )
}

@Composable
fun MetricTile(label: String, value: String, unit: String = "", modifier: Modifier = Modifier) {
    HudCard(modifier) {
        Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        if (unit.isNotEmpty()) Text(unit, style = MaterialTheme.typography.labelSmall, color = LocalHud.current.reactor)
    }
}

/**
 * Animated arc reactor. [progress] (0..1) sweeps the outer ring, so it shows real progress
 * (for example steps toward the goal) rather than decoration only.
 */
@Composable
fun ArcReactor(progress: Float, centerText: String, caption: String, size: Dp = 220.dp, modifier: Modifier = Modifier) {
    val hud = LocalHud.current
    val t = rememberInfiniteTransition(label = "reactor")
    val spin by t.animateFloat(0f, 360f, infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "spin")
    val pulse by t.animateFloat(0.55f, 1f, infiniteRepeatable(tween(2200), RepeatMode.Reverse), label = "pulse")
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val r = this.size.minDimension / 2
            // glow
            drawCircle(
                Brush.radialGradient(listOf(hud.reactor.copy(alpha = 0.45f * pulse), Color.Transparent), c, r),
                radius = r,
            )
            // ticks
            rotate(spin, c) {
                for (i in 0 until 36) {
                    val a = Math.toRadians(i * 10.0)
                    val inner = r * 0.88f
                    val outer = r * (if (i % 3 == 0) 1f else 0.95f)
                    drawLine(
                        hud.gold.copy(alpha = 0.8f),
                        Offset(c.x + (inner * cos(a)).toFloat(), c.y + (inner * sin(a)).toFloat()),
                        Offset(c.x + (outer * cos(a)).toFloat(), c.y + (outer * sin(a)).toFloat()),
                        strokeWidth = 2f,
                    )
                }
            }
            // progress ring
            val ring = r * 0.74f
            val stroke = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
            drawArc(hud.line.copy(alpha = 0.35f), -90f, 360f, false, topLeft = Offset(c.x - ring, c.y - ring),
                size = androidx.compose.ui.geometry.Size(ring * 2, ring * 2), style = stroke)
            drawArc(
                Brush.sweepGradient(listOf(hud.red, hud.gold, hud.reactor), c),
                -90f, 360f * progress.coerceIn(0f, 1f), false,
                topLeft = Offset(c.x - ring, c.y - ring),
                size = androidx.compose.ui.geometry.Size(ring * 2, ring * 2), style = stroke,
            )
            // core triangle
            val core = r * 0.42f
            val p = Path().apply {
                for (i in 0 until 3) {
                    val a = Math.toRadians(-90.0 + i * 120.0)
                    val x = c.x + (core * cos(a)).toFloat(); val y = c.y + (core * sin(a)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(p, hud.reactor.copy(alpha = 0.25f * pulse + 0.1f))
            drawPath(p, hud.reactor, style = Stroke(2.dp.toPx()))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerText, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
            Text(caption.uppercase(), style = MaterialTheme.typography.labelSmall, color = hud.reactor)
        }
    }
}
