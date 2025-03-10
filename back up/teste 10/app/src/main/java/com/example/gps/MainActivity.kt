package com.example.gps

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
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
    private lateinit var takePhotoButton: Button
    private lateinit var imageView: ImageView
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private var waypointCounter = 1
    private val waypointList = mutableListOf<Pair<Double, Double>>()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val CAMERA_REQUEST_CODE = 1002
    }

    private var listenerAdded = false // Flag to ensure listener is added only once
    private var isPhotoTaken = false // Flag to track if photo has been taken

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mapInitOptions = MapInitOptions(
            context = this,
            resourceOptions = ResourceOptions.Builder()
                .accessToken("sk.eyJ1IjoicnVpdWEiLCJhIjoiY200bXM4czU5MDBwZDJrcjJsZW9qNzVjOCJ9.TlDHWxGJe7rdI03udVud3w")
                .build()
        )

        mapView = MapView(this, mapInitOptions)
        setContentView(mapView)

        takePhotoButton = Button(this).apply {
            text = "Take Photo"
            setOnClickListener { takePhoto() }
        }

        imageView = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams(300, 300)
        }

        val buttonLayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = 16
            topMargin = 240
        }

        addContentView(takePhotoButton, buttonLayoutParams)
        addContentView(imageView, buttonLayoutParams)

        if (hasLocationPermission()) {
            initializeMap()
        } else {
            requestLocationPermission()
        }
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

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            CAMERA_REQUEST_CODE
        )
    }

    private fun createWaypoint(point: Point) {
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(point)
            .withIconSize(5.0)
            .withIconImage("marker-15") // Use default marker
            .withTextField(waypointCounter.toString()) // Add waypoint number as text
            .withTextSize(14.0) // Adjust text size
            .withTextColor("black") // Text color
            .withTextAnchor(TextAnchor.TOP) // Use TextAnchor enum for positioning text above the marker

        // Add the point annotation to the manager
        pointAnnotationManager.create(pointAnnotationOptions)

        // Add the point's latitude and longitude to the list
        waypointList.add(Pair(point.latitude(), point.longitude()))

        Toast.makeText(
            this,
            "Waypoint $waypointCounter added at: ${point.latitude()}, ${point.longitude()}",
            Toast.LENGTH_SHORT
        ).show()

        // Increment counter for the next waypoint
        waypointCounter++
    }

    private fun takePhoto() {
        if (!hasCameraPermission()) {
            requestCameraPermission()
            return
        }
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            startActivityForResult(intent, CAMERA_REQUEST_CODE)
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as? Bitmap
            imageBitmap?.let {
                imageView.setImageBitmap(it)
            }

            // Mark photo as taken to prevent duplicate waypoint creation
            isPhotoTaken = true
            createWaypointForPhoto() // Call this method to trigger waypoint creation
        }
    }

    private fun createWaypointForPhoto() {
        if (isPhotoTaken) {
            val locationComponentPlugin = mapView.location

            // Avoid re-adding the listener if it already exists
            if (!listenerAdded) {
                locationComponentPlugin.addOnIndicatorPositionChangedListener { point ->
                    // Ensure the waypoint is created only once
                    if (isPhotoTaken) {
                        val latitude = point.latitude()
                        val longitude = point.longitude()

                        // Create the waypoint
                        createWaypoint(Point.fromLngLat(longitude, latitude))

                        // Disable further waypoint creation after first one
                        isPhotoTaken = false

                        // Remove the listener to avoid further triggers
                        locationComponentPlugin.removeOnIndicatorPositionChangedListener {}

                        // Set the flag to prevent the listener from being added again
                        listenerAdded = true
                    }
                }
            }
        }
    }

    private fun initializeMap() {
        mapboxMap = mapView.getMapboxMap()
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
            val locationComponentPlugin = mapView.location
            locationComponentPlugin.updateSettings {
                enabled = true
                pulsingEnabled = true
            }
            pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
            locationComponentPlugin.addOnIndicatorPositionChangedListener { point ->
                // Obtain the current latitude and longitude
                val latitude = point.latitude()
                val longitude = point.longitude()

                // Log or use the latitude and longitude
                Toast.makeText(
                    this,
                    "Latitude: $latitude, Longitude: $longitude",
                    Toast.LENGTH_SHORT
                ).show()

                // Optionally, move the camera to the current location
                mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)
                        .build()
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            takePhoto()
        }
    }
}
