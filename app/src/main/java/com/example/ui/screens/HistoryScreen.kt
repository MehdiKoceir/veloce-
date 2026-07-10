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
import com.example.ui.components.RechartsLineGraph
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
    val isOnline by viewModel.isOnline.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isMetric = profile.metricUnits
    val unsyncedCount = remember(activities) { activities.count { !it.isSynced } }

    // Calculate aggregated metrics
    val totalCount = activities.size
    val totalDistanceM = activities.sumOf { it.distanceMeters }
    val totalDurationMs = activities.sumOf { it.durationMs }
    val totalCalories = activities.sumOf { it.calories }

    val totalDistanceDisplay = if (isMetric) totalDistanceM / 1000.0 else (totalDistanceM / 1000.0) * 0.621371
    val distanceUnit = if (isMetric) "km" else "mi"
    val durationHours = totalDurationMs.toDouble() / 3600000.0

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
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 1.sp
            )
            Text(
                text = "Suivez votre progression au fil du temps",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // --- Offline / Sync Status Banner ---
        if (!isOnline || unsyncedCount > 0) {
            item {
                if (!isOnline) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = "Hors-ligne",
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Mode Hors-ligne Actif",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "Vous pouvez continuer à enregistrer vos séances d'entraînement en toute sécurité. Les données sont stockées localement et seront synchronisées automatiquement dès que vous retrouverez une connexion.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Synchronisation",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "$unsyncedCount séance(s) en attente",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Sauvegardées localement, prêtes à être synchronisées avec le cloud.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (isSyncing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.5.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                Button(
                                    onClick = { viewModel.syncOfflineActivities() },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary
                                    ),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("sync_now_button")
                                ) {
                                    Text(
                                        text = "Sync",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Aggregated Stats Grid Card ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp))
                    .border(1.dp, MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Résumé Global",
                    color = MaterialTheme.colorScheme.onSurface,
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
            RechartsLineGraph(
                activities = activities
            )
        }

        // --- Activities Section Header ---
        item {
            Text(
                text = "Séances Enregistrées",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Aucune séance pour le moment.",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "Enregistrez votre première activité dans l'onglet Tracking !",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    onUpdate = { updated -> viewModel.updateActivity(updated) },
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
    onUpdate: (SportActivity) -> Unit,
    onDelete: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

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
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp))
            .padding(14.dp)
            .testTag("history_item_${activity.id}")
    ) {
        // Card Top Row (Type icon, title, actions)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = activity.title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = dateString,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showEditDialog = true },
                    modifier = Modifier.testTag("edit_activity_${activity.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.secondary
                    )
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
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Performance Metrics Grid Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "Distance", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = String.format("%.2f %s", distanceDisplay, distanceUnit),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Column {
                Text(text = "Durée", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = formatDuration(activity.durationMs / 1000),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Column {
                Text(text = "Effort", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "${activity.calories.roundToInt()} kcal",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Column {
                Text(text = "Dénivelé", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "${elevationDisplay.roundToInt()} $elevationUnit",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }

        if (activity.notes.isNotBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = activity.notes,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(6.dp))
                    .fillMaxWidth()
                    .padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // --- Server verification certificate ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f), shape = RoundedCornerShape(6.dp))
                .border(1.dp, MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(6.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.VerifiedUser,
                contentDescription = "Verified",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (activity.isSynced) {
                    "Cloud Engine : Signature certifiée anti-triche validée."
                } else {
                    "Recalculé & validé localement."
                },
                color = MaterialTheme.colorScheme.primary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    // Modern Material 3 Dynamic Edit Dialog
    if (showEditDialog) {
        var editedTitle by remember { mutableStateOf(activity.title) }
        var editedNotes by remember { mutableStateOf(activity.notes) }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Modifier l'activité",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it },
                        label = { Text("Titre de l'activité") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                    OutlinedTextField(
                        value = editedNotes,
                        onValueChange = { editedNotes = it },
                        label = { Text("Notes / Description") },
                        modifier = Modifier.fillMaxWidth().testTag("edit_notes_input"),
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdate(activity.copy(title = editedTitle, notes = editedNotes))
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.testTag("edit_activity_submit")
                ) {
                    Text("Enregistrer", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditDialog = false },
                    modifier = Modifier.testTag("edit_activity_cancel")
                ) {
                    Text("Annuler", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
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
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            fontSize = 10.sp
        )
    }
}
