package com.wubitcode.androidapp3.data

import com.wubitcode.androidapp3.model.TreasureLocation

/**
 * Provides the treasure locations used by the Toronto Treasure Hunt.
 *
 * This repository will contain Toronto City Hall as the starting
 * location followed by twenty treasure hunt destinations.
 */
object TreasureRepository {

    /**
     * Returns all locations participating in the treasure hunt.
     *
     * Additional Toronto locations will be added as the application
     * is developed.
     */
    fun getTreasureLocations(): List<TreasureLocation> {

        return listOf(
            TreasureLocation(
                id = 0,
                name = "Toronto City Hall",
                address = "100 Queen St W, Toronto, ON",
                latitude = 43.6534,
                longitude = -79.3841,
                clue = "Your Toronto Treasure Hunt begins here.",
                nextClue = "Your first Toronto destination is waiting for you."
            )
        )
    }
}