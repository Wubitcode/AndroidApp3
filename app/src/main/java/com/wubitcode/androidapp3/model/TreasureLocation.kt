package com.wubitcode.androidapp3.model

/**
 * Represents one destination in the Toronto Treasure Hunt.
 *
 * Each location contains the geographic coordinates needed for map and
 * location features, along with a clue and completion status used to
 * track the participant's progress through the hunt.
 */
data class TreasureLocation(
    val id: Int,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val clue: String,
    val nextClue: String,
    var isVisited: Boolean = false
)