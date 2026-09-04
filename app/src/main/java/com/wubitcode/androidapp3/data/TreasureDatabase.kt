package com.wubitcode.androidapp3.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.wubitcode.androidapp3.model.TreasureLocation

/**
 * Room database used by the Toronto Treasure Hunt application.
 *
 * The database stores all treasure destinations together with their
 * persistent completion state and optional treasure photo information.
 *
 * Database version history:
 * - Version 1: Original Treasure Hunt progress storage.
 * - Version 2: Adds the optional photoPath column.
 */
@Database(
    entities = [TreasureLocation::class],
    version = 2,
    exportSchema = false
)
abstract class TreasureDatabase : RoomDatabase() {

    /**
     * Provides access to database operations for TreasureLocation records.
     */
    abstract fun treasureDao(): TreasureDao

    companion object {

        /**
         * Holds the single Room database instance used by the application.
         *
         * Volatile ensures that all threads observe the most recently
         * initialized database instance.
         */
        @Volatile
        private var INSTANCE: TreasureDatabase? = null

        /**
         * Migrates the Treasure Hunt database from version 1 to version 2.
         *
         * The migration adds a nullable photoPath column without deleting
         * existing treasure records or participant completion progress.
         */
        private val MIGRATION_1_2: Migration =
            object : Migration(1, 2) {

                override fun migrate(
                    db: SupportSQLiteDatabase
                ) {

                    /*
                     * Adds optional photo storage support.
                     *
                     * Existing records receive NULL automatically, which
                     * preserves all previously stored Treasure Hunt data.
                     */
                    /*
 * Adds a nullable column for storing an optional local treasure photo path.
 *
 * No explicit SQL default is defined because the corresponding Room entity
 * property is nullable and does not declare a database-level default value.
 * Existing treasure records therefore receive NULL naturally while their
 * previously stored completion progress remains unchanged.
 */
                    db.execSQL(
                        """
    ALTER TABLE treasure_locations
    ADD COLUMN photoPath TEXT
    """.trimIndent()
                    )
                }
            }

        /**
         * Returns the singleton TreasureDatabase instance.
         *
         * The application context is used so the database does not retain
         * an Activity context beyond the Activity lifecycle.
         *
         * Synchronization prevents multiple database instances from being
         * created when the method is called from different threads.
         */
        fun getDatabase(
            context: Context
        ): TreasureDatabase {

            return INSTANCE ?: synchronized(this) {

                val instance: TreasureDatabase =
                    Room.databaseBuilder(
                        context.applicationContext,
                        TreasureDatabase::class.java,
                        "treasure_hunt_database"
                    )
                        /*
                         * Registers the version 1 → 2 migration so existing
                         * participant progress is upgraded instead of erased.
                         */
                        .addMigrations(MIGRATION_1_2)
                        .build()

                INSTANCE = instance

                instance
            }
        }
    }
}