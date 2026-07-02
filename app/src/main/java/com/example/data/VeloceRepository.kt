package com.example.data

import android.content.Context
import com.example.data.database.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class VeloceRepository(private val dao: VeloceDao) {

    val userProfile: Flow<UserProfile?> = dao.getProfileFlow()
    val activities: Flow<List<SportActivity>> = dao.getActivitiesFlow()
    val feedItems: Flow<List<SocialFeedItem>> = dao.getFeedFlow()

    suspend fun getProfileOneShot(): UserProfile {
        var profile = dao.getProfile()
        if (profile == null) {
            profile = UserProfile()
            dao.insertProfile(profile)
        }
        return profile
    }

    suspend fun updateProfile(profile: UserProfile) {
        dao.insertProfile(profile)
    }

    suspend fun insertActivity(activity: SportActivity): Long {
        return dao.insertActivity(activity)
    }

    suspend fun deleteActivity(activityId: Int) {
        dao.deleteActivityById(activityId)
    }

    suspend fun getActivityById(activityId: Int): SportActivity? {
        return dao.getActivityById(activityId)
    }

    /**
     * Simulates syncing an activity to the cloud.
     * Recalculates/validates calories secure-side (Cloud Function), marks synced,
     * and publishes to the dynamic Social Feed!
     */
    suspend fun syncActivityToCloud(activityId: Int): CalorieCalculator.VerificationResult {
        val activity = dao.getActivityById(activityId) ?: return CalorieCalculator.VerificationResult(false, "Activité non trouvée.")
        val profile = getProfileOneShot()

        val points = activity.getPoints()
        val type = CalorieCalculator.ActivityType.valueOf(activity.activityType)

        // Run Cloud verification simulator
        val verification = CalorieCalculator.validateAndVerifyActivity(
            points = points,
            reportedCalories = activity.calories,
            reportedDistanceMeters = activity.distanceMeters,
            reportedDurationMs = activity.durationMs,
            weightKg = profile.weightKg,
            activityType = type
        )

        if (verification.isValid) {
            // Update activity state as synced
            val updated = activity.copy(
                isSynced = true,
                calories = verification.verifiedCalories,
                distanceMeters = verification.verifiedDistanceMeters,
                elevationGain = verification.verifiedElevationGain,
                verificationMessage = verification.message
            )
            dao.insertActivity(updated)

            // Add to the dynamic social feed
            val feedItem = SocialFeedItem(
                localActivityId = updated.id,
                athleteName = profile.name,
                athleteAvatar = "user",
                activityType = updated.activityType,
                title = updated.title.ifBlank { "${activityTypeEmoji(updated.activityType)} Activité de ${profile.name}" },
                distanceMeters = updated.distanceMeters,
                durationMs = updated.durationMs,
                calories = updated.calories,
                elevationGain = updated.elevationGain,
                startTime = updated.startTime,
                kudosCount = 0,
                commentsCount = 0,
                hasUserLiked = false,
                commentsJson = "[]"
            )
            dao.insertFeedItem(feedItem)
        }

        return verification
    }

    private fun activityTypeEmoji(type: String): String {
        return when (type) {
            "RUNNING" -> "🏃"
            "CYCLING" -> "🚴"
            "WALKING" -> "🚶"
            "HIKING" -> "🥾"
            else -> "💪"
        }
    }

    suspend fun toggleKudos(feedItemId: Int) {
        val item = dao.getFeedItemById(feedItemId) ?: return
        val newLiked = !item.hasUserLiked
        val newKudosCount = if (newLiked) item.kudosCount + 1 else item.kudosCount - 1
        
        dao.updateFeedItem(item.copy(
            hasUserLiked = newLiked,
            kudosCount = newKudosCount
        ))
    }

    suspend fun addComment(feedItemId: Int, author: String, text: String) {
        val item = dao.getFeedItemById(feedItemId) ?: return
        val comments = item.getComments().toMutableList()
        comments.add(SocialFeedItem.Comment(
            author = author,
            text = text,
            timestamp = System.currentTimeMillis()
        ))
        
        dao.updateFeedItem(item.copy(
            commentsCount = comments.size,
            commentsJson = SocialFeedItem.serializeComments(comments)
        ))
    }

    /**
     * Pre-populates the social feed with professional athletes and simulated workouts
     * if the feed is empty.
     */
    suspend fun initializeAppDataIfEmpty() {
        // Ensure profile exists
        getProfileOneShot()

        // Populate feed
        val currentFeed = dao.getFeedFlow().firstOrNull()
        if (currentFeed.isNullOrEmpty()) {
            val now = System.currentTimeMillis()
            
            val initialFeed = listOf(
                SocialFeedItem(
                    athleteName = "Sarah Dupont",
                    athleteAvatar = "avatar_1",
                    activityType = "CYCLING",
                    title = "🚴 Col de la Gineste sous le soleil marseillais",
                    distanceMeters = 42500.0,
                    durationMs = 5400000, // 1h30
                    calories = 890.0,
                    elevationGain = 480.0,
                    startTime = now - 10800000, // 3h ago
                    kudosCount = 24,
                    commentsCount = 2,
                    hasUserLiked = false,
                    commentsJson = SocialFeedItem.serializeComments(listOf(
                        SocialFeedItem.Comment("Karim Ben", "Incroyable allure Sarah !", now - 10000000),
                        SocialFeedItem.Comment("Yasmine L.", "Magnifique dénivelé 🏔️", now - 9500000)
                    ))
                ),
                SocialFeedItem(
                    athleteName = "Karim Ben",
                    athleteAvatar = "avatar_2",
                    activityType = "RUNNING",
                    title = "🏃 Séance d'intervalles fractionnés (Bois de Boulogne)",
                    distanceMeters = 10400.0,
                    durationMs = 2880000, // 48 mins
                    calories = 720.0,
                    elevationGain = 32.0,
                    startTime = now - 86400000, // 1 day ago
                    kudosCount = 18,
                    commentsCount = 1,
                    hasUserLiked = false,
                    commentsJson = SocialFeedItem.serializeComments(listOf(
                        SocialFeedItem.Comment("Sarah Dupont", "Une machine !", now - 80000000)
                    ))
                ),
                SocialFeedItem(
                    athleteName = "Yasmine Larbi",
                    athleteAvatar = "avatar_3",
                    activityType = "HIKING",
                    title = "🥾 Randonnée d'altitude à Chréa (Algérie)",
                    distanceMeters = 12800.0,
                    durationMs = 12600000, // 3h30
                    calories = 950.0,
                    elevationGain = 680.0,
                    startTime = now - 172800000, // 2 days ago
                    kudosCount = 35,
                    commentsCount = 3,
                    hasUserLiked = false,
                    commentsJson = SocialFeedItem.serializeComments(listOf(
                        SocialFeedItem.Comment("Karim Ben", "L'air pur de la montagne !", now - 160000000),
                        SocialFeedItem.Comment("Sarah Dupont", "Les photos ont l'air magiques", now - 150000000),
                        SocialFeedItem.Comment("Athlète Anonyme", "Bravo pour le dénivelé cumulé !", now - 140000000)
                    ))
                )
            )
            
            dao.insertFeedItems(initialFeed)
        }
    }
}
