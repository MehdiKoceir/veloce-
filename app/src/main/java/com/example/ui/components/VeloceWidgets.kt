package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CalorieCalculator.TrackPoint
import com.example.ui.theme.*

/**
 * A beautiful, real-time custom Vector Track visualizer.
 * Projects latitude/longitude list onto a local 2D Canvas coordinate space.
 * Draws a glowing, athletic neon orange line with starting green dot and pulsating orange head.
 */
@Composable
fun RouteTrackVisualizer(
    points: List<TrackPoint>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulsate")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 4.0f,
        targetValue = 12.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_radius"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .background(VeloceDarkSurface, shape = RoundedCornerShape(16.dp))
            .border(1.dp, VeloceSecondaryContainer, shape = RoundedCornerShape(16.dp))
            .testTag("route_visualizer")
    ) {
        if (points.size < 2) {
            // Placeholder empty state visualizer
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📍 En attente du signal GPS...",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Démarrer l'activité pour tracer le parcours",
                    color = VeloceOnSurfaceMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val width = size.width
                val height = size.height

                // 1. Calculate boundaries of coordinates
                val latitudes = points.map { it.latitude }
                val longitudes = points.map { it.longitude }

                val minLat = latitudes.minOrNull() ?: 0.0
                val maxLat = latitudes.maxOrNull() ?: 0.0
                val minLng = longitudes.minOrNull() ?: 0.0
                val maxLng = longitudes.maxOrNull() ?: 0.0

                val latSpan = maxLat - minLat
                val lngSpan = maxLng - minLng

                // Avoid division by zero
                val safeLatSpan = if (latSpan == 0.0) 0.0001 else latSpan
                val safeLngSpan = if (lngSpan == 0.0) 0.0001 else lngSpan

                // Padding margins inside Canvas coordinates
                val padding = 10f

                // Project coordinates into canvas bounds
                fun project(pt: TrackPoint): Offset {
                    // Map longitudes to X (inverted horizontally depending on span direction)
                    val x = padding + ((pt.longitude - minLng) / safeLngSpan) * (width - 2 * padding)
                    // Map latitudes to Y (inverted: high latitude is up, so Y is 0.0)
                    val y = padding + (1.0 - (pt.latitude - minLat) / safeLatSpan) * (height - 2 * padding)
                    return Offset(x.toFloat(), y.toFloat())
                }

                // 2. Build track path
                val path = Path()
                val startOffset = project(points.first())
                path.moveTo(startOffset.x, startOffset.y)

                for (i in 1 until points.size) {
                    val offset = project(points[i])
                    path.lineTo(offset.x, offset.y)
                }

                // 3. Draw Track glowing shadow
                drawPath(
                    path = path,
                    color = VelocePrimary.copy(alpha = 0.25f),
                    style = Stroke(width = 12f)
                )

                // 4. Draw Core Track Line
                drawPath(
                    path = path,
                    color = VelocePrimary,
                    style = Stroke(width = 5f)
                )

                // 5. Draw Start Location (Volt/Green circle)
                drawCircle(
                    color = VeloceTertiary,
                    radius = 6f,
                    center = startOffset
                )

                // 6. Draw Current Location (Neon pulsating coral head)
                val currentOffset = project(points.last())
                // Outer Pulse
                drawCircle(
                    color = VelocePrimary.copy(alpha = 0.4f),
                    radius = pulseRadius,
                    center = currentOffset
                )
                // Inner solid core
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = currentOffset
                )
            }
        }
        if (points.size >= 2) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp)
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f), shape = RoundedCornerShape(20.dp))
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), shape = RoundedCornerShape(20.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(VeloceTertiary, shape = CircleShape)
                )
                Text(
                    text = "GPS SIGNAL FORT",
                    color = VeloceTertiary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * A beautiful, highly aesthetic canvas-based bar chart.
 * Draws a weekly athletic performance dashboard without external bloated libraries.
 */
@Composable
fun AthleticBarChart(
    data: List<Float>,
    labels: List<String>,
    metricLabel: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(VeloceDarkSurface, shape = RoundedCornerShape(16.dp))
            .border(1.dp, VeloceSecondaryContainer, shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = metricLabel,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val barCount = data.size
                if (barCount == 0) return@Canvas

                val maxVal = data.maxOrNull() ?: 1.0f
                val safeMax = if (maxVal == 0.0f) 1.0f else maxVal

                val spacing = 20f
                val totalSpacing = spacing * (barCount - 1)
                val barWidth = (width - totalSpacing) / barCount

                for (i in 0 until barCount) {
                    val x = i * (barWidth + spacing)
                    val barHeightFraction = data[i] / safeMax
                    val barHeight = height * barHeightFraction * 0.85f // Leave room for top text

                    // Draw nice dynamic bar with gradient brush
                    val barTop = height - barHeight
                    val brush = Brush.verticalGradient(
                        colors = listOf(VelocePrimary, VeloceSecondary)
                    )

                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(x, barTop),
                        size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f, 6f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach { label ->
                Text(
                    text = label,
                    color = VeloceOnSurfaceMuted,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
