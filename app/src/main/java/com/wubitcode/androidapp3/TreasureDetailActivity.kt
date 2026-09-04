package com.wubitcode.androidapp3

import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.wubitcode.androidapp3.data.TreasureDatabase
import com.wubitcode.androidapp3.data.TreasureRepository
import com.wubitcode.androidapp3.model.TreasureLocation
import kotlinx.coroutines.launch
import java.io.File

/**
 * Displays detailed information about a selected Treasure Hunt destination.
 *
 * The activity:
 * - Receives the selected treasure ID from MainActivity.
 * - Retrieves authoritative treasure information from Room.
 * - Displays the treasure name, address, clue, and completion status.
 * - Displays a previously captured treasure photo when available.
 * - Launches the device camera to capture a full-size treasure photo.
 * - Stores the resulting local photo path persistently in Room.
 *
 * Photo files are stored inside the application's own external Pictures
 * directory and are shared with the camera application securely through
 * Android FileProvider.
 */
class TreasureDetailActivity : AppCompatActivity() {

    companion object {

        /**
         * Intent key used by MainActivity when opening this detail screen.
         */
        const val EXTRA_TREASURE_ID = "extra_treasure_id"

        /**
         * Bundle key used to preserve a pending camera photo path if Android
         * recreates this Activity while the camera application is open.
         */
        private const val STATE_PENDING_PHOTO_PATH =
            "state_pending_photo_path"
    }

    /**
     * Unique ID of the treasure currently displayed by this Activity.
     */
    private var treasureId: Int = -1

    /**
     * Holds the treasure record currently loaded from Room.
     */
    private var currentTreasure: TreasureLocation? = null

    /**
     * Stores the file path prepared for the next camera photo.
     *
     * The path is saved before the external camera application opens so the
     * result can be associated with the correct treasure when the camera
     * returns successfully.
     */
    private var pendingPhotoPath: String? = null

    /**
     * ImageView used to display the treasure photo.
     */
    private lateinit var treasurePhotoImageView: ImageView

    /**
     * Launches the device camera and writes a full-size image to the
     * FileProvider URI supplied by this Activity.
     *
     * The result is true only when the camera successfully saves the photo.
     */
    private val takePictureLauncher =
        registerForActivityResult(
            ActivityResultContracts.TakePicture()
        ) { photoSaved ->

            if (photoSaved) {

                saveCapturedPhoto()

            } else {

                /*
                 * Removes an unused empty file when the participant cancels
                 * the camera operation before a photo is captured.
                 */
                deleteUnusedPendingPhoto()

                Toast.makeText(
                    this,
                    "Photo capture cancelled.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    /**
     * Initializes the detail screen and loads the selected treasure from Room.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_treasure_detail)

        /*
         * Restores the pending image path when Android recreates the Activity,
         * for example while the external camera application is being used.
         */
        pendingPhotoPath =
            savedInstanceState?.getString(
                STATE_PENDING_PHOTO_PATH
            )

        /*
         * Reads the treasure ID supplied by MainActivity.
         *
         * A value of -1 means the Activity was opened without a valid
         * Treasure Hunt destination and therefore cannot continue safely.
         */
        treasureId =
            intent.getIntExtra(
                EXTRA_TREASURE_ID,
                -1
            )

        if (treasureId == -1) {

            Toast.makeText(
                this,
                "Treasure information could not be loaded.",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        /*
         * Connects the photo and navigation controls defined in the
         * activity_treasure_detail.xml layout.
         */
        treasurePhotoImageView =
            findViewById(
                R.id.treasurePhotoImageView
            )

        val takePhotoButton: Button =
            findViewById(
                R.id.takePhotoButton
            )

        val backButton: Button =
            findViewById(
                R.id.detailBackButton
            )

        /*
         * Opens the device camera and prepares a secure image destination.
         */
        takePhotoButton.setOnClickListener {

            launchTreasureCamera()
        }

        /*
         * Returns to the previous Treasure Hunt screen.
         */
        backButton.setOnClickListener {

            finish()
        }

        /*
         * Room access is asynchronous so database operations do not block
         * Android's main user-interface thread.
         */
        lifecycleScope.launch {

            loadTreasureDetails()
        }
    }

    /**
     * Retrieves the selected treasure from Room and displays its information.
     */
    private suspend fun loadTreasureDetails() {

        val treasureDao =
            TreasureDatabase
                .getDatabase(applicationContext)
                .treasureDao()

        val treasure =
            treasureDao.getTreasureById(
                treasureId
            )

        if (treasure == null) {

            Toast.makeText(
                this,
                "Treasure information could not be found.",
                Toast.LENGTH_SHORT
            ).show()

            finish()

            return
        }

        currentTreasure = treasure

        /*
         * Connects the informational TextViews after a valid database record
         * has been retrieved.
         */
        val nameTextView: TextView =
            findViewById(
                R.id.treasureNameTextView
            )

        val addressTextView: TextView =
            findViewById(
                R.id.treasureAddressTextView
            )

        val clueTextView: TextView =
            findViewById(
                R.id.treasureClueTextView
            )

        val statusTextView: TextView =
            findViewById(
                R.id.treasureStatusTextView
            )

        nameTextView.text =
            treasure.name

        addressTextView.text =
            treasure.address

        clueTextView.text =
            treasure.clue

        statusTextView.text =
            if (treasure.isVisited) {

                "Status: Completed"

            } else {

                "Status: Not Completed"
            }

        /*
         * Displays the previously stored treasure photo when one exists.
         */
        displayTreasurePhoto(
            treasure.photoPath
        )
    }

    /**
     * Creates an application-specific image file and launches the camera.
     *
     * The output file is stored under:
     *
     * Android/data/<package>/files/Pictures/treasure_photos/
     *
     * FileProvider converts the local file into a temporary content URI that
     * can safely be shared with the external camera application.
     */
    private fun launchTreasureCamera() {

        val photoDirectory =
            File(
                getExternalFilesDir(
                    Environment.DIRECTORY_PICTURES
                ),
                "treasure_photos"
            )

        /*
         * Ensures the treasure photo directory exists before creating the
         * image file.
         */
        if (!photoDirectory.exists()) {

            photoDirectory.mkdirs()
        }

        /*
         * Uses the treasure ID and current time to create a unique filename.
         * This prevents a newly captured image from unintentionally replacing
         * another treasure's photograph.
         */
        val photoFile =
            File(
                photoDirectory,
                "treasure_${treasureId}_${System.currentTimeMillis()}.jpg"
            )

        pendingPhotoPath =
            photoFile.absolutePath

        /*
         * Generates a secure content URI using the FileProvider authority
         * declared in AndroidManifest.xml.
         */
        val photoUri: Uri =
            FileProvider.getUriForFile(
                this,
                "$packageName.fileprovider",
                photoFile
            )

        takePictureLauncher.launch(
            photoUri
        )
    }

    /**
     * Persists the newly captured treasure photo path in Room.
     *
     * After the database update succeeds, the photo is immediately displayed
     * on the detail screen and the in-memory treasure object is synchronized.
     */
    private fun saveCapturedPhoto() {

        val photoPath =
            pendingPhotoPath ?: return

        lifecycleScope.launch {

            val treasureDao =
                TreasureDatabase
                    .getDatabase(applicationContext)
                    .treasureDao()

            TreasureRepository.updateTreasurePhoto(
                treasureDao = treasureDao,
                treasureId = treasureId,
                photoPath = photoPath
            )

            /*
             * Synchronizes the currently displayed object with the value that
             * was just persisted in Room.
             */
            currentTreasure?.photoPath =
                photoPath

            displayTreasurePhoto(
                photoPath
            )

            pendingPhotoPath = null

            Toast.makeText(
                this@TreasureDetailActivity,
                "Treasure photo saved.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Displays a locally stored treasure image when a valid file exists.
     *
     * When no photo has been captured, the ImageView remains available as a
     * neutral placeholder for a future treasure photograph.
     */
    private fun displayTreasurePhoto(
        photoPath: String?
    ) {

        if (photoPath.isNullOrBlank()) {

            treasurePhotoImageView.setImageDrawable(
                null
            )

            return
        }

        val photoFile =
            File(photoPath)

        if (photoFile.exists()) {

            treasurePhotoImageView.setImageURI(
                Uri.fromFile(photoFile)
            )

        } else {

            /*
             * Handles the uncommon case where Room contains a photo path but
             * the corresponding physical file is no longer available.
             */
            treasurePhotoImageView.setImageDrawable(
                null
            )
        }
    }

    /**
     * Deletes a temporary photo file when camera capture is cancelled.
     *
     * This prevents unused zero-byte image files from accumulating inside
     * the application's private photo directory.
     */
    private fun deleteUnusedPendingPhoto() {

        pendingPhotoPath?.let { photoPath ->

            val photoFile =
                File(photoPath)

            if (photoFile.exists()) {

                photoFile.delete()
            }
        }

        pendingPhotoPath = null
    }

    /**
     * Preserves the pending photo path if Android recreates the Activity while
     * the external camera application is active.
     */
    override fun onSaveInstanceState(
        outState: Bundle
    ) {

        super.onSaveInstanceState(outState)

        pendingPhotoPath?.let { photoPath ->

            outState.putString(
                STATE_PENDING_PHOTO_PATH,
                photoPath
            )
        }
    }
}