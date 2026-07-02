package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

@Entity(tableName = "social_feed")
data class SocialFeedItem(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val localActivityId: Int? = null, // Ref to user's activity if synced
    val athleteName: String,
    val athleteAvatar: String, // "avatar_1", "avatar_2", "user"
    val activityType: String,
    val title: String,
    val distanceMeters: Double,
    val durationMs: Long,
    val calories: Double,
    val elevationGain: Double,
    val startTime: Long,
    val kudosCount: Int = 0,
    val commentsCount: Int = 0,
    val hasUserLiked: Boolean = false,
    val commentsJson: String = "[]" // List of Comment items
) {
    data class Comment(
        val author: String,
        val text: String,
        val timestamp: Long
    )

    fun getComments(): List<Comment> {
        return try {
            val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val type = Types.newParameterizedType(List::class.java, Comment::class.java)
            val adapter = moshi.adapter<List<Comment>>(type)
            adapter.fromJson(commentsJson) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    companion object {
        fun serializeComments(comments: List<Comment>): String {
            return try {
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val type = Types.newParameterizedType(List::class.java, Comment::class.java)
                val adapter = moshi.adapter<List<Comment>>(type)
                adapter.toJson(comments)
            } catch (e: Exception) {
                "[]"
            }
        }
    }
}
