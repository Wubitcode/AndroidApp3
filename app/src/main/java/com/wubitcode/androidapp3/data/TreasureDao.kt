package com.wubitcode.androidapp3.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wubitcode.androidapp3.model.TreasureLocation

/**
 * Defines all Room database operations used by the Toronto Treasure Hunt.
 *
 * Room automatically generates the implementation of this interface.
 * Suspend functions are used so database work can later run asynchronously
 * without blocking the application's main user-interface thread.
 */
@Dao
interface TreasureDao {

    /**
     * Inserts a collection of treasure locations into the database.
     *
     * REPLACE updates an existing treasure when another record with the
     * same primary key is inserted.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(treasures: List<TreasureLocation>)

    /**
     * Retrieves all treasure locations in their official hunt order.
     */
    @Query("SELECT * FROM treasure_locations ORDER BY id ASC")
    suspend fun getAllTreasures(): List<TreasureLocation>

    /**
     * Retrieves one treasure location using its unique identifier.
     *
     * The result is nullable because the requested treasure may not yet
     * exist in the database.
     */
    @Query("SELECT * FROM treasure_locations WHERE id = :treasureId LIMIT 1")
    suspend fun getTreasureById(treasureId: Int): TreasureLocation?

    /**
     * Marks one treasure location as completed.
     */
    @Query(
        """
        UPDATE treasure_locations
        SET isVisited = 1
        WHERE id = :treasureId
        """
    )
    suspend fun markTreasureVisited(treasureId: Int)

    /**
     * Returns the number of treasure locations that have been completed.
     *
     * This value can later be used to display participant progress.
     */
    @Query(
        """
        SELECT COUNT(*)
        FROM treasure_locations
        WHERE isVisited = 1
        """
    )
    suspend fun getVisitedCount(): Int

    /**
     * Resets every treasure location to an unvisited state.
     *
     * This allows the Treasure Hunt to be restarted from Toronto City Hall.
     */
    @Query("UPDATE treasure_locations SET isVisited = 0")
    suspend fun resetProgress()
}