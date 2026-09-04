package com.wubitcode.androidapp3.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents one destination in the Toronto Treasure Hunt.
 *
 * Each treasure location is stored as a Room database entity so the
 * application can preserve destination information, completion status,
 * and an optional participant photo between application sessions.
 *
 * @property id Unique identifier for the treasure destination.
 * @property name Human-readable name of the destination.
 * @property address Physical street address of the destination.
 * @property latitude Geographic latitude used for map positioning.
 * @property longitude Geographic longitude used for map positioning.
 * @property clue Hint displayed for the currently active treasure.
 * @property nextClue Hint revealed after the current treasure is completed.
 * @property isVisited Indicates whether the participant completed this stop.
 * @property photoPath Optional local path of a photo associated with the stop.
 */
@Entity(tableName = "treasure_locations")
data class TreasureLocation(

    /**
     * Unique identifier used as the Room primary key.
     */
    @PrimaryKey
    val id: Int,

    /**
     * Display name of the treasure destination.
     */
    val name: String,

    /**
     * Street address of the treasure destination.
     */
    val address: String,

    /**
     * Latitude used by MapLibre and distance calculations.
     */
    val latitude: Double,

    /**
     * Longitude used by MapLibre and distance calculations.
     */
    val longitude: Double,

    /**
     * Clue that guides the participant to this treasure.
     */
    val clue: String,

    /**
     * Clue revealed after this treasure has been completed.
     */
    val nextClue: String,

    /**
     * Persistent flag indicating whether the participant has reached
     * and completed this treasure destination.
     */
    var isVisited: Boolean = false,

    /**
     * Stores the optional local file path of a treasure photo.
     *
     * A null value means that no photo has been associated with this
     * destination yet.
     */
    var photoPath: String? = null
)