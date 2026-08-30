package com.wubitcode.androidapp3

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
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
 * The activity displays Toronto treasure locations on a MapLibre map,
 * requests the participant's current geographic position, and determines
 * whether the participant is close enough to complete the active treasure.
 */
class MainActivity : AppCompatActivity() {

    /**
     * Maximum distance, in metres, that a participant may be from a treasure
     * location for the application to consider that destination reached.
     */
    companion object {
        private const val TREASURE_REACHED_RADIUS_METRES = 75f
    }

    /**
     * MapView responsible for displaying and interacting with the
     * Toronto Treasure Hunt map.
     */
    private lateinit var mapView: MapView

    /**
     * Reference to the active MapLibre map.
     *
     * This allows location-related methods to update markers after the
     * map has completed initialization.
     */
    private var mapLibreMap: MapLibreMap? = null

    /**
     * Stores the marker representing the participant's current location.
     *
     * The existing marker is removed before a new position is displayed
     * so repeated location checks do not create duplicate markers.
     */
    private var userLocationMarker: Marker? = null

    /**
     * Holds the treasure locations for the current game session.
     *
     * A mutable copy is used so each location can be marked as visited
     * while the participant progresses through the hunt.
     */
    private val treasureLocations: MutableList<TreasureLocation> by lazy {
        TreasureRepository.getTreasureLocations().toMutableList()
    }

    /**
     * Identifies which treasure the participant is currently expected
     * to reach. The hunt begins with Toronto City Hall at index zero.
     */
    private var currentTreasureIndex = 0

    /**
     * Handles the result of the Android runtime location-permission request.
     *
     * Either approximate or precise permission allows location retrieval,
     * although precise access is preferred for distance-based verification.
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
                    "Location permission is needed to track treasure hunt progress.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    /**
     * Initializes MapLibre, displays treasure markers, positions the camera,
     * and begins the location-permission workflow.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initializes the MapLibre engine before accessing MapView.
        MapLibre.getInstance(this)

        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.mapView)

        // Restores saved MapView state when Android recreates the activity.
        mapView.onCreate(savedInstanceState)

        mapView.getMapAsync { map ->

            mapLibreMap = map

            /*
             * Loads a detailed OpenStreetMap-based style through OpenFreeMap.
             * This map source does not require a Google API key or billing.
             */
            map.setStyle(
                "https://tiles.openfreemap.org/styles/liberty"
            ) {

                displayTreasureMarkers(map)
                positionCameraAtStartingLocation(map)
                checkLocationPermission()
            }
        }
    }

    /**
     * Adds a marker for every treasure location currently available
     * in the repository.
     */
    private fun displayTreasureMarkers(map: MapLibreMap) {

        treasureLocations.forEach { treasure ->

            val treasurePosition = LatLng(
                treasure.latitude,
                treasure.longitude
            )

            map.addMarker(
                MarkerOptions()
                    .position(treasurePosition)
                    .title(treasure.name)
                    .snippet(treasure.address)
            )
        }
    }

    /**
     * Positions the camera over the first treasure-hunt destination,
     * which is Toronto City Hall.
     */
    private fun positionCameraAtStartingLocation(map: MapLibreMap) {

        val startingLocation = treasureLocations.firstOrNull() ?: return

        val startingPosition = LatLng(
            startingLocation.latitude,
            startingLocation.longitude
        )

        map.cameraPosition =
            CameraPosition.Builder()
                .target(startingPosition)
                .zoom(15.0)
                .build()
    }

    /**
     * Checks whether the participant has already granted location access.
     *
     * If permission is available, a location request is made immediately.
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
     * Retrieves the participant's current geographic position using
     * Android's built-in LocationManager.
     *
     * GPS is preferred because treasure completion depends on reasonably
     * accurate distance measurements. Network location is used as a fallback.
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

        // Location services must not be accessed without permission.
        if (!fineLocationGranted && !coarseLocationGranted) {
            return
        }

        val locationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager

        /*
         * Selects GPS when available because it normally provides better
         * accuracy for treasure verification.
         */
        val provider = when {

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
         * Explicitly identifies the Android platform CancellationSignal type
         * so Kotlin can resolve the correct getCurrentLocation() overload.
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

                // Compares the participant's position with the active treasure.
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
     * Displays the participant's latest geographic position on the map.
     */
    private fun displayCurrentLocation(location: Location) {

        val currentPosition = LatLng(
            location.latitude,
            location.longitude
        )

        // Removes the previous position marker before displaying an update.
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
     * Android's Location.distanceBetween() returns the distance in metres.
     * When the participant is within the configured arrival radius, the
     * treasure is marked as visited and the next clue is revealed.
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

        val distanceToTreasure = distanceResults[0]

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
     * Marks the current treasure as completed and displays the clue leading
     * to the participant's next destination.
     */
    private fun markTreasureReached(treasure: TreasureLocation) {

        // Prevents the same treasure from being completed more than once.
        if (treasure.isVisited) {
            return
        }

        treasure.isVisited = true

        currentTreasureIndex++

        if (currentTreasureIndex >= treasureLocations.size) {

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
                }
                .show()
        }
    }

    /**
     * Displays the final completion message after every treasure location
     * in the current hunt has been successfully visited.
     */
    private fun showTreasureHuntComplete() {

        AlertDialog.Builder(this)
            .setTitle("Treasure Hunt Complete!")
            .setMessage(
                "Congratulations! You completed all Toronto Treasure Hunt locations."
            )
            .setPositiveButton("Finish") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    /**
     * Forwards the activity start event to MapView.
     */
    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    /**
     * Resumes map rendering when the activity becomes active.
     */
    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    /**
     * Pauses map rendering when the activity is no longer active.
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
     * Allows MapView to release unnecessary resources when Android
     * reports a low-memory condition.
     */
    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    /**
     * Saves MapView state so the map can be restored when the
     * activity is recreated.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView.onSaveInstanceState(outState)
    }
}