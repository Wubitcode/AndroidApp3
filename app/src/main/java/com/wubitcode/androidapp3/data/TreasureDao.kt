package com.wubitcode.androidapp3.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.wubitcode.androidapp3.model.TreasureLocation

/**
 * Data Access Object for TreasureLocation records.
 *
 * This interface defines all Room database operations used by the
 * Toronto Treasure Hunt application, including initialization,
 * progress tracking, reset support, and optional treasure photos.
 */
@Dao
interface TreasureDao {

    /**
     * Inserts the predefined Treasure Hunt destinations into Room.
     *
     * Existing rows with matching primary keys are replaced so the
     * database remains synchronized with the application's destination data.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(
        treasures: List<TreasureLocation>
    )

    /**
     * Retrieves every Treasure Hunt destination in sequential ID order.
     */
    @Query(
        """
        SELECT *
        FROM treasure_locations
        ORDER BY id ASC
        """
    )
    suspend fun getAllTreasures(): List<TreasureLocation>

    /**
     * Retrieves a single treasure using its unique database identifier.
     *
     * @param treasureId ID of the requested treasure.
     * @return Matching treasure or null when no record exists.
     */
    @Query(
        """
        SELECT *
        FROM treasure_locations
        WHERE id = :treasureId
        LIMIT 1
        """
    )
    suspend fun getTreasureById(
        treasureId: Int
    ): TreasureLocation?

    /**
     * Marks a treasure destination as successfully completed.
     *
     * Only the completion field is changed so the remaining treasure
     * information is preserved.
     */
    @Query(
        """
        UPDATE treasure_locations
        SET isVisited = 1
        WHERE id = :treasureId
        """
    )
    suspend fun markTreasureVisited(
        treasureId: Int
    )

    /**
     * Stores or replaces the local photo path associated with a treasure.
     *
     * The path references a photo stored in the application's private
     * storage area. A nullable value allows the association to be removed
     * later without deleting the treasure record itself.
     *
     * @param treasureId ID of the treasure receiving the photo.
     * @param photoPath Local file path of the captured treasure photo.
     */
    @Query(
        """
        UPDATE treasure_locations
        SET photoPath = :photoPath
        WHERE id = :treasureId
        """
    )
    suspend fun updateTreasurePhoto(
        treasureId: Int,
        photoPath: String?
    )

    /**
     * Counts the number of destinations that have already been completed.
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
     * Resets completion status for every Treasure Hunt destination.
     *
     * This operation affects only progress and does not delete treasure
     * destination records or their associated information.
     */
    @Query(
        """
        UPDATE treasure_locations
        SET isVisited = 0
        """
    )
    suspend fun resetProgress()
}