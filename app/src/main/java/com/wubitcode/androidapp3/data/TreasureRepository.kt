package com.wubitcode.androidapp3.data

import com.wubitcode.androidapp3.model.TreasureLocation

/**
 * Provides the ordered location data and Room persistence operations used by
 * the Toronto Treasure Hunt application.
 *
 * Toronto City Hall serves as the official starting point. The participant
 * must then visit twenty Toronto businesses in sequence. Each completed
 * destination reveals a clue leading to the next treasure location.
 *
 * Keeping Treasure Hunt data and persistence operations in a dedicated
 * repository separates application data from map and user-interface logic.
 * This structure also makes the application easier to maintain, test,
 * and expand with additional features such as treasure photos.
 */
object TreasureRepository {

    /**
     * Initializes the Room database with the predefined Toronto Treasure Hunt
     * locations when the database is empty.
     *
     * Existing database records are preserved so previously completed
     * treasure progress and associated photo information are not overwritten
     * when the application is reopened.
     *
     * @param treasureDao DAO used to access persistent Treasure Hunt data.
     */
    suspend fun initializeDatabase(
        treasureDao: TreasureDao
    ) {

        val storedTreasures =
            treasureDao.getAllTreasures()

        /*
         * Predefined destination data is inserted only when Room contains
         * no treasure records. This protects previously saved participant
         * progress from being replaced during subsequent application launches.
         */
        if (storedTreasures.isEmpty()) {

            treasureDao.insertAll(
                getTreasureLocations()
            )
        }
    }

    /**
     * Retrieves all Treasure Hunt locations currently stored in Room.
     *
     * The locations are returned in official hunt order according to their
     * unique treasure ID values.
     *
     * @param treasureDao DAO used to retrieve persistent treasure records.
     * @return Ordered list of stored Treasure Hunt destinations.
     */
    suspend fun getStoredTreasureLocations(
        treasureDao: TreasureDao
    ): List<TreasureLocation> {

        return treasureDao.getAllTreasures()
    }

    /**
     * Records completion of one treasure location in the Room database.
     *
     * Persisting the visited state allows participant progress to remain
     * available after the application is closed, restarted, or recreated.
     *
     * @param treasureDao DAO used to update persistent Treasure Hunt data.
     * @param treasureId Unique ID of the completed treasure destination.
     */
    suspend fun markTreasureVisited(
        treasureDao: TreasureDao,
        treasureId: Int
    ) {

        treasureDao.markTreasureVisited(
            treasureId
        )
    }

    /**
     * Saves or replaces the local photo path associated with a treasure.
     *
     * The repository delegates the database update to TreasureDao so
     * Activities and other user-interface components do not need to interact
     * directly with Room database queries.
     *
     * A nullable photo path allows a treasure to exist without a photo and
     * also supports removing a previously stored photo association if that
     * feature is added later.
     *
     * @param treasureDao DAO used to update persistent Treasure Hunt data.
     * @param treasureId Unique ID of the treasure receiving the photo.
     * @param photoPath Local file path of the treasure photo, or null when
     * no photo is associated with the destination.
     */
    suspend fun updateTreasurePhoto(
        treasureDao: TreasureDao,
        treasureId: Int,
        photoPath: String?
    ) {

        treasureDao.updateTreasurePhoto(
            treasureId,
            photoPath
        )
    }

    /**
     * Returns the number of completed treasure locations stored in Room.
     *
     * This value can be used by the progress screen to display information
     * such as "8 of 21 treasures completed."
     *
     * @param treasureDao DAO used to retrieve persistent progress data.
     * @return Number of treasure locations marked as visited.
     */
    suspend fun getVisitedCount(
        treasureDao: TreasureDao
    ): Int {

        return treasureDao.getVisitedCount()
    }

    /**
     * Resets all Room-based Treasure Hunt completion progress.
     *
     * Destination records remain in the database while their visited status
     * is changed back to false. Treasure location information is not deleted.
     *
     * @param treasureDao DAO used to reset persistent Treasure Hunt progress.
     */
    suspend fun resetStoredProgress(
        treasureDao: TreasureDao
    ) {

        treasureDao.resetProgress()
    }

    /**
     * Returns all locations participating in the Toronto Treasure Hunt.
     *
     * Index 0 represents the starting point at Toronto City Hall.
     * Indexes 1 through 20 represent the twenty business destinations.
     *
     * These predefined objects provide the initial data inserted into Room
     * when the Treasure Hunt database is first created.
     */
    fun getTreasureLocations(): List<TreasureLocation> {

        return listOf(

            /*
             * Official Treasure Hunt starting point.
             */
            TreasureLocation(
                id = 0,
                name = "Toronto City Hall",
                address = "100 Queen St W, Toronto, ON",
                latitude = 43.6534,
                longitude = -79.3841,
                clue = "Your Toronto Treasure Hunt begins here.",
                nextClue = "Travel east along Queen Street to find an independent bookstore."
            ),

            /*
             * Business Stop 1
             */
            TreasureLocation(
                id = 1,
                name = "Ben McNally Books",
                address = "108 Queen St E, Toronto, ON",
                latitude = 43.65199,
                longitude = -79.36950,
                clue = "Find the independent bookstore east of Toronto City Hall.",
                nextClue = "Head toward Bloor and Spadina and search for a large used bookstore."
            ),

            /*
             * Business Stop 2
             */
            TreasureLocation(
                id = 2,
                name = "BMV Books",
                address = "471 Bloor St W, Toronto, ON",
                latitude = 43.66586,
                longitude = -79.40682,
                clue = "Find the bookstore filled with used books, music, and movies near the Annex.",
                nextClue = "Travel south toward Queen Street West and find another independent bookstore."
            ),

            /*
             * Business Stop 3
             */
            TreasureLocation(
                id = 3,
                name = "Type Books",
                address = "883 Queen St W, Toronto, ON",
                latitude = 43.64555,
                longitude = -79.41155,
                clue = "Find the independent bookstore along Queen Street West.",
                nextClue = "Your next destination is a bakery hidden inside colourful Kensington Market."
            ),

            /*
             * Business Stop 4
             */
            TreasureLocation(
                id = 4,
                name = "Blackbird Baking Co.",
                address = "172 Baldwin St, Toronto, ON",
                latitude = 43.65488,
                longitude = -79.40062,
                clue = "Follow the smell of fresh bread through Kensington Market.",
                nextClue = "Travel west to Roncesvalles Avenue and look for another independent bookstore."
            ),

            /*
             * Business Stop 5
             */
            TreasureLocation(
                id = 5,
                name = "Another Story Bookshop",
                address = "315 Roncesvalles Ave, Toronto, ON",
                latitude = 43.64855,
                longitude = -79.44974,
                clue = "Find the neighbourhood bookstore on Roncesvalles Avenue.",
                nextClue = "Head east to Toronto's historic Distillery District and look for a famous coffee roaster."
            ),

            /*
             * Business Stop 6
             */
            TreasureLocation(
                id = 6,
                name = "Balzac's Coffee Roasters",
                address = "1 Trinity St, Toronto, ON",
                latitude = 43.650126,
                longitude = -79.359408,
                clue = "Find the coffee shop inside Toronto's historic Distillery District.",
                nextClue = "Stay in the Distillery District and search for a shop dedicated to chocolate."
            ),

            /*
             * Business Stop 7
             */
            TreasureLocation(
                id = 7,
                name = "SOMA Chocolatemaker",
                address = "32 Tank House Lane, Toronto, ON",
                latitude = 43.65071,
                longitude = -79.35825,
                clue = "Find the chocolate shop hidden among the brick buildings of the Distillery District.",
                nextClue = "Travel west to Ossington Avenue and look for a specialty coffee roaster."
            ),

            /*
             * Business Stop 8
             */
            TreasureLocation(
                id = 8,
                name = "Pilot Coffee Roasters",
                address = "117 Ossington Ave, Toronto, ON",
                latitude = 43.6466154,
                longitude = -79.4195051,
                clue = "Find the coffee roaster along busy Ossington Avenue.",
                nextClue = "Return downtown and find an elegant coffee shop inside a historic building on Yonge Street."
            ),

            /*
             * Business Stop 9
             */
            TreasureLocation(
                id = 9,
                name = "Dineen Coffee Co.",
                address = "140 Yonge St, Toronto, ON",
                latitude = 43.651122,
                longitude = -79.379024,
                clue = "Find the coffee shop inside the historic Dineen Building.",
                nextClue = "Head toward Kensington Market and look for a coffee shop on Baldwin Street."
            ),

            /*
             * Business Stop 10
             */
            TreasureLocation(
                id = 10,
                name = "Jimmy's Coffee",
                address = "191 Baldwin St, Toronto, ON",
                latitude = 43.65490,
                longitude = -79.40090,
                clue = "Find the neighbourhood coffee shop on Baldwin Street.",
                nextClue = "Stay in Kensington Market and find a tiny taco shop nearby."
            ),

            /*
             * Business Stop 11
             */
            TreasureLocation(
                id = 11,
                name = "Seven Lives Tacos y Mariscos",
                address = "72 Kensington Ave, Toronto, ON",
                latitude = 43.65455,
                longitude = -79.40077,
                clue = "Find the popular taco shop in the heart of Kensington Market.",
                nextClue = "Travel toward Toronto's Entertainment District and find a Northern Thai restaurant."
            ),

            /*
             * Business Stop 12
             */
            TreasureLocation(
                id = 12,
                name = "PAI Northern Thai Kitchen",
                address = "18 Duncan St, Toronto, ON",
                latitude = 43.6478303,
                longitude = -79.3888174,
                clue = "Find the Northern Thai restaurant on Duncan Street.",
                nextClue = "Head west along Queen Street to find a long-running Italian restaurant."
            ),

            /*
             * Business Stop 13
             */
            TreasureLocation(
                id = 13,
                name = "Terroni Queen",
                address = "720 Queen St W, Toronto, ON",
                latitude = 43.646254,
                longitude = -79.4091805,
                clue = "Find the Italian restaurant that began on Queen Street West.",
                nextClue = "Your next stop is an Italian bakery on King Street West."
            ),

            /*
             * Business Stop 14
             */
            TreasureLocation(
                id = 14,
                name = "Forno Cultura",
                address = "609 King St W, Toronto, ON",
                latitude = 43.64418,
                longitude = -79.40072,
                clue = "Find the Italian bakery along King Street West.",
                nextClue = "Travel far west toward Mimico and search for a well-known Italian bakery."
            ),

            /*
             * Business Stop 15
             */
            TreasureLocation(
                id = 15,
                name = "SanRemo Bakery",
                address = "374 Royal York Rd, Toronto, ON",
                latitude = 43.61854,
                longitude = -79.49952,
                clue = "Find the long-running bakery on Royal York Road.",
                nextClue = "Head east to Riverdale and find a coffee shop overlooking the park."
            ),

            /*
             * Business Stop 16
             */
            TreasureLocation(
                id = 16,
                name = "Rooster Coffee House",
                address = "479 Broadview Ave, Toronto, ON",
                latitude = 43.66920,
                longitude = -79.35274,
                clue = "Find the coffee house overlooking Riverdale Park.",
                nextClue = "Return to Queen Street West and search for a French-style patisserie."
            ),

            /*
             * Business Stop 17
             */
            TreasureLocation(
                id = 17,
                name = "Nadège Patisserie",
                address = "780 Queen St W, Toronto, ON",
                latitude = 43.64584,
                longitude = -79.41143,
                clue = "Find the French-style patisserie along Queen Street West.",
                nextClue = "Head toward Harbord Street and search for a bookstore specializing in science fiction and fantasy."
            ),

            /*
             * Business Stop 18
             */
            TreasureLocation(
                id = 18,
                name = "Bakka-Phoenix Books",
                address = "84 Harbord St, Toronto, ON",
                latitude = 43.6631218,
                longitude = -79.4026244,
                clue = "Find the bookstore known for science fiction and fantasy.",
                nextClue = "Travel east to Queen Street East and find an independent neighbourhood bookstore."
            ),

            /*
             * Business Stop 19
             */
            TreasureLocation(
                id = 19,
                name = "Queen Books",
                address = "914 Queen St E, Toronto, ON",
                latitude = 43.66071,
                longitude = -79.34221,
                clue = "Find the independent bookstore in Toronto's east end.",
                nextClue = "Continue farther east toward the Beaches for your final business destination."
            ),

            /*
             * Business Stop 20
             */
            TreasureLocation(
                id = 20,
                name = "Book City - The Beach",
                address = "1950 Queen St E, Toronto, ON",
                latitude = 43.66930,
                longitude = -79.30100,
                clue = "Find the neighbourhood bookstore near Toronto's Beaches.",
                nextClue = "You have discovered the final treasure!"
            )
        )
    }
}