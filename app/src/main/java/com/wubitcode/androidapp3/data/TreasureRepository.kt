package com.wubitcode.androidapp3.data

import com.wubitcode.androidapp3.model.TreasureLocation

/**
 * Provides the location data used by the Toronto Treasure Hunt.
 *
 * Keeping treasure locations in a dedicated repository separates the
 * application's data from its user-interface and map logic. This structure
 * makes it easier to add, update, and retrieve treasure locations as the
 * hunt expands.
 */
object TreasureRepository {

    /**
     * Returns the ordered list of locations participating in the
     * Toronto Treasure Hunt.
     *
     * Toronto City Hall is the official starting point. Each completed
     * location provides a clue that directs the participant toward the
     * next destination.
     */
    fun getTreasureLocations(): List<TreasureLocation> {
        return listOf(

            // Starting point for the Toronto Treasure Hunt.
            TreasureLocation(
                id = 0,
                name = "Toronto City Hall",
                address = "100 Queen St W, Toronto, ON",
                latitude = 43.6534,
                longitude = -79.3841,
                clue = "Your Toronto Treasure Hunt begins here.",
                nextClue = "Travel east along Queen Street to find an independent bookstore."
            ),

            // Treasure Stop 1
            TreasureLocation(
                id = 1,
                name = "Ben McNally Books",
                address = "108 Queen St E, Toronto, ON",
                latitude = 43.65199,
                longitude = -79.36950,
                clue = "Find the independent bookstore east of Toronto City Hall.",
                nextClue = "Your next destination is a large used bookstore near Bloor and Spadina."
            ),

            // Treasure Stop 2
            TreasureLocation(
                id = 2,
                name = "BMV Books",
                address = "471 Bloor St W, Toronto, ON",
                latitude = 43.66586,
                longitude = -79.40682,
                clue = "Search near Bloor and Spadina for shelves filled with books, music, and more.",
                nextClue = "Head south toward Queen Street West for another independent bookstore."
            ),

            // Treasure Stop 3
            TreasureLocation(
                id = 3,
                name = "Type Books",
                address = "883 Queen St W, Toronto, ON",
                latitude = 43.64555,
                longitude = -79.41155,
                clue = "Find an independent bookstore along Toronto's Queen Street West.",
                nextClue = "Your next stop is a bakery hidden in the colourful Kensington Market."
            ),

            // Treasure Stop 4
            TreasureLocation(
                id = 4,
                name = "Blackbird Baking Co.",
                address = "172 Baldwin St, Toronto, ON",
                latitude = 43.65488,
                longitude = -79.40062,
                clue = "Follow the smell of fresh bread into Kensington Market.",
                nextClue = "Travel west to Roncesvalles Avenue and look for another independent bookstore."
            ),

            // Treasure Stop 5
            TreasureLocation(
                id = 5,
                name = "Another Story Bookshop",
                address = "315 Roncesvalles Ave, Toronto, ON",
                latitude = 43.64855,
                longitude = -79.44974,
                clue = "Find the independent bookstore on Roncesvalles Avenue.",
                nextClue = "Excellent work! More Toronto treasure locations are coming next."
            )
        )
    }
}