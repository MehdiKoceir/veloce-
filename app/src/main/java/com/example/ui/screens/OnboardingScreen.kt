package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.database.UserProfile
import com.example.ui.theme.VeloceDarkBackground
import com.example.ui.theme.VeloceOnSurfaceMuted

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    currentProfile: UserProfile?,
    onComplete: (name: String, weight: Double, height: Double, age: Int, gender: String, activityLevel: String, metric: Boolean) -> Unit
) {
    if (currentProfile == null) return

    var name by remember { mutableStateOf(currentProfile.name) }
    var weightInput by remember { mutableStateOf(currentProfile.weightKg.toString()) }
    var heightInput by remember { mutableStateOf(currentProfile.heightCm.toString()) }
    var ageInput by remember { mutableStateOf(currentProfile.age.toString()) }
    var gender by remember { mutableStateOf(currentProfile.gender) }
    var activityLevel by remember { mutableStateOf(currentProfile.activityLevel) }
    var metric by remember { mutableStateOf(currentProfile.metricUnits) }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 1. App Title / Logo
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.DirectionsRun,
                contentDescription = "Veloce",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "VELOCE",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
            )
        }

        Text(
            text = "L'énergie de l'effort, mesurée précisément.",
            style = MaterialTheme.typography.bodyMedium,
            color = VeloceOnSurfaceMuted,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 2. Custom Generated Illustration Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp))
        ) {
            Image(
                painter = painterResource(id = R.drawable.veloce_hero_banner),
                contentDescription = "Coureur Veloce",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Profil Physiologique",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            text = "Ces données sont requises pour calculer précisément vos calories brûlées via le moteur certifié MET.",
            style = MaterialTheme.typography.bodySmall,
            color = VeloceOnSurfaceMuted,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. User Inputs
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nom d'athlète") },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("onboarding_name"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.secondaryContainer
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = weightInput,
                onValueChange = { weightInput = it },
                label = { Text("Poids (${if (metric) "kg" else "lbs"})") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("onboarding_weight"),
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
                label = { Text("Taille (${if (metric) "cm" else "in"})") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("onboarding_height"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = ageInput,
                onValueChange = { ageInput = it },
                label = { Text("Âge") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("onboarding_age"),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )

            // Gender selector
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sexe",
                    style = MaterialTheme.typography.bodySmall,
                    color = VeloceOnSurfaceMuted,
                    modifier = Modifier.padding(start = 4.dp, bottom = 2.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            RoundedCornerShape(8.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.secondaryContainer,
                            RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("H", "F", "NB").forEach { option ->
                        val selected = when (option) {
                            "H" -> gender == "Homme"
                            "F" -> gender == "Femme"
                            else -> gender == "Non-binaire"
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp, horizontal = 2.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent)
                                .border(
                                    if (selected) 0.dp else 1.dp,
                                    if (selected) androidx.compose.ui.graphics.Color.Transparent else MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
                                    RoundedCornerShape(6.dp)
                                )
                                .clickableSingle {
                                    gender = when (option) {
                                        "H" -> "Homme"
                                        "F" -> "Femme"
                                        else -> "Non-binaire"
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                color = if (selected) MaterialTheme.colorScheme.onPrimary else VeloceOnSurfaceMuted,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Activity Level Selection
        Text(
            text = "Niveau d'activité physique",
            style = MaterialTheme.typography.bodySmall,
            color = VeloceOnSurfaceMuted,
            modifier = Modifier.align(Alignment.Start)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Modéré", "Actif", "Élite").forEach { level ->
                val selected = activityLevel == level
                Button(
                    onClick = { activityLevel = level },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selected) MaterialTheme.colorScheme.onSecondary else VeloceOnSurfaceMuted
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = level, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Let's go button!
        Button(
            onClick = {
                val weight = weightInput.toDoubleOrNull() ?: 70.0
                val height = heightInput.toDoubleOrNull() ?: 175.0
                val age = ageInput.toIntOrNull() ?: 30
                onComplete(
                    name.ifBlank { "Athlète Veloce" },
                    weight,
                    height,
                    age,
                    gender,
                    activityLevel,
                    metric
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("onboarding_complete_button"),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "COMMENCER L'ENTRAÎNEMENT",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.titleMedium
            )
        }
        
        Spacer(modifier = Modifier.height(40.dp))
    }
}

// Inline extension to avoid duplicate click triggers
@Composable
fun Modifier.clickableSingle(onClick: () -> Unit): Modifier {
    return this.clickable { onClick() }
}
