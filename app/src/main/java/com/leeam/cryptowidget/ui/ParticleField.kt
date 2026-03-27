package com.leeam.cryptowidget.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.leeam.cryptowidget.ui.theme.LocalThemeColors
import java.util.Random

private data class ParticleConfig(
    val x: Float,
    val baseY: Float,
    val speed: Float,
    val radiusDp: Float,
    val isAccent: Boolean,
    val alpha: Float
)

/**
 * Full-screen particle field — 22 tiny dots drifting upward in a seamless loop.
 * Driven entirely by Compose's InfiniteTransition (no background work, no AlarmManager).
 * Colors update automatically with the active [LocalThemeColors].
 */
@Composable
fun ParticleField(modifier: Modifier = Modifier) {
    val accent    = LocalThemeColors.current.accent
    val secondary = LocalThemeColors.current.secondary

    val transition = rememberInfiniteTransition(label = "particles")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            tween(10_000, easing = LinearEasing),
            RepeatMode.Restart
        ),
        label = "particle_time"
    )

    // Stable geometry — seeded RNG so positions are deterministic across recompositions
    val configs = remember {
        val rng = Random(0xC01FC010L)
        List(22) {
            ParticleConfig(
                x         = rng.nextFloat(),
                baseY     = rng.nextFloat(),
                speed     = rng.nextFloat() * 0.55f + 0.15f,
                radiusDp  = rng.nextFloat() * 2.2f + 1.0f,
                isAccent  = rng.nextBoolean(),
                alpha     = rng.nextFloat() * 0.22f + 0.04f
            )
        }
    }

    Canvas(modifier) {
        configs.forEach { p ->
            // Wrap y so the particle re-enters at the bottom after leaving the top
            val rawY  = p.baseY - time * p.speed
            val normY = ((rawY % 1f) + 1f) % 1f
            val color = if (p.isAccent) accent else secondary

            drawCircle(
                color  = color.copy(alpha = p.alpha),
                radius = p.radiusDp.dp.toPx(),
                center = Offset(p.x * size.width, normY * size.height)
            )
        }
    }
}
