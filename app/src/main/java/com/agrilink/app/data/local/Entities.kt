package com.agrilink.app.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Last successful market response per query, shown immediately and while offline. */
@Entity(tableName = "listing_cache")
data class ListingCacheEntity(
    @PrimaryKey val key: String,
    val json: String,
    val savedAt: Long,
)

/** An action taken without signal that is replayed by WorkManager when the network returns. */
@Entity(tableName = "pending_actions")
data class PendingActionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val kind: String,
    val method: String,
    val path: String,
    val body: String?,
    val label: String,
    val createdAt: Long,
    val attempts: Int = 0,
)

@Dao
interface ListingCacheDao {
    @Query("SELECT * FROM listing_cache WHERE `key` = :key")
    suspend fun get(key: String): ListingCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entity: ListingCacheEntity)

    @Query("DELETE FROM listing_cache WHERE savedAt < :before")
    suspend fun deleteOlderThan(before: Long)
}

@Dao
interface PendingActionDao {
    @Insert
    suspend fun insert(entity: PendingActionEntity): Long

    @Query("SELECT * FROM pending_actions ORDER BY id ASC")
    suspend fun all(): List<PendingActionEntity>

    @Query("SELECT COUNT(*) FROM pending_actions")
    fun count(): Flow<Int>

    @Query("DELETE FROM pending_actions WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE pending_actions SET attempts = attempts + 1 WHERE id = :id")
    suspend fun markAttempt(id: Long)

    @Query("DELETE FROM pending_actions")
    suspend fun clear()
}
