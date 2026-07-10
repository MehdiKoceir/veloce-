package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.SocialFeedItem
import com.example.data.database.UserProfile
import com.example.data.database.SportActivity
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ArrowForward
import com.example.ui.VeloceViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: VeloceViewModel,
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    val feedItems by viewModel.feedItems.collectAsState()
    val isMetric = profile.metricUnits

    var showCommentSheetForItem by remember { mutableStateOf<SocialFeedItem?>(null) }
    var searchInput by remember { mutableStateOf("") }

    val activities by viewModel.activities.collectAsState()
    val unpublishedActivities = remember(activities, feedItems) {
        activities.filter { activity ->
            feedItems.none { feedItem -> feedItem.localActivityId == activity.id }
        }
    }

    var activityToShare by remember { mutableStateOf<SportActivity?>(null) }

    val filteredFeed = remember(feedItems, searchInput) {
        if (searchInput.isBlank()) {
            feedItems
        } else {
            feedItems.filter {
                it.athleteName.contains(searchInput, ignoreCase = true) ||
                        it.title.contains(searchInput, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // --- Header ---
        item {
            Text(
                text = "FIL D'ACTUALITÉS",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 1.sp
            )
            Text(
                text = "Suivez et encouragez vos athlètes préférés",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        // --- Search/Follow user block ---
        item {
            OutlinedTextField(
                value = searchInput,
                onValueChange = { searchInput = it },
                placeholder = { Text("Rechercher un athlète...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = VeloceOnSurfaceMuted) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feed_search"),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VeloceSecondary,
                    unfocusedBorderColor = VeloceSecondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // --- Publish Recent Activity section ---
        if (unpublishedActivities.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = VeloceSecondaryContainer.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, VeloceSecondaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = "Share",
                                    tint = VelocePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "PARTAGER UN ENTRAÎNEMENT",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 1.sp
                                )
                            }
                            
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(VelocePrimary.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${unpublishedActivities.size} disponible(s)",
                                    color = VelocePrimary,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(unpublishedActivities) { activity ->
                                val dateStr = remember(activity.startTime) {
                                    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.FRENCH)
                                    sdf.format(Date(activity.startTime))
                                }
                                val distanceDisplay = if (isMetric) activity.distanceMeters / 1000.0 else (activity.distanceMeters / 1000.0) * 0.621371
                                val distanceUnit = if (isMetric) "km" else "mi"
                                val typeEmoji = when (activity.activityType) {
                                    "RUNNING" -> "🏃"
                                    "CYCLING" -> "🚴"
                                    "WALKING" -> "🚶"
                                    "HIKING" -> "🥾"
                                    else -> "💪"
                                }
                                
                                Card(
                                    modifier = Modifier
                                        .width(200.dp)
                                        .clickable { activityToShare = activity }
                                        .testTag("publish_picker_item_${activity.id}"),
                                    colors = CardDefaults.cardColors(
                                        containerColor = VeloceDarkSurfaceCard
                                    ),
                                    shape = RoundedCornerShape(10.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, VeloceSecondaryContainer.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "$typeEmoji ${activity.title.ifBlank { "Séance" }}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Text(
                                            text = dateStr,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = VeloceOnSurfaceMuted,
                                            fontSize = 11.sp
                                        )
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = String.format("%.2f %s", distanceDisplay, distanceUnit),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Black,
                                                    color = VelocePrimary
                                                )
                                                Text(
                                                    text = "DISTANCE",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontSize = 8.sp,
                                                    color = VeloceOnSurfaceMuted
                                                )
                                            }
                                            
                                            Icon(
                                                imageVector = Icons.Default.ArrowForward,
                                                contentDescription = "Publish",
                                                tint = VelocePrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Feed List ---
        if (filteredFeed.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = "Empty Feed",
                        tint = VeloceOnSurfaceMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Aucune activité trouvée.",
                        color = MaterialTheme.colorScheme.onBackground,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(filteredFeed, key = { it.id }) { item ->
                FeedItemCard(
                    item = item,
                    isMetric = isMetric,
                    onKudosClick = { viewModel.toggleKudos(item.id) },
                    onCommentClick = { showCommentSheetForItem = item },
                    onDeleteClick = if (item.athleteAvatar == "user") {
                        { viewModel.deleteFeedItem(item.id) }
                    } else null
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    // --- Interactive Comments Bottom Sheet / Dialog ---
    showCommentSheetForItem?.let { item ->
        // Retrieve fresh item state from list to keep comments up-to-date dynamically
        val freshItem = feedItems.find { it.id == item.id } ?: item
        CommentsDialog(
            item = freshItem,
            onDismiss = { showCommentSheetForItem = null },
            onPostComment = { text -> viewModel.postComment(freshItem.id, text) }
        )
    }

    // --- Share Activity Custom Caption Dialog ---
    activityToShare?.let { activity ->
        var customTitleInput by remember(activity) { mutableStateOf(activity.title) }

        val distanceDisplay = if (isMetric) activity.distanceMeters / 1000.0 else (activity.distanceMeters / 1000.0) * 0.621371
        val distanceUnit = if (isMetric) "km" else "mi"
        val durationDisplay = formatDuration(activity.durationMs / 1000)
        val caloriesDisplay = "${activity.calories.roundToInt()} kcal"
        
        val typeEmoji = when (activity.activityType) {
            "RUNNING" -> "🏃"
            "CYCLING" -> "🚴"
            "WALKING" -> "🚶"
            "HIKING" -> "🥾"
            else -> "💪"
        }

        AlertDialog(
            onDismissRequest = { activityToShare = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = VelocePrimary)
                    Text(
                        text = "Partager sur le fil d'actualités",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Mini summary of the activity
                    Card(
                        colors = CardDefaults.cardColors(containerColor = VeloceDarkSurfaceCard),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, VeloceSecondaryContainer.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "RÉSUMÉ DE LA SÉANCE",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = VeloceOnSurfaceMuted,
                                fontSize = 9.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$typeEmoji ${activity.title.ifBlank { "Entraînement" }}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Distance", style = MaterialTheme.typography.bodySmall, color = VeloceOnSurfaceMuted, fontSize = 10.sp)
                                    Text(String.format("%.2f %s", distanceDisplay, distanceUnit), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = Color.White)
                                }
                                Column {
                                    Text("Durée", style = MaterialTheme.typography.bodySmall, color = VeloceOnSurfaceMuted, fontSize = 10.sp)
                                    Text(durationDisplay, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = Color.White)
                                }
                                Column {
                                    Text("Calories", style = MaterialTheme.typography.bodySmall, color = VeloceOnSurfaceMuted, fontSize = 10.sp)
                                    Text(caloriesDisplay, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Black, color = VeloceAccentCoral)
                                }
                            }
                        }
                    }

                    // Caption input field
                    OutlinedTextField(
                        value = customTitleInput,
                        onValueChange = { customTitleInput = it },
                        label = { Text("Légende ou titre personnalisé") },
                        placeholder = { Text("Écrivez un message sympa...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("publish_dialog_caption_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VelocePrimary,
                            unfocusedBorderColor = VeloceSecondaryContainer
                        ),
                        maxLines = 3,
                        singleLine = false
                    )

                    // Quick suggestion chips
                    Text(
                        text = "Suggestions rapides :",
                        style = MaterialTheme.typography.bodySmall,
                        color = VeloceOnSurfaceMuted
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Nouvel exploit ! 💪", "Super sensations 🚀", "Un pas de plus ! 🔥").forEach { suggestion ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(VeloceSecondaryContainer.copy(alpha = 0.5f))
                                    .clickable {
                                        customTitleInput = if (customTitleInput.isBlank()) suggestion else "$customTitleInput - $suggestion"
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalCaption = customTitleInput.ifBlank {
                            "$typeEmoji Séance de ${profile.name}"
                        }
                        viewModel.publishActivityToFeed(activity.id, finalCaption)
                        activityToShare = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VelocePrimary),
                    modifier = Modifier.testTag("publish_dialog_submit_button")
                ) {
                    Text("PUBLIER", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { activityToShare = null },
                    modifier = Modifier.testTag("publish_dialog_cancel_button")
                ) {
                    Text("ANNULER", color = VeloceOnSurfaceMuted)
                }
            },
            containerColor = VeloceDarkSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun FeedItemCard(
    item: SocialFeedItem,
    isMetric: Boolean,
    onKudosClick: () -> Unit,
    onCommentClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null
) {
    val dateString = remember(item.startTime) {
        val sdf = SimpleDateFormat("dd MMM 'à' HH:mm", Locale.FRENCH)
        sdf.format(Date(item.startTime))
    }

    val distanceDisplay = if (isMetric) item.distanceMeters / 1000.0 else (item.distanceMeters / 1000.0) * 0.621371
    val distanceUnit = if (isMetric) "km" else "mi"

    val elevationDisplay = if (isMetric) item.elevationGain else item.elevationGain * 3.28084
    val elevationUnit = if (isMetric) "m" else "ft"

    val pace = remember(item.durationMs, item.distanceMeters) {
        if (item.distanceMeters <= 0.0) "0.0" else {
            val speedKmH = (item.distanceMeters / 1000.0) / (item.durationMs.toDouble() / 3600000.0)
            val speedDisplay = if (isMetric) speedKmH else speedKmH * 0.621371
            String.format("%.1f", speedDisplay)
        }
    }
    val paceUnit = if (isMetric) "km/h" else "mph"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("feed_item_${item.id}")
    ) {
        // Post Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Athlete Avatar (Stylized Letter Icon)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            when (item.athleteAvatar) {
                                "user" -> VelocePrimary
                                "avatar_1" -> VeloceSecondary
                                "avatar_2" -> VeloceTertiary
                                else -> Color.Gray
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.athleteName.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = item.athleteName,
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
            
            // Platform certified flag for cloud synchronizations
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (item.athleteAvatar == "user") {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(VelocePrimaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("MOI", color = VelocePrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (onDeleteClick != null) {
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("delete_feed_item_${item.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Supprimer le post",
                            tint = Color.Red.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Activity Title
        Text(
            text = item.title,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        // Activity Simple Stats Grid
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp))
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Distance", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = String.format("%.2f %s", distanceDisplay, distanceUnit),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Column {
                Text("Temps", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = formatDuration(item.durationMs / 1000),
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Column {
                Text("Allure", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "$pace $paceUnit",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Kudos and Comments action buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Kudos (Like) Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (item.hasUserLiked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.background)
                    .border(
                        1.dp,
                        if (item.hasUserLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        RoundedCornerShape(20.dp)
                    )
                    .clickable { onKudosClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("kudos_button_${item.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (item.hasUserLiked) Icons.Default.ThumbUp else Icons.Default.ThumbUpOffAlt,
                    contentDescription = "Kudos",
                    tint = if (item.hasUserLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${item.kudosCount} Kudos",
                    color = if (item.hasUserLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // Comments Pill
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(20.dp))
                    .clickable { onCommentClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .testTag("comments_button_${item.id}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Comment,
                    contentDescription = "Comments",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${item.commentsCount} Comments",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Dialog Comments Section with live typing support
 */
@Composable
fun CommentsDialog(
    item: SocialFeedItem,
    onDismiss: () -> Unit,
    onPostComment: (String) -> Unit
) {
    var commentText by remember { mutableStateOf("") }
    val comments = item.getComments()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Commentaires (${comments.size})",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // List of Comments
                if (comments.isEmpty()) {
                    Text(
                        text = "Aucun commentaire pour le moment. Soyez le premier !",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp)
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(comments) { comment ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background, shape = RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = comment.author,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                Text(
                                    text = comment.text,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input box
                OutlinedTextField(
                    value = commentText,
                    onValueChange = { commentText = it },
                    placeholder = { Text("Écrire un commentaire...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_comment_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (commentText.isNotBlank()) {
                        onPostComment(commentText)
                        commentText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.testTag("post_comment_submit")
            ) {
                Text("POSTER", color = MaterialTheme.colorScheme.onSecondary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("FERMER", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp)
    )
}
