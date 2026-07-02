package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserProfile::class, SportActivity::class, SocialFeedItem::class],
    version = 1,
    exportSchema = false
)
abstract class VeloceDatabase : RoomDatabase() {

    abstract fun veloceDao(): VeloceDao

    companion object {
        @Volatile
        private var INSTANCE: VeloceDatabase? = null

        fun getDatabase(context: Context): VeloceDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VeloceDatabase::class.java,
                    "veloce_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
