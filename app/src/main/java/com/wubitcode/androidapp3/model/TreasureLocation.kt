package com.wubitcode.androidapp3.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents one destination in the Toronto Treasure Hunt.
 *
 * This class is also a Room database entity so treasure information and
 * completion status can be stored locally on the Android device.
 */
@Entity(tableName = "treasure_locations")
data class TreasureLocation(

    /**
     * Unique identifier for each treasure location.
     *
     * IDs are assigned manually by TreasureRepository, so Room does not
     * automatically generate primary-key values.
     */
    @PrimaryKey
    val id: Int,

    // Name of the treasure destination or local business.
    val name: String,

    // Street address associated with the treasure location.
    val address: String,

    // Latitude used for map positioning and distance calculations.
    val latitude: Double,

    // Longitude used for map positioning and distance calculations.
    val longitude: Double,

    // Clue shown while the participant searches for this destination.
    val clue: String,

    // Clue revealed after this treasure has been completed.
    val nextClue: String,

    // Records whether the participant has successfully visited this location.
    var isVisited: Boolean = false
)