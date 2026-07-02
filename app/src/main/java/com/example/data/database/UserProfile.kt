package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1, // Only 1 profile can exist locally
    val name: String = "Athlète Veloce",
    val weightKg: Double = 70.0,
    val heightCm: Double = 175.0,
    val age: Int = 30,
    val gender: String = "Non-binaire", // Homme, Femme, Non-binaire
    val activityLevel: String = "Modéré", // Sédentaire, Modéré, Actif, Très actif
    val metricUnits: Boolean = true // True for Km/Kg, False for Miles/Lbs
)
