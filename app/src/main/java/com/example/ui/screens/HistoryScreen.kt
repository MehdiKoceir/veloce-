package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CalorieCalculator
import com.example.data.database.SportActivity
import com.example.data.database.UserProfile
import com.example.ui.VeloceViewModel
import com.example.ui.components.AthleticBarChart
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    viewModel: VeloceViewModel,
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    val activities by viewModel.activities.collectAsState()
    val isMetric = profile.metricUnits

    // Calculate aggregated metrics
    val totalCount = activities.size
    val totalDistanceM = activities.sumOf { it.distanceMeters }
    val totalDurationMs = activities.sumOf { it.durationMs }
    val totalCalories = activities.sumOf { it.calories }

    val totalDistanceDisplay = if (isMetric) totalDistanceM / 1000.0 else (totalDistanceM / 1000.0) * 0.621371
    val distanceUnit = if (isMetric) "km" else "mi"
    val durationHours = totalDurationMs.toDouble() / 3600000.0

    // Prepare chart data for last 7 days (or general activity progression)
    val chartData = remember(activities) {
        if (activities.isEmpty()) {
            listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
        } else {
            // Take up to last 7 activities as bars, pad with 0s if less
            val last7 = activities.take(7).reversed().map { (it.distanceMeters / 1000f).toFloat() }
            val padding = 7 - last7.size
            List(padding) { 0f } + last7
        }
    }
    
    val chartLabels = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Title Header ---
        item {
            Text(
                text = "HISTORIQUE & STATISTIQUES",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
            Text(
                text = "Suivez votre progression au fil du temps",
                style = MaterialTheme.typography.bodySmall,
                color = VeloceOnSurfaceMuted,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // --- Aggregated Stats Grid Card ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(VeloceDarkSurface, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, VeloceSecondaryContainer, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Résumé Global",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MiniStatItem(
                        title = "Séances",
                        value = totalCount.toString(),
                        icon = Icons.Default.DirectionsRun,
                        modifier = Modifier.weight(1f)
                    )
                    MiniStatItem(
                        title = "Distance",
                        value = String.format("%.1f %s", totalDistanceDisplay, distanceUnit),
                        icon = Icons.Default.LocationOn,
                        modifier = Modifier.weight(1.5f)
                    )
                    MiniStatItem(
                        title = "Calories",
                        value = String.format("%d kcal", totalCalories.roundToInt()),
                        icon = Icons.Default.LocalFireDepartment,
                        modifier = Modifier.weight(1.5f)
                    )
                }
            }
        }

        // --- Performance Progression Chart ---
        item {
            AthleticBarChart(
                data = chartData,
                labels = chartLabels,
                metricLabel = "Volume d'entraînement (${distanceUnit} par séance)"
            )
        }

        // --- Activities Section Header ---
        item {
            Text(
                text = "Séances Enregistrées",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // --- Activities List Items ---
        if (activities.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = "Aucun",
                        tint = VeloceOnSurfaceMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Aucune séance pour le moment.",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Enregistrez votre première activité dans l'onglet Tracking !",
                        color = VeloceOnSurfaceMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        } else {
            items(activities, key = { it.id }) { activity ->
                ActivityHistoryCard(
                    activity = activity,
                    isMetric = isMetric,
                    onDelete = { viewModel.deleteActivity(activity.id) }
                )
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityHistoryCard(
    activity: SportActivity,
    isMetric: Boolean,
    onDelete: () -> Unit
) {
    val dateString = remember(activity.startTime) {
        val sdf = SimpleDateFormat("dd MMMM yyyy 'à' HH:mm", Locale.FRENCH)
        sdf.format(Date(activity.startTime))
    }

    val type = remember(activity.activityType) {
        try {
            CalorieCalculator.ActivityType.valueOf(activity.activityType)
        } catch (e: Exception) {
            CalorieCalculator.ActivityType.RUNNING
        }
    }

    val icon = when (type) {
        CalorieCalculator.ActivityType.RUNNING -> Icons.Default.DirectionsRun
        CalorieCalculator.ActivityType.CYCLING -> Icons.Default.DirectionsBike
        CalorieCalculator.ActivityType.WALKING -> Icons.Default.DirectionsWalk
        CalorieCalculator.ActivityType.HIKING -> Icons.Default.Terrain
    }

    val distanceDisplay = if (isMetric) activity.distanceMeters / 1000.0 else (activity.distanceMeters / 1000.0) * 0.621371
    val distanceUnit = if (isMetric) "km" else "mi"

    val elevationDisplay = if (isMetric) activity.elevationGain else activity.elevationGain * 3.28084
    val elevationUnit = if (isMetric) "m" else "ft"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(VeloceDarkSurface, shape = RoundedCornerShape(12.dp))
            .border(1.dp, VeloceSecondaryContainer, shape = RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("history_item_${activity.id}")
    ) {
        // Card Top Row (Type icon, title, delete)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(VelocePrimaryContainer, shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = VelocePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = activity.title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = dateString,
                        color = VeloceOnSurfaceMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_activity_${activity.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.Red.copy(alpha = 0.8f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Performance Metrics Grid Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Distance", color = VeloceOnSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = String.format("%.2f %s", distanceDisplay, distanceUnit),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Column {
                Text(text = "Durée", color = VeloceOnSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = formatDuration(activity.durationMs / 1000),
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Column {
                Text(text = "Effort", color = VeloceOnSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "${activity.calories.roundToInt()} kcal",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Column {
                Text(text = "Dénivelé", color = VeloceOnSurfaceMuted, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "${elevationDisplay.roundToInt()} $elevationUnit",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        if (activity.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = activity.notes,
                color = VeloceOnSurface,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .background(VeloceDarkBackground, shape = RoundedCornerShape(6.dp))
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- Server verification certificate ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(VelocePrimaryContainer.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                .border(1.dp, VelocePrimaryContainer, shape = RoundedCornerShape(6.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "Verified",
                tint = VelocePrimary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (activity.isSynced) {
                    "Cloud Engine : Signature certifiée anti-triche validée."
                } else {
                    "Recalculé & validé localement."
                },
                color = VelocePrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MiniStatItem(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = VeloceSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = title,
            color = VeloceOnSurfaceMuted,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp
        )
    }
}
