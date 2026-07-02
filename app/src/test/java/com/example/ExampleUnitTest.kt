package com.example

import com.example.data.CalorieCalculator
import com.example.data.CalorieCalculator.ActivityType
import com.example.data.CalorieCalculator.TrackPoint
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testGetDynamicMET_Walking() {
        // Slow walk < 4.0 km/h should give 2.8 MET
        assertEquals(2.8, CalorieCalculator.getDynamicMET(ActivityType.WALKING, 2.5), 0.01)

        // Fast walk 5.5-6.5 km/h should give approx 4.3 MET
        assertEquals(4.3, CalorieCalculator.getDynamicMET(ActivityType.WALKING, 6.5), 0.01)
    }

    @Test
    fun testGetDynamicMET_Running() {
        // Slow run <= 8.0 km/h should give 8.3 MET
        assertEquals(8.3, CalorieCalculator.getDynamicMET(ActivityType.RUNNING, 7.5), 0.01)

        // Moderate run 10.0 km/h should give 9.8 MET
        assertEquals(9.8, CalorieCalculator.getDynamicMET(ActivityType.RUNNING, 10.0), 0.01)

        // Fast run 12.0 km/h should give 11.8 MET
        assertEquals(11.8, CalorieCalculator.getDynamicMET(ActivityType.RUNNING, 12.0), 0.01)
    }

    @Test
    fun testGetDynamicMET_Cycling() {
        // Moderate cycling 19.0-22.0 km/h should scale around 6.0 to 8.0 MET
        assertEquals(6.0, CalorieCalculator.getDynamicMET(ActivityType.CYCLING, 19.0), 0.01)
        assertEquals(8.0, CalorieCalculator.getDynamicMET(ActivityType.CYCLING, 22.0), 0.01)
    }

    @Test
    fun testCalculateDistance() {
        // Paris Tour Eiffel to Arc de Triomphe distance is approx 2.1 - 2.2 km
        val distance = CalorieCalculator.calculateDistance(
            48.8584, 2.2945, // Eiffel
            48.8738, 2.2950  // Arc
        )
        // Verify it calculates around 1715 meters
        assertTrue(distance in 1600.0..1800.0)
    }

    @Test
    fun testCalculateCalories() {
        val now = System.currentTimeMillis()
        // Simulate a 10 km/h running session of 1 hour (3600000 ms) with 3 points
        // Point 1 (Start)
        val p1 = TrackPoint(48.8584, 2.2945, 35.0, now)
        // Point 2 (Midpoint, 5km away, 30 mins later)
        val p2 = TrackPoint(48.8134, 2.2945, 35.0, now + 1800000)
        // Point 3 (End point, another 5km away, 30 mins later)
        val p3 = TrackPoint(48.7684, 2.2945, 35.0, now + 3600000)

        val points = listOf(p1, p2, p3)
        val weightKg = 70.0 // 70 kg

        val calories = CalorieCalculator.calculateCalories(points, weightKg, ActivityType.RUNNING)

        // Estimated Calories: MET for 10 km/h is 9.8.
        // Calories = 9.8 * 70 kg * 1.0 hour = 686 kcal.
        // Let's assert it calculates around that!
        assertEquals(686.0, calories, 10.0)
    }

    @Test
    fun testCalculateSimpleCalories_WithElevation() {
        // Test elevation gain factor in simple calorie calculations
        val calNoElevation = CalorieCalculator.calculateSimpleCalories(
            durationMinutes = 60.0,
            averageSpeedKmH = 10.0,
            elevationGainMeters = 0.0,
            weightKg = 70.0,
            activityType = ActivityType.RUNNING
        )

        val calWithElevation = CalorieCalculator.calculateSimpleCalories(
            durationMinutes = 60.0,
            averageSpeedKmH = 10.0,
            elevationGainMeters = 500.0, // 500m climb!
            weightKg = 70.0,
            activityType = ActivityType.RUNNING
        )

        // Climb should increase physical effort and yield higher calories
        assertTrue(calWithElevation > calNoElevation)
    }

    @Test
    fun testAntiCheatValidation() {
        val now = System.currentTimeMillis()
        val p1 = TrackPoint(48.8584, 2.2945, 35.0, now)
        val p2 = TrackPoint(48.8585, 2.2946, 35.0, now + 5000) // very small movement, short duration

        val points = listOf(p1, p2)
        val expectedCalories = CalorieCalculator.calculateCalories(points, 70.0, ActivityType.WALKING)

        val result = CalorieCalculator.validateAndVerifyActivity(
            points = points,
            reportedCalories = expectedCalories,
            reportedDistanceMeters = 15.0,
            reportedDurationMs = 5000,
            weightKg = 70.0,
            activityType = ActivityType.WALKING
        )

        // Since it's within realistic bounds, it should pass verification!
        assertTrue(result.isValid)
    }

    @Test
    fun testAntiCheatSuspectFalsification() {
        val now = System.currentTimeMillis()
        val p1 = TrackPoint(48.8584, 2.2945, 35.0, now)
        val p2 = TrackPoint(48.8585, 2.2946, 35.0, now + 5000)

        val result = CalorieCalculator.validateAndVerifyActivity(
            points = listOf(p1, p2),
            reportedCalories = 9999.0, // Impossibly high calories reported for 5 seconds walk!
            reportedDistanceMeters = 15.0,
            reportedDurationMs = 5000,
            weightKg = 70.0,
            activityType = ActivityType.WALKING
        )

        // Should be rejected by anti-cheat server check
        assertFalse(result.isValid)
        assertTrue(result.message.contains("suspectée") || result.message.contains("impossible"))
    }
}
