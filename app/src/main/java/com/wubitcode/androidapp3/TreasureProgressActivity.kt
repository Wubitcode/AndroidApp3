package com.wubitcode.androidapp3

import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.wubitcode.androidapp3.data.TreasureDatabase
import com.wubitcode.androidapp3.model.TreasureLocation
import kotlinx.coroutines.launch

/**
 * Displays the participant's progress through the Toronto Treasure Hunt.
 *
 * The screen reads Treasure Hunt information directly from the Room database
 * and categorizes each destination as:
 *
 * - Completed: a location that has already been visited.
 * - Current: the next location the participant must reach.
 * - Locked: a future location that has not yet been unlocked.
 *
 * Future destination names and clues remain hidden so the sequential nature
 * of the Treasure Hunt is preserved.
 */
class TreasureProgressActivity : AppCompatActivity() {

    /**
     * Displays the participant's numerical Treasure Hunt progress.
     */
    private lateinit var progressSummaryTextView: TextView

    /**
     * Container that holds the dynamically generated progress entries.
     */
    private lateinit var progressContainer: LinearLayout

    /**
     * Initializes the progress screen and loads the latest Treasure Hunt
     * completion state from the Room database.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_treasure_progress)

        /*
         * Connects the activity to the views defined in the progress layout.
         */
        progressSummaryTextView =
            findViewById(R.id.progressSummaryTextView)

        progressContainer =
            findViewById(R.id.progressContainer)

        val backButton: Button =
            findViewById(R.id.progressBackButton)

        /*
         * Finishing this activity returns the participant to the existing
         * Treasure Hunt map without creating another MainActivity instance.
         */
        backButton.setOnClickListener {
            finish()
        }

        /*
         * Room database operations are performed asynchronously so database
         * access does not block the Android user-interface thread.
         */
        lifecycleScope.launch {
            loadTreasureProgress()
        }
    }

    /**
     * Retrieves all Treasure Hunt destinations from Room and determines
     * which location is currently active.
     */
    private suspend fun loadTreasureProgress() {

        val treasureDao =
            TreasureDatabase
                .getDatabase(applicationContext)
                .treasureDao()

        val treasures =
            treasureDao.getAllTreasures()

        /*
         * Removes any previously displayed rows before rebuilding the screen.
         * This keeps the UI accurate if the activity is refreshed.
         */
        progressContainer.removeAllViews()

        if (treasures.isEmpty()) {

            progressSummaryTextView.text =
                "No Treasure Hunt progress is available."

            return
        }

        /*
         * Counts completed destinations using the Room-persisted isVisited
         * property rather than relying on temporary in-memory state.
         */
        val completedCount =
            treasures.count { treasure ->
                treasure.isVisited
            }

        progressSummaryTextView.text =
            "$completedCount of ${treasures.size} treasures completed"

        /*
         * The first unvisited treasure is the participant's currently
         * unlocked destination.
         *
         * A value of -1 means every Treasure Hunt location is complete.
         */
        val currentTreasureIndex =
            treasures.indexOfFirst { treasure ->
                !treasure.isVisited
            }

        treasures.forEachIndexed { index, treasure ->

            when {

                /*
                 * Completed destinations may safely display their real
                 * location information because they have already been found.
                 */
                treasure.isVisited -> {
                    addCompletedTreasureRow(
                        treasure = treasure,
                        stopNumber = index + 1
                    )
                }

                /*
                 * Only the first unvisited treasure is currently unlocked.
                 * Its real destination name is displayed to the participant.
                 */
                index == currentTreasureIndex -> {
                    addCurrentTreasureRow(
                        treasure = treasure,
                        stopNumber = index + 1
                    )
                }

                /*
                 * Future locations remain hidden so participants cannot skip
                 * ahead or discover upcoming Treasure Hunt destinations.
                 */
                else -> {
                    addLockedTreasureRow(
                        stopNumber = index + 1
                    )
                }
            }
        }
    }

    /**
     * Creates a row for a Treasure Hunt destination that has already
     * been successfully completed.
     *
     * @param treasure completed Treasure Hunt destination.
     * @param stopNumber human-readable stop number shown to the participant.
     */
    private fun addCompletedTreasureRow(
        treasure: TreasureLocation,
        stopNumber: Int
    ) {

        val row =
            createProgressRow()

        val titleTextView =
            createRowTitle(
                "Stop $stopNumber — ${treasure.name}"
            )

        val statusTextView =
            createRowStatus(
                "✓ Completed"
            )

        row.addView(titleTextView)
        row.addView(statusTextView)

        progressContainer.addView(row)
    }

    /**
     * Creates the row representing the participant's currently active
     * Treasure Hunt destination.
     *
     * @param treasure currently unlocked Treasure Hunt location.
     * @param stopNumber human-readable stop number shown to the participant.
     */
    private fun addCurrentTreasureRow(
        treasure: TreasureLocation,
        stopNumber: Int
    ) {

        val row =
            createProgressRow()

        val titleTextView =
            createRowTitle(
                "Stop $stopNumber — ${treasure.name}"
            )

        val statusTextView =
            createRowStatus(
                "● Current Treasure"
            )

        /*
         * The clue is shown only for the active destination. Future treasure
         * clues remain hidden until those locations are unlocked.
         */
        val clueTextView =
            TextView(this).apply {

                text = treasure.clue

                textSize = 15f

                setPadding(
                    0,
                    dpToPixels(6),
                    0,
                    0
                )
            }

        row.addView(titleTextView)
        row.addView(statusTextView)
        row.addView(clueTextView)

        progressContainer.addView(row)
    }

    /**
     * Creates a placeholder row for a Treasure Hunt location that has
     * not yet been unlocked.
     *
     * No real business name, address, coordinates, or clue is exposed.
     *
     * @param stopNumber human-readable stop number shown to the participant.
     */
    private fun addLockedTreasureRow(
        stopNumber: Int
    ) {

        val row =
            createProgressRow()

        val titleTextView =
            createRowTitle(
                "Stop $stopNumber — Locked Treasure"
            )

        val statusTextView =
            createRowStatus(
                "🔒 Not Yet Unlocked"
            )

        row.addView(titleTextView)
        row.addView(statusTextView)

        progressContainer.addView(row)
    }

    /**
     * Creates the reusable vertical container used for each progress entry.
     *
     * Layout parameters and spacing are applied programmatically because the
     * individual Treasure Hunt rows are created dynamically at runtime.
     */
    private fun createProgressRow(): LinearLayout {

        return LinearLayout(this).apply {

            orientation = LinearLayout.VERTICAL

            setPadding(
                dpToPixels(16),
                dpToPixels(14),
                dpToPixels(16),
                dpToPixels(14)
            )

            layoutParams =
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {

                    /*
                     * Adds consistent vertical spacing between progress rows.
                     */
                    bottomMargin = dpToPixels(12)
                }
        }
    }

    /**
     * Creates the primary title used by each Treasure Hunt progress row.
     *
     * @param title text describing the Treasure Hunt stop.
     */
    private fun createRowTitle(
        title: String
    ): TextView {

        return TextView(this).apply {

            text = title

            textSize = 18f

            setTypeface(
                typeface,
                android.graphics.Typeface.BOLD
            )
        }
    }

    /**
     * Creates the secondary status label displayed beneath each row title.
     *
     * @param status completion, current, or locked status text.
     */
    private fun createRowStatus(
        status: String
    ): TextView {

        return TextView(this).apply {

            text = status

            textSize = 15f

            setPadding(
                0,
                dpToPixels(6),
                0,
                0
            )
        }
    }

    /**
     * Converts density-independent pixels into physical screen pixels.
     *
     * Using dp values keeps dynamically generated spacing visually
     * consistent across Android devices with different screen densities.
     *
     * @param dp density-independent pixel value.
     * @return equivalent pixel value for the current device.
     */
    private fun dpToPixels(
        dp: Int
    ): Int {

        return (
                dp *
                        resources.displayMetrics.density
                ).toInt()
    }
}