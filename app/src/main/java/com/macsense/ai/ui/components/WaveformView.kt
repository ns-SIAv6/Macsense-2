package com.macsense.ai.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * Production waveform visualizer used in the vocal layer of VerticalDawScreen and
 * LyricsStudioScreen. Renders a bar-based amplitude envelope with an animated
 * playhead and optional word-highlight regions.
 *
 * @param amplitudes   Normalized amplitude values [0..1], one per time slice.
 * @param playheadFraction  Current playback position [0..1].
 * @param wordRegions  Optional highlight regions: list of Pair(startFraction, endFraction).
 * @param onSeek       Called with a [0..1] seek fraction when the user taps the waveform.
 * @param accentColor  Primary waveform tint.
 * @param height       Height of the drawable area.
 */
@Composable
fun WaveformView(
    amplitudes: List<Float>,
    playheadFraction: Float = 0f,
    wordRegions: List<Pair<Float, Float>> = emptyList(),
    onSeek: ((Float) -> Unit)? = null,
    accentColor: Color = Color(0xFF8B5CF6),
    height: Dp = 80.dp,
    modifier: Modifier = Modifier
) {
    val playheadAnim by animateFloatAsState(
        targetValue = playheadFraction,
        animationSpec = tween(80, easing = LinearEasing),
        label = "waveform-playhead"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0D0D1A))
            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .then(
                if (onSeek != null) Modifier.pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek(fraction)
                    }
                } else Modifier
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawWaveformBars(amplitudes, accentColor)
            drawWordHighlights(wordRegions, accentColor)
            drawPlayhead(playheadAnim, accentColor)
        }
    }
}

private fun DrawScope.drawWaveformBars(amplitudes: List<Float>, accent: Color) {
    if (amplitudes.isEmpty()) return
    val barCount = amplitudes.size
    val barWidth = size.width / barCount
    val centerY = size.height / 2f
    val maxBarH = size.height * 0.45f

    amplitudes.forEachIndexed { i, amp ->
        val barH = (amp.coerceIn(0f, 1f) * maxBarH).coerceAtLeast(2f)
        val x = i * barWidth + barWidth / 2f
        val alpha = 0.4f + amp * 0.6f
        drawLine(
            color = accent.copy(alpha = alpha),
            start = Offset(x, centerY - barH),
            end = Offset(x, centerY + barH),
            strokeWidth = (barWidth * 0.55f).coerceAtLeast(1f),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawWordHighlights(regions: List<Pair<Float, Float>>, accent: Color) {
    regions.forEach { (start, end) ->
        val x0 = start * size.width
        val x1 = end * size.width
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(accent.copy(alpha = 0f), accent.copy(alpha = 0.28f), accent.copy(alpha = 0f)),
                startX = x0,
                endX = x1
            ),
            topLeft = Offset(x0, 0f),
            size = androidx.compose.ui.geometry.Size(x1 - x0, size.height)
        )
    }
}

private fun DrawScope.drawPlayhead(fraction: Float, accent: Color) {
    val x = fraction * size.width
    drawLine(
        color = Color.White.copy(alpha = 0.9f),
        start = Offset(x, 0f),
        end = Offset(x, size.height),
        strokeWidth = 2f
    )
    // Playhead cap circle
    drawCircle(color = accent, radius = 5f, center = Offset(x, 0f))
}

/**
 * Animated recording waveform — uses a sine-wobble over microphone amplitude.
 * Used in FlowCaptureScreen's live monitor strip.
 */
@Composable
fun LiveWaveformStrip(
    isActive: Boolean,
    accentColor: Color = Color(0xFF00E5FF),
    height: Dp = 56.dp,
    barCount: Int = 48,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live-waveform")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave-phase"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0D0D1A))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val barW = size.width / barCount
            val centerY = size.height / 2f
            val maxH = size.height * 0.4f

            for (i in 0 until barCount) {
                val t = i.toFloat() / barCount
                val amp = if (isActive) {
                    abs(sin(phase + t * 6f * PI.toFloat())) * (0.4f + 0.6f * abs(sin(t * 3.7f + phase * 0.5f)))
                } else {
                    0.08f + abs(sin(t * PI.toFloat())) * 0.05f
                }
                val bH = (amp * maxH).coerceAtLeast(2f)
                val x = i * barW + barW / 2f
                drawLine(
                    color = accentColor.copy(alpha = if (isActive) 0.5f + amp * 0.5f else 0.2f),
                    start = Offset(x, centerY - bH),
                    end = Offset(x, centerY + bH),
                    strokeWidth = (barW * 0.55f).coerceAtLeast(1f),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

/**
 * Karaoke-style word-highlight bar. Displays a line of text with the
 * currently active word highlighted in [accentColor].
 *
 * @param words         All words in the current line.
 * @param activeIndex   Index of the word currently being sung (−1 = none).
 */
@Composable
fun WordHighlightBar(
    words: List<String>,
    activeIndex: Int = -1,
    accentColor: Color = Color(0xFF8B5CF6),
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        words.forEachIndexed { index, word ->
            val isActive = index == activeIndex
            Text(
                text = word,
                color = if (isActive) accentColor else Color(0xFF9CA3AF),
                fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Normal,
                fontSize = if (isActive) 17.sp else 14.sp,
                fontFamily = FontFamily.Default,
                modifier = Modifier.padding(horizontal = 3.dp)
            )
        }
    }
}
