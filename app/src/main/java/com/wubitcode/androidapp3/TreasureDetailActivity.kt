package com.wubitcode.androidapp3

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.wubitcode.androidapp3.data.TreasureDatabase
import kotlinx.coroutines.launch

/**
 * Displays detailed information about a selected treasure location.
 *
 * The activity receives a treasure ID through the launching Intent,
 * retrieves the matching treasure from the Room database, and displays
 * its name, address, clue, and completion status.
 */
class TreasureDetailActivity : AppCompatActivity() {

    companion object {
        /**
         * Intent key used to identify which treasure should be displayed.
         */
        const val EXTRA_TREASURE_ID = "extra_treasure_id"
    }

    /**
     * Initializes the detail screen and loads the selected treasure
     * from the local Room database.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_treasure_detail)

        // Obtain the treasure ID supplied by the activity that opened this screen.
        val treasureId = intent.getIntExtra(EXTRA_TREASURE_ID, -1)

        // Prevent an invalid Intent from attempting to query the database.
        if (treasureId == -1) {
            finish()
            return
        }

        // Access the application's Room database and Treasure DAO.
        val treasureDao =
            TreasureDatabase
                .getDatabase(applicationContext)
                .treasureDao()

        /*
         * Room database operations are performed inside lifecycleScope so
         * they run asynchronously and automatically stop when this activity
         * is destroyed.
         */
        lifecycleScope.launch {

            val treasure = treasureDao.getTreasureById(treasureId)

            /*
             * If the treasure cannot be found, close the screen rather than
             * displaying incomplete or misleading information.
             */
            if (treasure == null) {
                finish()
                return@launch
            }

            // Connect the layout views to the selected treasure information.
            val nameTextView =
                findViewById<TextView>(R.id.treasureNameTextView)

            val addressTextView =
                findViewById<TextView>(R.id.treasureAddressTextView)

            val clueTextView =
                findViewById<TextView>(R.id.treasureClueTextView)

            val statusTextView =
                findViewById<TextView>(R.id.treasureStatusTextView)

            // Populate the detail screen with values retrieved from Room.
            nameTextView.text = treasure.name
            addressTextView.text = treasure.address
            clueTextView.text = treasure.clue

            statusTextView.text =
                if (treasure.isVisited) {
                    "Status: Completed"
                } else {
                    "Status: Not Completed"
                }
        }
    }
}