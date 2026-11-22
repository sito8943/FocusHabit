package dev.sito8943.focushabit.ui.patterns

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sito8943.focushabit.ui.theme.Bebas
import dev.sito8943.focushabit.ui.theme.Brand
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.delay

private data class Piece(val angle: Float, val speed: Float, val spin: Float, val size: Float, val colorIndex: Int)

private val confettiColors = listOf(Brand.Red, Brand.Navy, Brand.Green, Brand.Gold)

/**
 * "Good job!" card with a confetti burst in the brand colours. Shows for
 * ~2.2 s and then calls [onFinished]. Does not block touches.
 */
@Composable
fun Celebration(
    visible: Boolean,
    title: String,
    message: String,
    onFinished: () -> Unit,
) {
    val progress = remember { Animatable(0f) }
    val pieces = remember {
        List(70) {
            Piece(
                angle = Random.nextFloat() * 360f,
                speed = 0.35f + Random.nextFloat() * 0.65f,
                spin = Random.nextFloat() * 720f - 360f,
                size = 6f + Random.nextFloat() * 8f,
                colorIndex = Random.nextInt(confettiColors.size),
            )
        }
    }
    LaunchedEffect(visible) {
        if (!visible) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(1_600, easing = LinearEasing))
        delay(600)
        onFinished()
    }

    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val t = progress.value
                val origin = Offset(size.width / 2, size.height * 0.42f)
                val reach = size.minDimension * 0.75f
                pieces.forEach { p ->
                    val radians = Math.toRadians(p.angle.toDouble())
                    val distance = reach * p.speed * (1 - (1 - t) * (1 - t)) // ease-out
                    val gravity = size.height * 0.35f * t * t
                    val center = Offset(
                        origin.x + (cos(radians) * distance).toFloat(),
                        origin.y + (sin(radians) * distance).toFloat() + gravity,
                    )
                    val px = p.size.dp.toPx()
                    rotate(p.spin * t, center) {
                        drawRect(
                            color = confettiColors[p.colorIndex].copy(alpha = 1f - t * 0.8f),
                            topLeft = Offset(center.x - px / 2, center.y - px / 4),
                            size = Size(px, px / 2),
                        )
                    }
                }
            }
            AnimatedVisibility(visible, enter = scaleIn(initialScale = 0.7f) + fadeIn()) {
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                ) {
                    Column(
                        Modifier.padding(horizontal = 32.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(title, fontFamily = Bebas, fontSize = 34.sp, color = MaterialTheme.colorScheme.tertiary)
                        Text(
                            message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
