package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TactileKnob(
    label: String,
    value: Float, // 0.0f to 1.0f
    onValueChange: (Float) -> Unit,
    color: Color = CyberCyan,
    modifier: Modifier = Modifier
) {
    var currentValue by remember(value) { mutableStateOf(value) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val delta = -dragAmount.y / 200f
                        currentValue = (currentValue + delta).coerceIn(0f, 1f)
                        onValueChange(currentValue)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.width / 2f - 6.dp.toPx()

                // Background track arc (from 135 deg to 405 deg)
                val startAngle = 135f
                val sweepAngle = 270f

                drawArc(
                    color = CardBorder,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )

                // Active value arc
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle * currentValue,
                    useCenter = false,
                    style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                )

                // Dial Indicator line
                val dialAngle = Math.toRadians((startAngle + sweepAngle * currentValue).toDouble())
                val indicatorStart = Offset(
                    center.x + (radius - 12.dp.toPx()) * cos(dialAngle).toFloat(),
                    center.y + (radius - 12.dp.toPx()) * sin(dialAngle).toFloat()
                )
                val indicatorEnd = Offset(
                    center.x + radius * cos(dialAngle).toFloat(),
                    center.y + radius * sin(dialAngle).toFloat()
                )

                drawLine(
                    color = TextPrimary,
                    start = indicatorStart,
                    end = indicatorEnd,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label.uppercase(),
            color = TextMuted,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${(currentValue * 100).toInt()}%",
            color = color,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
