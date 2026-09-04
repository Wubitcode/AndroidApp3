package com.wubitcode.androidapp3

import android.Manifest
import android.content.Context
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
 * This activity coordinates the MapLibre map, Android location services,
 * treasure progression, Room database persistence, and participant feedback.
 *
 * Assignment 6 improves the original Treasure Hunt by storing completion
 * progress in Room so visited locations remain completed after the
 * application is closed and reopened.
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
     * This reference allows treasure and participant markers to be updated
     * after the map finishes loading.
     */
    private var mapLibreMap: MapLibreMap? = null

    /**
     * Marker representing the participant's most recently detected location.
     */
    private var userLocationMarker: Marker? = null

    /**
     * Marker representing the currently active treasure location.
     *
     * Only one treasure marker is displayed at a time so future locations
     * remain hidden until the participant completes the current destination.
     */
    private var activeTreasureMarker: Marker? = null

    /**
     * Room DAO used to read and update Treasure Hunt progress.
     */
    private lateinit var treasureDao: TreasureDao

    /**
     * Holds the predefined Toronto Treasure Hunt locations in hunt order.
     *
     * Room stores the persistent completion state while this list provides
     * the in-memory objects used by the map and location workflow.
     */
    private val treasureLocations: MutableList<TreasureLocation> by lazy {
        TreasureRepository.getTreasureLocations().toMutableList()
    }

    /**
     * Index of the treasure the participant must currently reach.
     *
     * This value is reconstructed from Room when the application starts.
     */
    private var currentTreasureIndex = 0

    /**
     * Handles the result of Android's runtime location-permission request.
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
     * MapLibre map, and location-check workflow.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * Initializes MapLibre before the MapView is accessed.
         */
        MapLibre.getInstance(this)

        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)

        /*
         * Forwards the saved Android state to MapView so map state can be
         * restored after activity recreation.
         */
        mapView.onCreate(savedInstanceState)

        /*
         * Obtains the Room DAO from the application's singleton database.
         */
        treasureDao =
            TreasureDatabase
                .getDatabase(applicationContext)
                .treasureDao()

        val checkLocationButton: Button =
            findViewById(R.id.checkLocationButton)

        /*
         * Requests a fresh geographic position whenever the participant
         * chooses to verify the active treasure location.
         */
        checkLocationButton.setOnClickListener {
            checkLocationPermission()
        }

        /*
         * Room operations are performed asynchronously using lifecycleScope.
         *
         * The map is initialized only after saved treasure progress has been
         * restored so the correct active destination appears immediately.
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
         * Creates a lookup table so Room's persisted visited states can
         * efficiently be applied to the in-memory treasure objects.
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
         * If every destination has been visited, the index is placed after
         * the final list item to represent a completed Treasure Hunt.
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
     * Initializes the MapLibre map after Room progress has been restored.
     */
    private fun initializeMap() {

        mapView.getMapAsync { map ->

            mapLibreMap = map

            /*
             * Uses OpenFreeMap to provide detailed Toronto street mapping
             * without requiring Google Maps billing credentials.
             */
            map.setStyle(
                "https://tiles.openfreemap.org/styles/liberty"
            ) {

                if (treasureLocations.isEmpty()) {

                    Toast.makeText(
                        this,
                        "No treasure locations are currently available.",
                        Toast.LENGTH_LONG
                    ).show()

                } else if (currentTreasureIndex >= treasureLocations.size) {

                    showTreasureHuntComplete()

                } else {

                    displayCurrentTreasureMarker(map)

                    positionCameraAtCurrentTreasure(map)

                    checkLocationPermission()
                }
            }
        }
    }

    /**
     * Displays only the currently active treasure marker.
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
     * Android's LocationManager.
     *
     * GPS is preferred when precise permission is available. Network
     * positioning is used as a fallback.
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
         * permission is available.
         */
        val provider = when {

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
         * selects the correct LocationManagerCompat overload.
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
     * Displays the participant's most recent position on the map.
     */
    private fun displayCurrentLocation(location: Location) {

        val currentPosition =
            LatLng(
                location.latitude,
                location.longitude
            )

        /*
         * Prevents duplicate participant markers after repeated
         * Check My Location requests.
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
     * The treasure is considered completed when the participant is within
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
     * Setting the in-memory state immediately prevents repeated location
     * checks from completing the same treasure while the database update
     * is being processed.
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
     * Advances the Treasure Hunt after a location has been persisted in Room.
     *
     * The completed location's next clue is displayed before the newly
     * unlocked marker replaces the previous treasure marker.
     */
    private fun advanceToNextTreasure(
        completedTreasure: TreasureLocation
    ) {

        currentTreasureIndex++

        if (currentTreasureIndex >= treasureLocations.size) {

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
     * Forwards the Android activity start event to MapView.
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
     * Saves MapView state if Android recreates the activity.
     */
    override fun onSaveInstanceState(
        outState: Bundle
    ) {
        super.onSaveInstanceState(outState)

        mapView.onSaveInstanceState(outState)
    }
}