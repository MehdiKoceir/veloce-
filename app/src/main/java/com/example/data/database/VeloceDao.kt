package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface VeloceDao {

    // --- USER PROFILE ---
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getProfile(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: UserProfile)


    // --- SPORT ACTIVITIES ---
    @Query("SELECT * FROM sport_activities ORDER BY startTime DESC")
    fun getActivitiesFlow(): Flow<List<SportActivity>>

    @Query("SELECT * FROM sport_activities WHERE id = :id LIMIT 1")
    suspend fun getActivityById(id: Int): SportActivity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: SportActivity): Long

    @Query("DELETE FROM sport_activities WHERE id = :id")
    suspend fun deleteActivityById(id: Int)


    // --- SOCIAL FEED ---
    @Query("SELECT * FROM social_feed ORDER BY startTime DESC")
    fun getFeedFlow(): Flow<List<SocialFeedItem>>

    @Query("SELECT * FROM social_feed WHERE id = :id LIMIT 1")
    suspend fun getFeedItemById(id: Int): SocialFeedItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedItem(item: SocialFeedItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeedItems(items: List<SocialFeedItem>)

    @Update
    suspend fun updateFeedItem(item: SocialFeedItem)

    @Query("DELETE FROM social_feed WHERE localActivityId = :activityId")
    suspend fun deleteFeedItemByActivityId(activityId: Int)

    @Query("SELECT * FROM social_feed WHERE localActivityId = :activityId LIMIT 1")
    suspend fun getFeedItemByActivityId(activityId: Int): SocialFeedItem?

    @Query("DELETE FROM social_feed")
    suspend fun clearFeed()
}
