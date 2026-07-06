package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CalorieCalculator.ActivityType
import com.example.data.database.UserProfile
import com.example.ui.VeloceViewModel
import com.example.ui.VeloceViewModel.TrackingState
import com.example.ui.components.RouteTrackVisualizer
import com.example.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackingScreen(
    viewModel: VeloceViewModel,
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    val state by viewModel.trackingState.collectAsState()
    val activityType by viewModel.selectedActivityType.collectAsState()
    val useSimulation by viewModel.useSimulation.collectAsState()
    
    val durationSec by viewModel.durationSeconds.collectAsState()
    val distanceM by viewModel.distanceMeters.collectAsState()
    val elevationG by viewModel.elevationGainMeters.collectAsState()
    val calories by viewModel.caloriesBurned.collectAsState()
    val avgSpeed by viewModel.avgSpeedKmH.collectAsState()
    val points by viewModel.trackedPoints.collectAsState()

    var showSaveDialog by remember { mutableStateOf(false) }
    var titleInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    // Metric conversions
    val isMetric = profile.metricUnits
    val distanceDisplay = if (isMetric) distanceM / 1000.0 else (distanceM / 1000.0) * 0.621371
    val distanceUnit = if (isMetric) "km" else "mi"

    val speedDisplay = if (isMetric) avgSpeed else avgSpeed * 0.621371
    val speedUnit = if (isMetric) "km/h" else "mph"

    val elevationDisplay = if (isMetric) elevationG else elevationG * 3.28084
    val elevationUnit = if (isMetric) "m" else "ft"

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state == TrackingState.IDLE) {
            // --- IDLE SCREEN (Starter Setup) ---
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "COMMENCER UNE ACTIVITÉ",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 1.sp
            )
            Text(
                text = "Sélectionnez votre sport et lancez le tracé",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Sport selection selector cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                listOf(
                    Triple(ActivityType.RUNNING, Icons.Default.DirectionsRun, "Course"),
                    Triple(ActivityType.CYCLING, Icons.Default.DirectionsBike, "Vélo"),
                    Triple(ActivityType.WALKING, Icons.Default.DirectionsWalk, "Marche"),
                    Triple(ActivityType.HIKING, Icons.Default.Terrain, "Rando")
                ).forEach { (type, icon, name) ->
                    val isSelected = activityType == type
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .border(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { viewModel.selectActivityType(type) }
                            .padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = name,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = name,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Simulation mode toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Mode Simulateur GPS 🕹️",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Simule un entraînement réel avec dénivelé et vitesse variable. Idéal pour tester dans l'émulateur !",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = useSimulation,
                    onCheckedChange = { viewModel.toggleSimulation(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("simulation_switch")
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Giant Glow Start Button
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(4.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    .clickable { viewModel.startTracking() }
                    .testTag("start_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = when (activityType) {
                            ActivityType.RUNNING -> Icons.Default.DirectionsRun
                            ActivityType.CYCLING -> Icons.Default.DirectionsBike
                            ActivityType.WALKING -> Icons.Default.DirectionsWalk
                            ActivityType.HIKING -> Icons.Default.Terrain
                        },
                        contentDescription = "Start",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "DÉMARRER",
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium,
                        letterSpacing = 1.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))

        } else {
            // --- RECORDING SCREEN (Huge Contrast HUD) ---
            Spacer(modifier = Modifier.height(8.dp))

            // Top Status Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (state == TrackingState.RECORDING) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (state == TrackingState.RECORDING) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (state == TrackingState.RECORDING) "ENREGISTREMENT" else "PAUSE",
                    color = if (state == TrackingState.RECORDING) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Giant Duration Metric
            Text(
                text = formatDuration(durationSec),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Black,
                    lineHeight = 64.sp
                ),
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("timer_text")
            )
            Text(
                text = "TEMPS ÉCOULÉ",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sub metrics grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "DISTANCE",
                    value = String.format("%.2f", distanceDisplay),
                    unit = distanceUnit,
                    icon = Icons.Default.LocationOn,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "CALORIES",
                    value = calories.roundToInt().toString(),
                    unit = "kcal",
                    icon = Icons.Default.LocalFireDepartment,
                    modifier = Modifier.weight(1f),
                    valueColor = VeloceAccentCoral
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    title = "ALLURE MOYENNE",
                    value = String.format("%.1f", speedDisplay),
                    unit = speedUnit,
                    icon = Icons.Default.Speed,
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "DÉNIVELÉ +",
                    value = elevationDisplay.roundToInt().toString(),
                    unit = elevationUnit,
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Route map trace card
            RouteTrackVisualizer(points = points)

            Spacer(modifier = Modifier.height(24.dp))

            // Control Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pause / Resume Button
                Button(
                    onClick = {
                        if (state == TrackingState.RECORDING) {
                            viewModel.pauseTracking()
                        } else {
                            viewModel.resumeTracking()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state == TrackingState.RECORDING) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .height(56.dp)
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .testTag("pause_resume_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = if (state == TrackingState.RECORDING) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Icon(
                        imageVector = if (state == TrackingState.RECORDING) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Pause",
                        tint = if (state == TrackingState.RECORDING) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (state == TrackingState.RECORDING) "PAUSE" else "REPRENDRE",
                        color = if (state == TrackingState.RECORDING) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Black
                    )
                }

                // Stop / Finish Button
                Button(
                    onClick = {
                        titleInput = "Activité de ${profile.name}"
                        notesInput = ""
                        showSaveDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .height(56.dp)
                        .weight(1f)
                        .padding(horizontal = 8.dp)
                        .testTag("stop_button"),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Color.Red
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "TERMINER",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // --- Save workout popup dialog ---
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = {
                Text(
                    text = "Enregistrer votre session 🏆",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Donnez un titre et une note à votre exploit pour le partager sur le feed public.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    OutlinedTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
                        label = { Text("Titre de l'activité") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_title_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = notesInput,
                        onValueChange = { notesInput = it },
                        label = { Text("Comment s'est passée la séance ?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("save_notes_input"),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveTrackingSession(titleInput, notesInput)
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("save_confirm_button")
                ) {
                    Text("ENREGISTRER", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        viewModel.discardTrackingSession()
                        showSaveDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red),
                    modifier = Modifier.testTag("save_discard_button")
                ) {
                    Text("SUPPRIMER")
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    unit: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color? = null
) {
    val actualValueColor = valueColor ?: MaterialTheme.colorScheme.onSurface

    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = value,
                color = actualValueColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 32.sp
            )
            Text(
                text = unit,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 3.dp)
            )
        }
    }
}

/**
 * Formats seconds to readable HH:MM:SS format
 */
fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        String.format("%02d:%02d:%02d", h, m, s)
    } else {
        String.format("%02d:%02d", m, s)
    }
}
