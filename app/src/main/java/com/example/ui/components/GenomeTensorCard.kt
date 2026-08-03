package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SoundGenome
import com.example.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun GenomeTensorCard(
    genome: SoundGenome,
    isSelected: Boolean = false,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) CyberCyan else CardBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .testTag("genome_card_${genome.id}"),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = genome.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Gen ${genome.generation} • ${genome.soundType.label}",
                        color = genome.soundType.color,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                if (genome.scarMagnitude > 0f) {
                    Surface(
                        color = CrimsonRed.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed)
                    ) {
                        Text(
                            text = "SCAR ${(genome.scarMagnitude * 100).toInt()}%",
                            color = CrimsonRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 4D Tensor Radar Graph
                Box(
                    modifier = Modifier.size(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TensorRadarCanvas(
                        mass = genome.mass,
                        radiance = genome.radiance,
                        entropy = genome.entropy,
                        curvature = genome.curvature,
                        accentColor = genome.soundType.color
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Tensor Numeric Values
                Column(modifier = Modifier.weight(1f)) {
                    TensorValueBar("MASS (M)", genome.mass, CyberCyan)
                    TensorValueBar("RADIANCE (R)", genome.radiance, GoldenAmber)
                    TensorValueBar("ENTROPY (E)", genome.entropy, CrimsonRed)
                    TensorValueBar("CURVATURE (C)", genome.curvature, CyberPurple)
                }
            }
        }
    }
}

@Composable
fun TensorRadarCanvas(
    mass: Float,
    radiance: Float,
    entropy: Float,
    curvature: Float,
    accentColor: Color
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.width / 2f - 8.dp.toPx()

        // Draw radar concentric grid
        for (r in listOf(0.33f, 0.66f, 1.0f)) {
            drawCircle(
                color = CardBorder,
                radius = maxRadius * r,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
        }

        // 4 Axes: 0 (M), PI/2 (R), PI (E), 3PI/2 (C)
        val values = listOf(mass, radiance, entropy, curvature)
        val points = mutableListOf<Offset>()

        for (i in 0..3) {
            val angle = i * (PI / 2f) - (PI / 2f)
            val v = values[i].coerceIn(0.1f, 1.0f)
            val x = center.x + (maxRadius * v * cos(angle)).toFloat()
            val y = center.y + (maxRadius * v * sin(angle)).toFloat()
            points.add(Offset(x, y))

            val axisX = center.x + (maxRadius * cos(angle)).toFloat()
            val axisY = center.y + (maxRadius * sin(angle)).toFloat()
            drawLine(
                color = CardBorder,
                start = center,
                end = Offset(axisX, axisY),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Draw 4D Polygon
        val path = Path().apply {
            moveTo(points[0].x, points[0].y)
            lineTo(points[1].x, points[1].y)
            lineTo(points[2].x, points[2].y)
            lineTo(points[3].x, points[3].y)
            close()
        }

        drawPath(
            path = path,
            color = accentColor.copy(alpha = 0.35f)
        )
        drawPath(
            path = path,
            color = accentColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun TensorValueBar(label: String, value: Float, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(70.dp)
        )
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(4.dp),
            color = color,
            trackColor = DarkSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${(value * 100).toInt()}%",
            color = TextPrimary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
