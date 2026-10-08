package com.agrilink.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ListingCacheEntity::class, PendingActionEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun listingCache(): ListingCacheDao
    abstract fun pendingActions(): PendingActionDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "agrilink.db")
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
