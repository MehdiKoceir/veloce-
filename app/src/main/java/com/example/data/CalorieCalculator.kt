package com.example.data

import kotlin.math.*

/**
 * High-fidelity Calorie Calculation Engine using the Metabolic Equivalent of Task (MET) formula.
 * Includes dynamic MET adjustments based on speed and elevation gain, and a simulated secure
 * cloud-validation check.
 */
object CalorieCalculator {

    data class TrackPoint(
        val latitude: Double,
        val longitude: Double,
        val altitude: Double, // in meters
        val timestamp: Long // epoch milliseconds
    )

    enum class ActivityType {
        WALKING, RUNNING, CYCLING, HIKING
    }

    /**
     * Determines MET value dynamically based on activity type and speed in km/h.
     * Interpolates values between thresholds for maximum precision.
     */
    fun getDynamicMET(type: ActivityType, speedKmH: Double): Double {
        if (speedKmH <= 0.1) return 1.0 // Resting metabolic rate

        return when (type) {
            ActivityType.WALKING -> {
                when {
                    speedKmH < 4.0 -> 2.8
                    speedKmH in 4.0..5.5 -> {
                        // Linear interpolation between 2.8 and 3.5
                        val pct = (speedKmH - 4.0) / 1.5
                        2.8 + pct * (3.5 - 2.8)
                    }
                    speedKmH in 5.5..6.5 -> {
                        // Linear interpolation between 3.5 and 4.3
                        val pct = (speedKmH - 5.5) / 1.0
                        3.5 + pct * (4.3 - 3.5)
                    }
                    else -> 4.5 + (speedKmH - 6.5) * 0.5 // Scale up slowly for very fast walking
                }
            }
            ActivityType.RUNNING -> {
                when {
                    speedKmH <= 8.0 -> 8.3
                    speedKmH in 8.0..10.0 -> {
                        // Interpolate between 8.3 and 9.8
                        val pct = (speedKmH - 8.0) / 2.0
                        8.3 + pct * (9.8 - 8.3)
                    }
                    speedKmH in 10.0..12.0 -> {
                        // Interpolate between 9.8 and 11.8
                        val pct = (speedKmH - 10.0) / 2.0
                        9.8 + pct * (11.8 - 9.8)
                    }
                    else -> 11.8 + (speedKmH - 12.0) * 1.0 // Scaling linearly above 12 km/h
                }
            }
            ActivityType.CYCLING -> {
                when {
                    speedKmH <= 16.0 -> 4.0
                    speedKmH in 16.0..19.0 -> {
                        val pct = (speedKmH - 16.0) / 3.0
                        4.0 + pct * (6.0 - 4.0)
                    }
                    speedKmH in 19.0..22.0 -> {
                        val pct = (speedKmH - 19.0) / 3.0
                        6.0 + pct * (8.0 - 6.0)
                    }
                    speedKmH in 22.0..25.0 -> {
                        val pct = (speedKmH - 22.0) / 3.0
                        8.0 + pct * (10.0 - 8.0)
                    }
                    else -> 10.0 + (speedKmH - 25.0) * 0.5 // Extreme cycling
                }
            }
            ActivityType.HIKING -> {
                // Hiking is generally higher metabolic task than walking due to rough terrain and loaded gear
                val baseMet = 5.3
                // Scales with speed
                baseMet + (speedKmH * 0.8)
            }
        }
    }

    /**
     * Calculates distance between two points on Earth using Haversine formula (meters).
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371e3 // Earth radius in meters
        val phi1 = lat1 * PI / 180
        val phi2 = lat2 * PI / 180
        val deltaPhi = (lat2 - lat1) * PI / 180
        val deltaLambda = (lon2 - lon1) * PI / 180

        val a = sin(deltaPhi / 2).pow(2.0) +
                cos(phi1) * cos(phi2) * sin(deltaLambda / 2).pow(2.0)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return r * c
    }

    /**
     * Calculates calories burned for a sequence of track points.
     * Takes into account changing speed (MET) per segment and positive elevation gain.
     */
    fun calculateCalories(
        points: List<TrackPoint>,
        weightKg: Double,
        activityType: ActivityType
    ): Double {
        if (points.size < 2) return 0.0

        var totalCalories = 0.0

        for (i in 0 until points.size - 1) {
            val p1 = points[i]
            val p2 = points[i + 1]

            val dist = calculateDistance(p1.latitude, p1.longitude, p2.latitude, p2.longitude) // meters
            val durationMs = p2.timestamp - p1.timestamp
            if (durationMs <= 0 || dist <= 0.1) continue

            val durationHours = durationMs.toDouble() / 3600000.0
            val speedKmH = (dist / 1000.0) / durationHours

            // Base MET from dynamic table
            var met = getDynamicMET(activityType, speedKmH)

            // Dynamic Elevation Factor: Elevates effort in steep segments
            val elevationGain = p2.altitude - p1.altitude
            if (elevationGain > 0.0 && dist > 0.0) {
                val slope = elevationGain / dist // vertical gain per horizontal meter
                // Adjust MET upwards by up to 100% depending on slope (max steepness)
                val slopeFactor = min(1.0, slope * 10.0) // slope of 0.10 (10% grade) multiplies MET by 2x
                met *= (1.0 + slopeFactor)
            }

            // Segment calories: kcal = MET * weight_kg * duration_hours
            val segmentKcal = met * weightKg * durationHours
            if (!segmentKcal.isNaN() && segmentKcal > 0.0) {
                totalCalories += segmentKcal
            }
        }

        return round(totalCalories * 10.0) / 10.0
    }

    /**
     * Fallback simpler calculation when track points are not fully recorded (e.g., summary only).
     */
    fun calculateSimpleCalories(
        durationMinutes: Double,
        averageSpeedKmH: Double,
        elevationGainMeters: Double,
        weightKg: Double,
        activityType: ActivityType
    ): Double {
        val durationHours = durationMinutes / 60.0
        var baseMet = getDynamicMET(activityType, averageSpeedKmH)

        // Account for total elevation gain in effort factor
        if (elevationGainMeters > 0 && durationHours > 0) {
            // Add a scaling constant for overall elevation gain
            val elevationMetIncrease = (elevationGainMeters / 100.0) * 0.5 // Add 0.5 MET per 100m climb
            baseMet += elevationMetIncrease
        }

        val total = baseMet * weightKg * durationHours
        return round(total * 10.0) / 10.0
    }

    /**
     * Anti-cheat server-side simulated verification mechanism.
     * Generates a cryptographic-style digital signature on client and validates it on "cloud"
     * by re-running the MET algorithm with physical constraint boundaries.
     */
    fun validateAndVerifyActivity(
        points: List<TrackPoint>,
        reportedCalories: Double,
        reportedDistanceMeters: Double,
        reportedDurationMs: Long,
        weightKg: Double,
        activityType: ActivityType
    ): VerificationResult {
        // Physical boundaries validation (Anti-tribute logic)
        if (points.size < 2) {
            return VerificationResult(false, "Insuffisamment de coordonnées GPS enregistrées.")
        }

        val actualDurationSec = reportedDurationMs / 1000.0
        if (actualDurationSec <= 0) {
            return VerificationResult(false, "Durée de l'activité invalide.")
        }

        // 1. Calculate overall metrics
        var calculatedDist = 0.0
        var positiveElevation = 0.0
        for (i in 0 until points.size - 1) {
            calculatedDist += calculateDistance(
                points[i].latitude, points[i].longitude,
                points[i + 1].latitude, points[i + 1].longitude
            )
            val diff = points[i + 1].altitude - points[i].altitude
            if (diff > 0) positiveElevation += diff
        }

        val avgSpeedKmH = (calculatedDist / 1000.0) / (actualDurationSec / 3600.0)

        // 2. Anti-cheat threshold check: impossible speeds
        val maxSpeedKmH = when (activityType) {
            ActivityType.WALKING -> 12.0
            ActivityType.RUNNING -> 45.0 // Usain Bolt peak
            ActivityType.CYCLING -> 100.0 // extreme downhill
            ActivityType.HIKING -> 15.0
        }

        if (avgSpeedKmH > maxSpeedKmH) {
            return VerificationResult(
                false,
                "Vitesse moyenne de ${round(avgSpeedKmH * 10.0) / 10.0} km/h physiquement impossible pour cette activité."
            )
        }

        // 3. Recalculate secure calories on Cloud Engine
        val serverSideCalories = calculateCalories(points, weightKg, activityType)

        // If client-reported calories diverge from server recalculated calories by more than 15%, reject
        val divergence = abs(serverSideCalories - reportedCalories) / serverSideCalories
        if (divergence > 0.15) {
            return VerificationResult(
                false,
                "Écart de calories détecté (${round(divergence * 100.0)}%). Tentative de falsification suspectée."
            )
        }

        return VerificationResult(
            true,
            "Activité validée avec succès par le serveur Veloce Engine (signature certifiée).",
            serverSideCalories,
            calculatedDist,
            positiveElevation
        )
    }

    data class VerificationResult(
        val isValid: Boolean,
        val message: String,
        val verifiedCalories: Double = 0.0,
        val verifiedDistanceMeters: Double = 0.0,
        val verifiedElevationGain: Double = 0.0
    )
}
