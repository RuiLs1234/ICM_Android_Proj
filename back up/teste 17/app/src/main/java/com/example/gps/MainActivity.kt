package com.example.gps

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ResourceOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var browseMemoriesButton: Button
    private lateinit var linkMemoriesButton: Button
    private lateinit var createMemoryButton: Button
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private var waypointCounter = 1
    private val waypointList = mutableListOf<Pair<Double, Double>>()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize MapView with MapInitOptions
        val mapInitOptions = MapInitOptions(
            context = this,
            resourceOptions = ResourceOptions.Builder()
                .accessToken("sk.eyJ1IjoicnVpdWEiLCJhIjoiY200bXM4czU5MDBwZDJrcjJsZW9qNzVjOCJ9.TlDHWxGJe7rdI03udVud3w")
                .build()
        )
        mapView = MapView(this, mapInitOptions)

        // Create a root FrameLayout container
        val rootLayout = FrameLayout(this)

        // Add MapView as the background
        rootLayout.addView(mapView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Create an overlay LinearLayout for buttons
        val overlayLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Set layout parameters for the overlay layout
        val overlayLayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        // Initialize the navigation buttons
        browseMemoriesButton = Button(this).apply {
            text = "Browse Memories"
            setOnClickListener { navigateToBrowseMemories() }
        }
        linkMemoriesButton = Button(this).apply {
            text = "Link Memories"
            setOnClickListener { navigateToLinkMemories() }
        }
        createMemoryButton = Button(this).apply {
            text = "Create Memory"
            setOnClickListener { navigateToCreateMemory() }
        }

        // Add the buttons to the overlay layout
        overlayLayout.addView(browseMemoriesButton)
        overlayLayout.addView(linkMemoriesButton)
        overlayLayout.addView(createMemoryButton)

        // Add the overlay layout to the root layout
        rootLayout.addView(overlayLayout, overlayLayoutParams)

        // Set the root layout as the content view
        setContentView(rootLayout)

        // Check for location permission and initialize the map
        if (hasLocationPermission()) {
            initializeMap()
        } else {
            requestLocationPermission()
        }
    }

    private fun navigateToBrowseMemories() {
        // Navigate to BrowseMemoriesActivity (ensure this activity exists)
        startActivity(Intent(this, BrowseMemoriesActivity::class.java))
    }

    private fun navigateToLinkMemories() {
        // Navigate to LinkMemoriesActivity (ensure this activity exists)
        //startActivity(Intent(this, LinkMemoriesActivity::class.java))
    }

    private fun navigateToCreateMemory() {
        // Navigate to CreateMemoryActivity (ensure this activity exists)
        startActivity(Intent(this, CreateMemoryActivity::class.java))
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun createWaypoint(point: Point) {
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconSize(5.0)
            .withIconImage("marker-15") // default marker image
            .withTextField(waypointCounter.toString())
            .withTextSize(14.0)
            .withTextColor("black")
            .withTextAnchor(TextAnchor.TOP)

        pointAnnotationManager.create(pointAnnotationOptions)
        waypointList.add(Pair(point.latitude(), point.longitude()))

        Toast.makeText(
            this,
            "Waypoint $waypointCounter added at: ${point.latitude()}, ${point.longitude()}",
            Toast.LENGTH_SHORT
        ).show()

        waypointCounter++
    }

    private fun initializeMap() {
        mapboxMap = mapView.getMapboxMap()
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
            // Enable the location component for current location updates
            val locationComponentPlugin = mapView.location
            locationComponentPlugin.updateSettings {
                enabled = true
                pulsingEnabled = true
            }
            // Create the annotation manager for adding waypoints
            pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
            // Listen to location changes and update the camera; also show current coordinates
            locationComponentPlugin.addOnIndicatorPositionChangedListener { point ->
                val latitude = point.latitude()
                val longitude = point.longitude()
                Toast.makeText(
                    this,
                    "Latitude: $latitude, Longitude: $longitude",
                    Toast.LENGTH_SHORT
                ).show()
                mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)
                        .build()
                )
                // Optionally, you can call createWaypoint(point) here if you want to add a waypoint automatically.
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeMap()
            } else {
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
