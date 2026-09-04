package com.wubitcode.androidapp3

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.lifecycleScope
import com.wubitcode.androidapp3.data.TreasureDao
import com.wubitcode.androidapp3.data.TreasureDatabase
import com.wubitcode.androidapp3.data.TreasureRepository
import com.wubitcode.androidapp3.model.TreasureLocation
import kotlinx.coroutines.launch
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.Marker
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

/**
 * Main activity for the Toronto Treasure Hunt application.
 *
 * Responsibilities:
 * - Displays the Treasure Hunt using MapLibre and OpenFreeMap.
 * - Retrieves the participant's current Android device location.
 * - Verifies whether the participant is close enough to the active treasure.
 * - Stores completed treasure locations permanently using Room.
 * - Restores Treasure Hunt progress when the application is reopened.
 * - Displays only the currently unlocked treasure destination.
 * - Opens a detailed information screen when the active treasure marker
 *   is selected.
 * - Opens the Treasure Hunt Progress screen so participants can review
 *   completed, current, and locked destinations.
 *
 * Assignment 6 expands the original Treasure Hunt by introducing persistent
 * Room storage, detail navigation, progress tracking, and additional user
 * interface improvements while preserving the sequential treasure-unlocking
 * workflow created in Assignment 5.
 */
class MainActivity : AppCompatActivity() {

    companion object {

        /**
         * Maximum distance, in metres, that a participant may be from
         * the active treasure for the destination to be considered reached.
         */
        private const val TREASURE_REACHED_RADIUS_METRES = 75f
    }

    /**
     * MapView responsible for displaying the Toronto Treasure Hunt map.
     */
    private lateinit var mapView: MapView

    /**
     * Reference to the initialized MapLibre map.
     *
     * Keeping this reference allows the activity to update treasure and
     * participant markers after the map has finished loading.
     */
    private var mapLibreMap: MapLibreMap? = null

    /**
     * Marker representing the participant's most recently detected location.
     */
    private var userLocationMarker: Marker? = null

    /**
     * Marker representing the currently active treasure destination.
     *
     * Only one treasure marker is displayed at a time so future locations
     * remain hidden until the participant completes the current destination.
     */
    private var activeTreasureMarker: Marker? = null

    /**
     * Room DAO used to read and update persistent Treasure Hunt progress.
     */
    private lateinit var treasureDao: TreasureDao

    /**
     * Holds the predefined Toronto Treasure Hunt locations in hunt order.
     *
     * Room stores the persistent visited state while this mutable collection
     * provides the in-memory objects used by the map and location workflow.
     */
    private val treasureLocations: MutableList<TreasureLocation> by lazy {
        TreasureRepository.getTreasureLocations().toMutableList()
    }

    /**
     * Index of the treasure the participant must currently reach.
     *
     * The value is reconstructed from Room whenever the application starts.
     */
    private var currentTreasureIndex = 0

    /**
     * Handles the result of Android's runtime location-permission request.
     *
     * Either precise or approximate location permission is sufficient for
     * attempting a location check, although GPS is preferred when available.
     */
    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineLocationGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            val coarseLocationGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineLocationGranted || coarseLocationGranted) {

                showCurrentLocation()

            } else {

                Toast.makeText(
                    this,
                    "Location permission is required to verify treasure locations.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    /**
     * Initializes the user interface, Room database, Treasure Hunt progress,
     * MapLibre map, navigation controls, and location-check workflow.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * MapLibre must be initialized before the MapView is accessed.
         */
        MapLibre.getInstance(this)

        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)

        /*
         * Forwards the saved Android activity state to MapView so the map
         * can restore its internal state after activity recreation.
         */
        mapView.onCreate(savedInstanceState)

        /*
         * Obtains the Treasure DAO from the singleton Room database.
         */
        treasureDao =
            TreasureDatabase
                .getDatabase(applicationContext)
                .treasureDao()

        /*
         * Connects the Treasure Hunt controls defined in activity_main.xml
         * to their corresponding MainActivity behaviour.
         */
        val viewProgressButton: Button =
            findViewById(R.id.viewProgressButton)

        val checkLocationButton: Button =
            findViewById(R.id.checkLocationButton)

        /*
         * Opens the Treasure Hunt Progress screen.
         *
         * The progress activity reads completion information directly from
         * Room, ensuring the participant always sees the persisted state.
         */
        viewProgressButton.setOnClickListener {
            openTreasureProgress()
        }

        /*
         * Requests a fresh device position whenever the participant chooses
         * to verify whether they have reached the active treasure.
         */
        checkLocationButton.setOnClickListener {
            checkLocationPermission()
        }

        /*
         * Room operations are performed asynchronously using lifecycleScope.
         *
         * The map is initialized only after stored Treasure Hunt progress has
         * been restored so the correct active destination appears immediately.
         */
        lifecycleScope.launch {

            initializeTreasureProgress()

            initializeMap()
        }
    }

    /**
     * Initializes Room with the predefined treasure locations and restores
     * previously completed destinations.
     *
     * The next active treasure is determined by locating the first database
     * record that has not yet been visited.
     */
    private suspend fun initializeTreasureProgress() {

        TreasureRepository.initializeDatabase(treasureDao)

        val storedTreasures =
            TreasureRepository.getStoredTreasureLocations(treasureDao)

        /*
         * Creates a lookup table so Room's persisted visited state can be
         * efficiently applied to the in-memory treasure objects.
         */
        val storedTreasureMap =
            storedTreasures.associateBy { treasure ->
                treasure.id
            }

        treasureLocations.forEach { treasure ->

            treasure.isVisited =
                storedTreasureMap[treasure.id]?.isVisited ?: false
        }

        /*
         * Finds the first treasure that has not been completed.
         *
         * If every destination has already been visited, the index is placed
         * after the final list item to represent a completed Treasure Hunt.
         */
        val firstUnvisitedIndex =
            treasureLocations.indexOfFirst { treasure ->
                !treasure.isVisited
            }

        currentTreasureIndex =
            if (firstUnvisitedIndex == -1) {
                treasureLocations.size
            } else {
                firstUnvisitedIndex
            }
    }

    /**
     * Initializes the MapLibre map after persistent Room progress has
     * been restored.
     */
    private fun initializeMap() {

        mapView.getMapAsync { map ->

            mapLibreMap = map

            /*
             * Opens the detail screen when the participant taps the currently
             * active treasure marker.
             *
             * Returning true consumes the click so MapLibre does not also
             * perform its default marker-click behaviour for this marker.
             */
            map.setOnMarkerClickListener { marker ->

                if (marker == activeTreasureMarker) {

                    openCurrentTreasureDetails()

                    true

                } else {

                    /*
                     * Returning false allows MapLibre to process markers that
                     * are not the active treasure using its normal behaviour.
                     */
                    false
                }
            }

            /*
             * Uses OpenFreeMap to provide detailed Toronto street mapping
             * without requiring Google Maps billing credentials.
             */
            map.setStyle(
                "https://tiles.openfreemap.org/styles/liberty"
            ) {

                when {

                    treasureLocations.isEmpty() -> {

                        Toast.makeText(
                            this,
                            "No treasure locations are currently available.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    currentTreasureIndex >= treasureLocations.size -> {

                        showTreasureHuntComplete()
                    }

                    else -> {

                        displayCurrentTreasureMarker(map)

                        positionCameraAtCurrentTreasure(map)

                        checkLocationPermission()
                    }
                }
            }
        }
    }

    /**
     * Displays only the currently active treasure marker.
     *
     * Future treasures remain hidden until the participant successfully
     * completes the current destination.
     */
    private fun displayCurrentTreasureMarker(map: MapLibreMap) {

        if (currentTreasureIndex >= treasureLocations.size) {
            return
        }

        val currentTreasure =
            treasureLocations[currentTreasureIndex]

        val treasurePosition =
            LatLng(
                currentTreasure.latitude,
                currentTreasure.longitude
            )

        /*
         * Removes the previously active marker before displaying the newly
         * unlocked treasure destination.
         */
        activeTreasureMarker?.remove()

        activeTreasureMarker =
            map.addMarker(
                MarkerOptions()
                    .position(treasurePosition)
                    .title(currentTreasure.name)
                    .snippet(currentTreasure.clue)
            )
    }

    /**
     * Opens TreasureDetailActivity for the currently active destination.
     *
     * Only the treasure ID is transferred because TreasureDetailActivity
     * retrieves the authoritative treasure information directly from Room.
     * This avoids duplicating database information inside the Intent.
     */
    private fun openCurrentTreasureDetails() {

        /*
         * Prevents detail navigation when the Treasure Hunt is already
         * complete or when there is no valid active treasure.
         */
        if (currentTreasureIndex >= treasureLocations.size) {
            return
        }

        val currentTreasure =
            treasureLocations[currentTreasureIndex]

        /*
         * Creates an explicit Intent targeting TreasureDetailActivity and
         * supplies the active treasure's unique database identifier.
         */
        val detailIntent =
            Intent(
                this,
                TreasureDetailActivity::class.java
            ).apply {

                putExtra(
                    TreasureDetailActivity.EXTRA_TREASURE_ID,
                    currentTreasure.id
                )
            }

        startActivity(detailIntent)
    }

    /**
     * Opens the Treasure Hunt Progress screen.
     *
     * TreasureProgressActivity independently retrieves progress information
     * from Room, allowing it to display the latest completed, active, and
     * locked destinations without duplicating state from MainActivity.
     */
    private fun openTreasureProgress() {

        /*
         * Creates an explicit Intent for TreasureProgressActivity.
         *
         * No additional data is required because the progress screen reads
         * the authoritative Treasure Hunt state directly from Room.
         */
        val progressIntent =
            Intent(
                this,
                TreasureProgressActivity::class.java
            )

        startActivity(progressIntent)
    }

    /**
     * Moves the map camera to the currently active treasure destination.
     */
    private fun positionCameraAtCurrentTreasure(map: MapLibreMap) {

        if (currentTreasureIndex >= treasureLocations.size) {
            return
        }

        val currentTreasure =
            treasureLocations[currentTreasureIndex]

        val treasurePosition =
            LatLng(
                currentTreasure.latitude,
                currentTreasure.longitude
            )

        map.cameraPosition =
            CameraPosition.Builder()
                .target(treasurePosition)
                .zoom(15.0)
                .build()
    }

    /**
     * Determines whether location permission has already been granted.
     *
     * A fresh device position is requested immediately when permission
     * exists. Otherwise Android displays the runtime permission dialog.
     */
    private fun checkLocationPermission() {

        val fineLocationGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted || coarseLocationGranted) {

            showCurrentLocation()

        } else {

            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    /**
     * Retrieves the participant's current geographic position using
     * Android's built-in LocationManager.
     *
     * GPS is preferred when precise permission is available. Network
     * positioning is used as a fallback when GPS cannot be used.
     */
    private fun showCurrentLocation() {

        val fineLocationGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted =
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        if (!fineLocationGranted && !coarseLocationGranted) {
            return
        }

        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        /*
         * GPS is preferred for accurate treasure verification when precise
         * location permission is available.
         */
        val provider =
            when {

                fineLocationGranted &&
                        locationManager.isProviderEnabled(
                            LocationManager.GPS_PROVIDER
                        ) -> {

                    LocationManager.GPS_PROVIDER
                }

                locationManager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                ) -> {

                    LocationManager.NETWORK_PROVIDER
                }

                else -> {

                    Toast.makeText(
                        this,
                        "Please enable location services to continue the Treasure Hunt.",
                        Toast.LENGTH_LONG
                    ).show()

                    return
                }
            }

        /*
         * Explicitly declares the Android CancellationSignal type so Kotlin
         * selects the appropriate LocationManagerCompat overload.
         */
        val cancellationSignal:
                android.os.CancellationSignal? = null

        LocationManagerCompat.getCurrentLocation(
            locationManager,
            provider,
            cancellationSignal,
            ContextCompat.getMainExecutor(this)
        ) { location: Location? ->

            if (location != null) {

                displayCurrentLocation(location)

                checkTreasureDistance(location)

            } else {

                Toast.makeText(
                    this,
                    "Current location could not be determined.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Displays the participant's most recently detected geographic position
     * on the map.
     */
    private fun displayCurrentLocation(location: Location) {

        val currentPosition =
            LatLng(
                location.latitude,
                location.longitude
            )

        /*
         * Removes the previous participant marker so repeated location checks
         * do not create duplicate "Your Location" markers.
         */
        userLocationMarker?.remove()

        userLocationMarker =
            mapLibreMap?.addMarker(
                MarkerOptions()
                    .position(currentPosition)
                    .title("Your Location")
                    .snippet("Current Treasure Hunt Position")
            )
    }

    /**
     * Calculates the distance between the participant and the active treasure.
     *
     * The destination is considered completed when the participant is within
     * the configured 75-metre arrival radius.
     */
    private fun checkTreasureDistance(
        currentLocation: Location
    ) {

        if (currentTreasureIndex >= treasureLocations.size) {

            showTreasureHuntComplete()

            return
        }

        val currentTreasure =
            treasureLocations[currentTreasureIndex]

        val distanceResults =
            FloatArray(1)

        Location.distanceBetween(
            currentLocation.latitude,
            currentLocation.longitude,
            currentTreasure.latitude,
            currentTreasure.longitude,
            distanceResults
        )

        val distanceToTreasure =
            distanceResults[0]

        if (
            distanceToTreasure <=
            TREASURE_REACHED_RADIUS_METRES
        ) {

            markTreasureReached(currentTreasure)

        } else {

            Toast.makeText(
                this,
                "You are ${distanceToTreasure.toInt()} m from ${currentTreasure.name}.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Marks the active treasure as completed and persists that status in Room.
     *
     * Updating the in-memory value immediately prevents repeated location
     * checks from completing the same treasure while the asynchronous Room
     * database update is still being processed.
     */
    private fun markTreasureReached(
        treasure: TreasureLocation
    ) {

        if (treasure.isVisited) {
            return
        }

        treasure.isVisited = true

        /*
         * Saves the completed treasure to Room asynchronously.
         */
        lifecycleScope.launch {

            TreasureRepository.markTreasureVisited(
                treasureDao,
                treasure.id
            )

            advanceToNextTreasure(treasure)
        }
    }

    /**
     * Advances the Treasure Hunt after the completed location has been
     * persisted in Room.
     *
     * The completed location's next clue is displayed before the newly
     * unlocked treasure marker replaces the previous marker.
     */
    private fun advanceToNextTreasure(
        completedTreasure: TreasureLocation
    ) {

        currentTreasureIndex++

        if (currentTreasureIndex >= treasureLocations.size) {

            /*
             * Removes the final active marker after all destinations have
             * been successfully completed.
             */
            activeTreasureMarker?.remove()

            activeTreasureMarker = null

            showTreasureHuntComplete()

        } else {

            AlertDialog.Builder(this)
                .setTitle("Treasure Reached!")
                .setMessage(
                    """
                    You reached ${completedTreasure.name}.

                    Next clue:
                    ${completedTreasure.nextClue}
                    """.trimIndent()
                )
                .setPositiveButton(
                    "Continue"
                ) { dialog, _ ->

                    dialog.dismiss()

                    /*
                     * Replaces the completed treasure marker with the newly
                     * unlocked destination and centers the map on it.
                     */
                    mapLibreMap?.let { map ->

                        displayCurrentTreasureMarker(map)

                        positionCameraAtCurrentTreasure(map)
                    }
                }
                .show()
        }
    }

    /**
     * Displays the final success message after every required treasure
     * destination has been completed.
     */
    private fun showTreasureHuntComplete() {

        AlertDialog.Builder(this)
            .setTitle("Treasure Hunt Complete!")
            .setMessage(
                """
                Congratulations!

                You completed the Toronto Treasure Hunt and visited all
                required locations.

                You are now eligible to enter the draw for a free vacation.
                """.trimIndent()
            )
            .setPositiveButton(
                "Finish"
            ) { dialog, _ ->

                dialog.dismiss()
            }
            .show()
    }

    /**
     * Forwards the Android activity start lifecycle event to MapView.
     */
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    /**
     * Resumes MapView rendering when the activity becomes active.
     */
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    /**
     * Pauses MapView rendering when the activity is no longer active.
     */
    override fun onPause() {
        mapView.onPause()
        super.onPause()
    }

    /**
     * Stops MapView processing when the activity becomes invisible.
     */
    override fun onStop() {
        mapView.onStop()
        super.onStop()
    }

    /**
     * Releases MapView resources when the activity is destroyed.
     */
    override fun onDestroy() {
        mapView.onDestroy()
        super.onDestroy()
    }

    /**
     * Allows MapView to release unnecessary resources when Android reports
     * a low-memory condition.
     */
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    /**
     * Saves MapView state if Android recreates this activity.
     */
    override fun onSaveInstanceState(
        outState: Bundle
    ) {

        super.onSaveInstanceState(outState)

        mapView.onSaveInstanceState(outState)
    }
}