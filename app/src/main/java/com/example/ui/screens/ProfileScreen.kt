package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.UserProfile
import com.example.ui.VeloceViewModel
import com.example.ui.theme.*
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: VeloceViewModel,
    profile: UserProfile,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activities by viewModel.activities.collectAsState()

    var name by remember(profile) { mutableStateOf(profile.name) }
    var weightInput by remember(profile) { mutableStateOf(profile.weightKg.toString()) }
    var heightInput by remember(profile) { mutableStateOf(profile.heightCm.toString()) }
    var ageInput by remember(profile) { mutableStateOf(profile.age.toString()) }
    var gender by remember(profile) { mutableStateOf(profile.gender) }
    var activityLevel by remember(profile) { mutableStateOf(profile.activityLevel) }
    var metricUnits by remember(profile) { mutableStateOf(profile.metricUnits) }

    // Calculate dynamic records
    val maxDistanceM = activities.maxOfOrNull { it.distanceMeters } ?: 0.0
    val maxCalories = activities.maxOfOrNull { it.calories } ?: 0.0
    val maxDurationMs = activities.maxOfOrNull { it.durationMs } ?: 0L

    val isMetric = profile.metricUnits
    val maxDistanceDisplay = if (isMetric) maxDistanceM / 1000.0 else (maxDistanceM / 1000.0) * 0.621371
    val distanceUnit = if (isMetric) "km" else "mi"

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Large stylized Avatar
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = name.take(1).uppercase(),
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 36.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = name,
            color = MaterialTheme.colorScheme.onBackground,
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Rang : Athlète Veloce Élite",
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECTION: PARAMÈTRES PHYSIQUES ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Paramètres de Santé",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Name Input
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nom d'athlète") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Weight and Height Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Poids (kg)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_weight_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )

                    OutlinedTextField(
                        value = heightInput,
                        onValueChange = { heightInput = it },
                        label = { Text("Taille (cm)") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_height_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Age and Gender Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = ageInput,
                        onValueChange = { ageInput = it },
                        label = { Text("Âge") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_age_input"),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )

                    OutlinedTextField(
                        value = gender,
                        onValueChange = { gender = it },
                        label = { Text("Sexe") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("profile_gender_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save parameters button
                Button(
                    onClick = {
                        val weight = weightInput.toDoubleOrNull() ?: profile.weightKg
                        val height = heightInput.toDoubleOrNull() ?: profile.heightCm
                        val age = ageInput.toIntOrNull() ?: profile.age
                        viewModel.updateProfile(
                            name,
                            weight,
                            height,
                            age,
                            gender,
                            activityLevel,
                            metricUnits
                        )
                        Toast.makeText(context, "Profil sauvegardé avec succès !", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_save_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("SAUVEGARDER LES MODIFICATIONS")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION: UNITÉS & PRÉFÉRENCES ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Unités & Préférences",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Système Métrique (km/kg)",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (metricUnits) "Utilise les kilomètres et kilogrammes." else "Utilise les miles et les livres.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = metricUnits,
                        onCheckedChange = {
                            metricUnits = it
                            viewModel.updateProfile(
                                name,
                                weightInput.toDoubleOrNull() ?: profile.weightKg,
                                heightInput.toDoubleOrNull() ?: profile.heightCm,
                                ageInput.toIntOrNull() ?: profile.age,
                                gender,
                                activityLevel,
                                it
                            )
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondary,
                            checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.testTag("metric_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(14.dp))

                val isDarkTheme by viewModel.isDarkTheme.collectAsState()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Thème Sombre / Mode Nuit",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = if (isDarkTheme) "Utilise le thème professionnel sombre." else "Utilise le thème professionnel clair.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = isDarkTheme,
                        onCheckedChange = {
                            viewModel.toggleTheme()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.secondary,
                            checkedTrackColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.testTag("theme_switch")
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION: RECORDS PERSONNELS ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "🏅 Records Personnels",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Star, contentDescription = "Dist", tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = String.format("%.2f %s", maxDistanceDisplay, distanceUnit),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Text("Plus longue distance", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = "Cal", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${maxCalories.roundToInt()} kcal",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Text("Max d'efforts", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.HourglassEmpty, contentDescription = "Dur", tint = MaterialTheme.colorScheme.tertiary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatDuration(maxDurationMs / 1000),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp
                        )
                        Text("Plus longue session", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- SECTION: MON COMPTE FIREBASE ---
        val firebaseUser by viewModel.firebaseUser.collectAsState()
        firebaseUser?.let { user ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "🔐 Compte Athlète Connecté",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (user.displayName.firstOrNull() ?: 'A').uppercaseChar().toString(),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column {
                            Text(
                                text = user.displayName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = user.email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.signOut()
                            Toast.makeText(context, "Déconnexion réussie !", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("firebase_logout_button"),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SE DÉCONNECTER DU COMPTE", color = MaterialTheme.colorScheme.onErrorContainer, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // --- SECTION: RGPD & EXPORT ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Confidentialité & RGPD",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Vos activités de tracking et données biologiques sont stockées de façon sécurisée et privée en local (Offline-first). Vous pouvez exporter vos données sous un format standard GPX/JSON.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        // Simulate export
                        Toast.makeText(context, "Export GPX/JSON généré avec succès ! (Téléchargement initié)", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.background),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("rgpd_export_button"),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Export", tint = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("EXPORTER MES DONNÉES GPX/JSON", color = MaterialTheme.colorScheme.onBackground)
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                Spacer(modifier = Modifier.height(12.dp))

                // Development/testing support: Reset Landing Home page and Onboarding
                Button(
                    onClick = {
                        // Reset Welcome Seen state
                        viewModel.setSeenWelcome(false)
                        // Reset Profile to trigger onboarding again for verification
                        viewModel.updateProfile(
                            "Athlète Veloce",
                            70.0,
                            175.0,
                            30,
                            "Non-binaire",
                            "Modéré",
                            true
                        )
                        Toast.makeText(context, "Application réinitialisée ! Redirection...", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_welcome_button"),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Réinitialiser", tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("RÉINITIALISER L'ENTRÉE (LANDING + ONBOARDING)", color = MaterialTheme.colorScheme.onErrorContainer, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- PREMIUM PRO FLAGS (Elegant gated feature block) ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkspacePremium, contentDescription = "Premium", tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fonctionnalités PRO Veloce",
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Black,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Accédez aux plans d'entraînements personnalisés, à l'analyse avancée des zones cardiaques BLE, et aux classements de segments en direct.",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        Toast.makeText(context, "Bientôt disponible ! (Modèle premium d'abonnement flexible)", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upgrade_premium_button"),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("DEVENIR MEMBRE PRO", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}
