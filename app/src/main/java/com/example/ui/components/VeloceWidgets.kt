package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CalorieCalculator
import com.example.data.CalorieCalculator.TrackPoint
import com.example.data.database.SportActivity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.roundToInt

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

@Composable
fun RealTimeActivityTimerComponent(
    durationSec: Long,
    distanceMeters: Double,
    calories: Double,
    speedKmH: Double,
    activityType: CalorieCalculator.ActivityType,
    weightKg: Double,
    isMetric: Boolean,
    isPaused: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "timer_ring")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )

    var showMetExpl by remember { mutableStateOf(false) }

    val currentMet = CalorieCalculator.getDynamicMET(activityType, speedKmH)

    val displayDistance = if (isMetric) distanceMeters / 1000.0 else (distanceMeters / 1000.0) * 0.621371
    val distanceUnit = if (isMetric) "km" else "mi"

    fun formatDurationLocal(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format("%02d:%02d:%02d", h, m, s)
        } else {
            String.format("%02d:%02d", m, s)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Interactive Ring Timer Container
        Box(
            modifier = Modifier
                .size(260.dp)
                .padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 10.dp.toPx()
                val sizePx = size.width
                val radius = (sizePx - strokeWidth) / 2f
                val centerOffset = Offset(sizePx / 2f, sizePx / 2f)

                // Background Ring
                drawCircle(
                    color = VeloceSecondaryContainer.copy(alpha = 0.4f),
                    radius = radius,
                    center = centerOffset,
                    style = Stroke(width = strokeWidth)
                )

                // Sweeping Active Trace
                val startAngle = if (!isPaused) rotation else -90f
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            VelocePrimary,
                            VeloceSecondary,
                            VeloceAccentCoral,
                            VelocePrimary.copy(alpha = 0.05f),
                            VelocePrimary
                        )
                    ),
                    startAngle = startAngle,
                    sweepAngle = 300f,
                    useCenter = false,
                    topLeft = Offset(strokeWidth / 2f, strokeWidth / 2f),
                    size = androidx.compose.ui.geometry.Size(sizePx - strokeWidth, sizePx - strokeWidth),
                    style = Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )

                // Outer Glowing Neon Orbit Dots
                val numDots = 12
                for (i in 0 until numDots) {
                    val angleRad = (i * (360f / numDots) + (if (!isPaused) rotation * 0.2f else 0f)) * Math.PI / 180.0
                    val dotRadius = if (i % 3 == 0) 5f else 3f
                    val dotCenter = Offset(
                        (centerOffset.x + (radius + 16.dp.toPx()) * cos(angleRad)).toFloat(),
                        (centerOffset.y + (radius + 16.dp.toPx()) * sin(angleRad)).toFloat()
                    )
                    drawCircle(
                        color = VelocePrimary.copy(alpha = if (!isPaused) glowAlpha else 0.4f),
                        radius = dotRadius,
                        center = dotCenter
                    )
                }
            }

            // Central HUD Text Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Active State Indicator Dot
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val dotColor = if (isPaused) VeloceAccentCoral else VeloceTertiary
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(dotColor, shape = CircleShape)
                    )
                    Text(
                        text = if (isPaused) "PAUSE" else "SUIVI EN COURS",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isPaused) VeloceAccentCoral else VeloceTertiary,
                        letterSpacing = 1.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Giant Monospace-feeling Duration Timer
                Text(
                    text = formatDurationLocal(durationSec),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 50.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.testTag("realtime_timer_duration")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Small Milliseconds/Sub-seconds tick animation just to make it incredibly alive
                val msTicks = if (isPaused) "00" else String.format("%02d", (System.currentTimeMillis() % 1000) / 10)
                Text(
                    text = ":$msTicks SEC",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = VeloceOnSurfaceMuted,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Real-Time Flanking Metrics Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Distance Flank Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = VeloceDarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsRun,
                            contentDescription = "Distance",
                            tint = VelocePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "DISTANCE",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = VeloceOnSurfaceMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = String.format("%.2f", displayDistance),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Text(
                            text = distanceUnit,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = VeloceOnSurfaceMuted,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }

            // Calorie (MET-calculated) Flank Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = VeloceDarkSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = "Calories",
                            tint = VeloceAccentCoral,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "CALORIES MET",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = VeloceOnSurfaceMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = calories.roundToInt().toString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = VeloceAccentCoral
                        )
                        Text(
                            text = "kcal",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = VeloceOnSurfaceMuted,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Educational MET Formula Explorer Panel (M3 styled, expandable)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clickable { showMetExpl = !showMetExpl },
            colors = CardDefaults.cardColors(
                containerColor = VeloceDarkSurfaceCard
            ),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                VeloceSecondaryContainer.copy(alpha = 0.6f)
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "MET Info",
                            tint = VelocePrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "Formule Métabolique (MET) Active",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Intensité instantanée : ${String.format("%.1f", currentMet)} MET",
                                style = MaterialTheme.typography.bodySmall,
                                color = VelocePrimary
                            )
                        }
                    }
                    Icon(
                        imageVector = if (showMetExpl) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expand",
                        tint = VeloceOnSurfaceMuted
                    )
                }

                AnimatedVisibility(visible = showMetExpl) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Divider(color = VeloceSecondaryContainer.copy(alpha = 0.4f))

                        Text(
                            text = "Les équivalents métaboliques (MET) mesurent l'effort physique par rapport au repos (1 MET). Le calcul précis en temps réel utilise la formule :",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloceOnSurfaceMuted
                        )

                        // Highlighted MET Formula box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(VeloceDarkSurface, shape = RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Kcal = MET × Poids (kg) × Temps (heures)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = VeloceTertiary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Votre calcul actuel :",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = VeloceOnSurfaceMuted
                                )
                                Text(
                                    text = "${String.format("%.1f", currentMet)} MET × ${weightKg} kg × ${String.format("%.4f", durationSec / 3600.0)} h = ${calories.roundToInt()} kcal",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }

                        // MET guidelines based on speeds
                        Text(
                            text = "Échelles de MET pour le type ${activityType.name} :",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Au repos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VeloceOnSurfaceMuted
                                )
                                Text(
                                    text = "1.0 MET",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Séance modérée",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VeloceOnSurfaceMuted
                                )
                                Text(
                                    text = when (activityType) {
                                        CalorieCalculator.ActivityType.WALKING -> "2.8 - 3.5 MET"
                                        CalorieCalculator.ActivityType.RUNNING -> "8.3 - 9.8 MET"
                                        CalorieCalculator.ActivityType.CYCLING -> "4.0 - 6.0 MET"
                                        CalorieCalculator.ActivityType.HIKING -> "5.3 - 6.5 MET"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Effort intense",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = VeloceOnSurfaceMuted
                                )
                                Text(
                                    text = when (activityType) {
                                        CalorieCalculator.ActivityType.WALKING -> "> 4.3 MET"
                                        CalorieCalculator.ActivityType.RUNNING -> "> 11.8 MET"
                                        CalorieCalculator.ActivityType.CYCLING -> "> 8.0 MET"
                                        CalorieCalculator.ActivityType.HIKING -> "> 7.5 MET"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * A highly immersive, interactive, dual-line 'Recharts'-style performance graph.
 * Visualizes the progress of workout duration and calories burned over chronological time.
 * Supports interactive dragging/tapping to display a real-time HUD tooltip.
 */
@Composable
fun RechartsLineGraph(
    activities: List<SportActivity>,
    modifier: Modifier = Modifier
) {
    // Sort activities chronologically (oldest to newest) to show progress over time
    val sortedActivities = remember(activities) {
        activities.sortedBy { it.startTime }.takeLast(10)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = VeloceDarkSurface
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            VeloceSecondaryContainer.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and quick summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Progression Temporelle",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Text(
                        text = "Évolution de l'effort et des calories",
                        style = MaterialTheme.typography.bodySmall,
                        color = VeloceOnSurfaceMuted
                    )
                }
                
                Icon(
                    imageVector = Icons.Default.Timeline,
                    contentDescription = "Graph icon",
                    tint = VelocePrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (sortedActivities.size < 2) {
                // Empty state for the graph
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(VeloceDarkSurfaceCard.copy(alpha = 0.5f), shape = RoundedCornerShape(12.dp))
                        .border(1.dp, VeloceSecondaryContainer.copy(alpha = 0.3f), shape = RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = "Trending",
                            tint = VeloceOnSurfaceMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Enregistrez au moins 2 séances",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Ajoutez des activités pour visualiser vos graphiques de progression temporelle interactive.",
                            style = MaterialTheme.typography.bodySmall,
                            color = VeloceOnSurfaceMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                // Interactive visibility states for the series (mimicking Recharts legend triggers)
                var showDuration by remember { mutableStateOf(true) }
                var showCalories by remember { mutableStateOf(true) }

                // Interactive touch selection index
                var selectedIndex by remember { mutableStateOf(-1) }
                var touchX by remember { mutableStateOf(0f) }

                // Extract data points
                val durationsMin = remember(sortedActivities) {
                    sortedActivities.map { (it.durationMs / 60000.0f) }
                }
                val caloriesBurned = remember(sortedActivities) {
                    sortedActivities.map { it.calories.toFloat() }
                }
                val dateLabels = remember(sortedActivities) {
                    val sdf = SimpleDateFormat("dd/MM", Locale.FRENCH)
                    sortedActivities.map { sdf.format(Date(it.startTime)) }
                }

                val maxDuration = remember(durationsMin) { (durationsMin.maxOrNull() ?: 1.0f).coerceAtLeast(1.0f) }
                val maxCalories = remember(caloriesBurned) { (caloriesBurned.maxOrNull() ?: 1.0f).coerceAtLeast(1.0f) }

                // The Chart Canvas Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(sortedActivities) {
                                detectTapGestures(
                                    onPress = { offset ->
                                        // Find nearest point
                                        val leftMargin = 60f
                                        val rightMargin = 40f
                                        val drawableWidth = size.width - leftMargin - rightMargin
                                        val stepX = drawableWidth / (sortedActivities.size - 1)
                                        
                                        val relativeX = offset.x - leftMargin
                                        val index = (relativeX / stepX).roundToInt().coerceIn(0, sortedActivities.size - 1)
                                        selectedIndex = index
                                        touchX = leftMargin + index * stepX
                                    }
                                )
                            }
                            .pointerInput(sortedActivities) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val leftMargin = 60f
                                        val rightMargin = 40f
                                        val drawableWidth = size.width - leftMargin - rightMargin
                                        val stepX = drawableWidth / (sortedActivities.size - 1)
                                        val relativeX = offset.x - leftMargin
                                        val index = (relativeX / stepX).roundToInt().coerceIn(0, sortedActivities.size - 1)
                                        selectedIndex = index
                                        touchX = leftMargin + index * stepX
                                    },
                                    onDrag = { change, _ ->
                                        val leftMargin = 60f
                                        val rightMargin = 40f
                                        val drawableWidth = size.width - leftMargin - rightMargin
                                        val stepX = drawableWidth / (sortedActivities.size - 1)
                                        val relativeX = change.position.x - leftMargin
                                        val index = (relativeX / stepX).roundToInt().coerceIn(0, sortedActivities.size - 1)
                                        selectedIndex = index
                                        touchX = leftMargin + index * stepX
                                    },
                                    onDragEnd = {
                                        // Optionally reset, but keeping the tooltip pinned on last touch is nicer
                                    }
                                )
                            }
                    ) {
                        val width = size.width
                        val height = size.height

                        val leftMargin = 60f
                        val rightMargin = 40f
                        val topMargin = 30f
                        val bottomMargin = 40f

                        val drawableWidth = width - leftMargin - rightMargin
                        val drawableHeight = height - topMargin - bottomMargin

                        val stepX = drawableWidth / (sortedActivities.size - 1)

                        // 1. Draw dashed gridlines (Y-axis grid)
                        val numGridlines = 4
                        for (g in 0..numGridlines) {
                            val ratio = g.toFloat() / numGridlines
                            val y = topMargin + drawableHeight * ratio
                            
                            // Horizontal gridline
                            drawLine(
                                color = VeloceSecondaryContainer.copy(alpha = 0.25f),
                                start = Offset(leftMargin, y),
                                end = Offset(width - rightMargin, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                            )
                        }

                        // 2. Draw lines and area gradients
                        if (showDuration) {
                            val durationPath = Path()
                            val gradientPath = Path()

                            // Initialize path
                            val x0 = leftMargin
                            val y0 = height - bottomMargin - (durationsMin[0] / maxDuration) * drawableHeight
                            durationPath.moveTo(x0, y0)
                            gradientPath.moveTo(x0, height - bottomMargin)
                            gradientPath.lineTo(x0, y0)

                            for (i in 1 until sortedActivities.size) {
                                val x = leftMargin + i * stepX
                                val y = height - bottomMargin - (durationsMin[i] / maxDuration) * drawableHeight
                                durationPath.lineTo(x, y)
                                gradientPath.lineTo(x, y)
                            }
                            gradientPath.lineTo(leftMargin + (sortedActivities.size - 1) * stepX, height - bottomMargin)
                            gradientPath.close()

                            // Draw subtle Area gradient fill (Recharts design)
                            drawPath(
                                path = gradientPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        VelocePrimary.copy(alpha = 0.25f),
                                        VelocePrimary.copy(alpha = 0.0f)
                                    )
                                )
                            )

                            // Draw Line
                            drawPath(
                                path = durationPath,
                                color = VelocePrimary,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        if (showCalories) {
                            val caloriesPath = Path()
                            val gradientPath = Path()

                            // Initialize path
                            val x0 = leftMargin
                            val y0 = height - bottomMargin - (caloriesBurned[0] / maxCalories) * drawableHeight
                            caloriesPath.moveTo(x0, y0)
                            gradientPath.moveTo(x0, height - bottomMargin)
                            gradientPath.lineTo(x0, y0)

                            for (i in 1 until sortedActivities.size) {
                                val x = leftMargin + i * stepX
                                val y = height - bottomMargin - (caloriesBurned[i] / maxCalories) * drawableHeight
                                caloriesPath.lineTo(x, y)
                                gradientPath.lineTo(x, y)
                            }
                            gradientPath.lineTo(leftMargin + (sortedActivities.size - 1) * stepX, height - bottomMargin)
                            gradientPath.close()

                            // Draw subtle Area gradient fill (Recharts design)
                            drawPath(
                                path = gradientPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        VeloceAccentCoral.copy(alpha = 0.25f),
                                        VeloceAccentCoral.copy(alpha = 0.0f)
                                    )
                                )
                            )

                            // Draw Line
                            drawPath(
                                path = caloriesPath,
                                color = VeloceAccentCoral,
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }

                        // 3. Draw Axis Labels & ticks
                        // Draw X Axis Line
                        drawLine(
                            color = VeloceSecondaryContainer.copy(alpha = 0.6f),
                            start = Offset(leftMargin, height - bottomMargin),
                            end = Offset(width - rightMargin, height - bottomMargin),
                            strokeWidth = 1.dp.toPx()
                        )

                        // 4. Draw Interactive Touch Tracker line & nodes
                        if (selectedIndex in sortedActivities.indices) {
                            // Vertical tracking guideline
                            drawLine(
                                color = Color.White.copy(alpha = 0.4f),
                                start = Offset(touchX, topMargin),
                                end = Offset(touchX, height - bottomMargin),
                                strokeWidth = 1.5.dp.toPx()
                            )

                            // Intersect node for Duration
                            if (showDuration) {
                                val durationY = height - bottomMargin - (durationsMin[selectedIndex] / maxDuration) * drawableHeight
                                // Outer pulse
                                drawCircle(
                                    color = VelocePrimary.copy(alpha = 0.3f),
                                    radius = 10.dp.toPx(),
                                    center = Offset(touchX, durationY)
                                )
                                // Inner solid dot
                                drawCircle(
                                    color = Color.White,
                                    radius = 5.dp.toPx(),
                                    center = Offset(touchX, durationY)
                                )
                                drawCircle(
                                    color = VelocePrimary,
                                    radius = 3.5.dp.toPx(),
                                    center = Offset(touchX, durationY)
                                )
                            }

                            // Intersect node for Calories
                            if (showCalories) {
                                val caloriesY = height - bottomMargin - (caloriesBurned[selectedIndex] / maxCalories) * drawableHeight
                                // Outer pulse
                                drawCircle(
                                    color = VeloceAccentCoral.copy(alpha = 0.3f),
                                    radius = 10.dp.toPx(),
                                    center = Offset(touchX, caloriesY)
                                )
                                // Inner solid dot
                                drawCircle(
                                    color = Color.White,
                                    radius = 5.dp.toPx(),
                                    center = Offset(touchX, caloriesY)
                                )
                                drawCircle(
                                    color = VeloceAccentCoral,
                                    radius = 3.5.dp.toPx(),
                                    center = Offset(touchX, caloriesY)
                                )
                            }
                        }
                    }
                }

                // X-Axis Labels row underneath the canvas
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    dateLabels.forEachIndexed { idx, label ->
                        val isSelected = idx == selectedIndex
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else VeloceOnSurfaceMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.clickable { selectedIndex = idx }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Interactive Info Tooltip Panel
                AnimatedVisibility(visible = selectedIndex in sortedActivities.indices) {
                    if (selectedIndex in sortedActivities.indices) {
                        val selectedActivity = sortedActivities[selectedIndex]
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = VeloceDarkSurfaceCard
                            ),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                VeloceSecondaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.2f)) {
                                    val dateStr = remember(selectedActivity.startTime) {
                                        val sdf = SimpleDateFormat("dd MMMM 'à' HH:mm", Locale.FRENCH)
                                        sdf.format(Date(selectedActivity.startTime))
                                    }
                                    Text(
                                        text = dateStr.uppercase(),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = VelocePrimary,
                                        fontSize = 10.sp
                                    )
                                    Text(
                                        text = selectedActivity.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.weight(1.8f),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (showDuration) {
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                text = "DURÉE",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 9.sp,
                                                color = VeloceOnSurfaceMuted,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = String.format("%.1f min", durationsMin[selectedIndex]),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Black,
                                                color = Color.White
                                            )
                                        }
                                    }
                                    
                                    if (showCalories) {
                                        Column(
                                            horizontalAlignment = Alignment.End,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        ) {
                                            Text(
                                                text = "BRÛLÉ",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontSize = 9.sp,
                                                color = VeloceOnSurfaceMuted,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${caloriesBurned[selectedIndex].roundToInt()} kcal",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Black,
                                                color = VeloceAccentCoral
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Interactive Legend Toggles (clicking shows/hides a series dynamically)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Duration Legend Checkbox
                    Row(
                        modifier = Modifier
                            .clickable { showDuration = !showDuration }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = if (showDuration) VelocePrimary else Color.Gray.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = "Durée d'entraînement",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (showDuration) Color.White else VeloceOnSurfaceMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Calories Legend Checkbox
                    Row(
                        modifier = Modifier
                            .clickable { showCalories = !showCalories }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    color = if (showCalories) VeloceAccentCoral else Color.Gray.copy(alpha = 0.4f),
                                    shape = CircleShape
                                )
                        )
                        Text(
                            text = "Calories (kcal)",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (showCalories) Color.White else VeloceOnSurfaceMuted
                        )
                    }
                }
            }
        }
    }
}

