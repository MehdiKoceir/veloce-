package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.CalorieCalculator.TrackPoint
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "sport_activities")
data class SportActivity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: String = "", // Links to Firebase Auth user UID or local user
    val activityType: String, // WALKING, RUNNING, CYCLING, HIKING
    val startTime: Long, // Epoch timestamp millis
    val durationMs: Long,
    val distanceMeters: Double,
    val calories: Double,
    val elevationGain: Double,
    val title: String,
    val notes: String = "",
    val routePointsJson: String = "[]", // Serialized list of TrackPoint
    val isSynced: Boolean = false,
    val verificationMessage: String = ""
) {
    // Helper to get points list from local database JSON
    fun getPoints(): List<TrackPoint> {
        return try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, TrackPoint::class.java)
            val adapter = moshi.adapter<List<TrackPoint>>(type)
            adapter.fromJson(routePointsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        // Helper to serialize points list to JSON
        fun serializePoints(points: List<TrackPoint>): String {
            return try {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val type = Types.newParameterizedType(List::class.java, TrackPoint::class.java)
                val adapter = moshi.adapter<List<TrackPoint>>(type)
                adapter.toJson(points)
            } catch (e: Exception) {
                "[]"
            }
        }
    }
}
