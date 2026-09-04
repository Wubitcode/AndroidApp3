package com.wubitcode.androidapp3.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.wubitcode.androidapp3.model.TreasureLocation

/**
 * Defines the Room database used by the Toronto Treasure Hunt application.
 *
 * The database stores TreasureLocation entities and provides access to
 * TreasureDao for inserting, retrieving, updating, and resetting treasure
 * progress.
 */
@Database(
    entities = [TreasureLocation::class],
    version = 1,
    exportSchema = false
)
abstract class TreasureDatabase : RoomDatabase() {

    /**
     * Provides access to all supported Treasure Hunt database operations.
     */
    abstract fun treasureDao(): TreasureDao

    companion object {

        /**
         * Holds the single database instance used throughout the application.
         *
         * Volatile ensures that changes to this reference are immediately
         * visible to all application threads.
         */
        @Volatile
        private var INSTANCE: TreasureDatabase? = null

        /**
         * Returns the application's shared Room database instance.
         *
         * A singleton database prevents multiple database instances from
         * being created unnecessarily and ensures consistent local data.
         */
        fun getDatabase(context: Context): TreasureDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance =
                    Room.databaseBuilder(
                        context.applicationContext,
                        TreasureDatabase::class.java,
                        "treasure_hunt_database"
                    ).build()

                INSTANCE = instance

                instance
            }
        }
    }
}