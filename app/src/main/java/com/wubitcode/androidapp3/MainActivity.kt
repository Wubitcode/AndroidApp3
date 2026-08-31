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
import com.wubitcode.androidapp3.data.TreasureRepository
import com.wubitcode.androidapp3.model.TreasureLocation
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
 * This activity manages the primary treasure-hunt workflow. It displays
 * the currently active treasure on a MapLibre map, requests the participant's
 * geographic position, calculates the distance to the active destination,
 * records completed treasures, and advances the participant through the hunt.
 *
 * Game progress is stored locally so participants can close and reopen the
 * application without losing their current treasure location.
 */
class MainActivity : AppCompatActivity() {

    companion object {

        /**
         * Maximum distance, in metres, that a participant may be from the
         * active treasure for the destination to be considered reached.
         */
        private const val TREASURE_REACHED_RADIUS_METRES = 75f

        /**
         * Name of the SharedPreferences file used to store treasure-hunt
         * progress between application sessions.
         */
        private const val PREFS_NAME = "treasure_hunt_progress"

        /**
         * Preference key used to save the index of the next active treasure.
         */
        private const val KEY_CURRENT_TREASURE_INDEX =
            "current_treasure_index"
    }

    /**
     * MapView responsible for rendering and interacting with the
     * Toronto Treasure Hunt map.
     */
    private lateinit var mapView: MapView

    /**
     * Reference to the active MapLibre map.
     *
     * This allows location and treasure methods to update map content
     * after the map has completed initialization.
     */
    private var mapLibreMap: MapLibreMap? = null

    /**
     * Stores the marker representing the participant's latest geographic
     * position on the map.
     *
     * Keeping this reference allows the previous marker to be removed
     * before a new location is displayed.
     */
    private var userLocationMarker: Marker? = null

    /**
     * Stores the marker representing the currently active treasure.
     *
     * Only one destination is displayed at a time so future treasure
     * locations remain hidden until they are unlocked.
     */
    private var activeTreasureMarker: Marker? = null

    /**
     * Holds a mutable copy of the locations supplied by TreasureRepository.
     *
     * A mutable list is required because the visited state of each treasure
     * changes as the participant progresses through the hunt.
     */
    private val treasureLocations: MutableList<TreasureLocation> by lazy {
        TreasureRepository.getTreasureLocations().toMutableList()
    }

    /**
     * Identifies the treasure that the participant is currently expected
     * to reach.
     *
     * Index zero represents Toronto City Hall, the official starting point.
     */
    private var currentTreasureIndex = 0

    /**
     * Handles the result of Android's runtime location-permission request.
     *
     * Either approximate or precise location permission allows the app
     * to continue. Precise location is preferred because treasure completion
     * depends on accurate distance measurements.
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
     * Initializes MapLibre, restores saved treasure-hunt progress,
     * connects the map and location button, and displays the active treasure.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
         * Initializes the MapLibre mapping engine before the MapView is
         * created or accessed by the activity.
         */
        MapLibre.getInstance(this)

        setContentView(R.layout.activity_main)

        /*
         * Restores the participant's previously saved position in the
         * treasure-hunt sequence before the map is displayed.
         */
        restoreTreasureProgress()

        mapView = findViewById(R.id.mapView)

        /*
         * Connects the Check My Location button to the location verification
         * workflow. Participants can request a new GPS reading after travelling
         * to another destination without restarting the application.
         */
        val checkLocationButton: Button =
            findViewById(R.id.checkLocationButton)

        checkLocationButton.setOnClickListener {
            checkLocationPermission()
        }

        /*
         * Restores MapView state when Android recreates the activity after
         * events such as configuration changes.
         */
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { map ->

            mapLibreMap = map

            /*
             * Loads an OpenStreetMap-based map style through OpenFreeMap.
             *
             * This provides detailed Toronto mapping without requiring
             * Google Maps billing credentials or an API key.
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

                    /*
                     * If all locations were previously completed, preserve
                     * that completed state instead of restarting the hunt.
                     */
                    showTreasureHuntComplete()

                } else {

                    // Displays only the currently unlocked treasure.
                    displayCurrentTreasureMarker(map)

                    // Moves the map camera to the active search area.
                    positionCameraAtCurrentTreasure(map)

                    // Begins the location-permission workflow.
                    checkLocationPermission()
                }
            }
        }
    }

    /**
     * Displays only the treasure that the participant is currently expected
     * to reach.
     *
     * The previous treasure marker is removed before the newly unlocked
     * destination is displayed.
     */
    private fun displayCurrentTreasureMarker(map: MapLibreMap) {

        if (currentTreasureIndex >= treasureLocations.size) {
            return
        }

        val currentTreasure =
            treasureLocations[currentTreasureIndex]

        val treasurePosition = LatLng(
            currentTreasure.latitude,
            currentTreasure.longitude
        )

        // Prevents previously completed treasure markers from remaining visible.
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
     *
     * The method is reused each time a new location is unlocked so the
     * participant is automatically guided toward the next search area.
     */
    private fun positionCameraAtCurrentTreasure(map: MapLibreMap) {

        if (currentTreasureIndex >= treasureLocations.size) {
            return
        }

        val currentTreasure =
            treasureLocations[currentTreasureIndex]

        val treasurePosition = LatLng(
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
     * Determines whether the participant has already granted location access.
     *
     * If permission exists, a fresh location reading is requested immediately.
     * Otherwise, Android displays the runtime permission dialog.
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
     * Retrieves the participant's current geographic position using Android's
     * built-in LocationManager.
     *
     * GPS is preferred when precise location permission is available because
     * treasure verification depends on reasonably accurate coordinates.
     * Network-based positioning is used as a fallback.
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

        /*
         * Prevents location services from being accessed unless the user
         * has granted at least one supported location permission.
         */
        if (!fineLocationGranted && !coarseLocationGranted) {
            return
        }

        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        /*
         * Uses GPS only when precise permission has been granted.
         *
         * If only approximate permission is available, the network provider
         * is preferred to avoid requesting precision that the user did not grant.
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
                    "Please enable location services to continue the treasure hunt.",
                    Toast.LENGTH_LONG
                ).show()

                return
            }
        }

        /*
         * Explicitly declares the Android CancellationSignal type so Kotlin
         * can select the correct LocationManagerCompat.getCurrentLocation()
         * overload.
         */
        val cancellationSignal: android.os.CancellationSignal? = null

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
     * Displays the participant's most recent geographic position on the map.
     *
     * The previous location marker is removed first so repeated location
     * checks do not create duplicate participant markers.
     */
    private fun displayCurrentLocation(location: Location) {

        val currentPosition = LatLng(
            location.latitude,
            location.longitude
        )

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
     * Location.distanceBetween() returns the distance in metres. When the
     * participant is within the configured arrival radius, the treasure is
     * marked as completed and the hunt advances to the next destination.
     */
    private fun checkTreasureDistance(currentLocation: Location) {

        if (currentTreasureIndex >= treasureLocations.size) {
            showTreasureHuntComplete()
            return
        }

        val currentTreasure =
            treasureLocations[currentTreasureIndex]

        val distanceResults = FloatArray(1)

        Location.distanceBetween(
            currentLocation.latitude,
            currentLocation.longitude,
            currentTreasure.latitude,
            currentTreasure.longitude,
            distanceResults
        )

        val distanceToTreasure =
            distanceResults[0]

        if (distanceToTreasure <= TREASURE_REACHED_RADIUS_METRES) {

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
     * Marks the active treasure as visited and advances the participant
     * to the next destination.
     *
     * Progress is saved immediately so a completed treasure remains completed
     * even if the application closes before the participant reaches the next one.
     */
    private fun markTreasureReached(treasure: TreasureLocation) {

        // Prevents the same treasure from being completed multiple times.
        if (treasure.isVisited) {
            return
        }

        treasure.isVisited = true

        currentTreasureIndex++

        // Saves progress as soon as the active treasure is completed.
        saveTreasureProgress()

        if (currentTreasureIndex >= treasureLocations.size) {

            activeTreasureMarker?.remove()
            activeTreasureMarker = null

            showTreasureHuntComplete()

        } else {

            AlertDialog.Builder(this)
                .setTitle("Treasure Reached!")
                .setMessage(
                    """
                    You reached ${treasure.name}.

                    Next clue:
                    ${treasure.nextClue}
                    """.trimIndent()
                )
                .setPositiveButton("Continue") { dialog, _ ->

                    dialog.dismiss()

                    /*
                     * Replaces the completed treasure marker with the newly
                     * unlocked destination and moves the camera toward the
                     * next search area.
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
     * Saves the participant's current position in the treasure-hunt sequence.
     *
     * SharedPreferences provides lightweight local persistence suitable for
     * storing a small progress value such as the active treasure index.
     */
    private fun saveTreasureProgress() {

        getSharedPreferences(
            PREFS_NAME,
            MODE_PRIVATE
        )
            .edit()
            .putInt(
                KEY_CURRENT_TREASURE_INDEX,
                currentTreasureIndex
            )
            .apply()
    }

    /**
     * Restores the participant's saved progress from a previous app session.
     *
     * Any treasure before the restored index is marked as visited so the
     * in-memory data remains consistent with the stored game progress.
     */
    private fun restoreTreasureProgress() {

        val savedIndex =
            getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
            ).getInt(
                KEY_CURRENT_TREASURE_INDEX,
                0
            )

        currentTreasureIndex =
            savedIndex.coerceIn(
                0,
                treasureLocations.size
            )

        /*
         * Reconstructs the visited state of completed locations when the
         * application is reopened.
         */
        treasureLocations.forEachIndexed { index, treasure ->
            treasure.isVisited = index < currentTreasureIndex
        }
    }

    /**
     * Displays the final success message after all treasure locations
     * have been successfully visited.
     *
     * Completing all twenty business stops makes the participant eligible
     * to enter the anniversary vacation draw described in the assignment.
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
            .setPositiveButton("Finish") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Forwards the activity start event to MapView so its resources remain
     * synchronized with the Android activity lifecycle.
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
     * Saves MapView state so the map can be restored if Android recreates
     * the activity after a configuration change.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}