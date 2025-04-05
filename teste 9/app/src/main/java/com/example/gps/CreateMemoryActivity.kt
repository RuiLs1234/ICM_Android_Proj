package com.example.gps

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mapbox.geojson.Point
import com.mapbox.maps.*
import com.mapbox.maps.plugin.locationcomponent.location

class CreateMemoryActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var gpsTextView: TextView
    private lateinit var takeSelfieButton: Button
    private lateinit var saveMemoryButton: Button
    private lateinit var selfieImageView: ImageView
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    private var currentPhoto: Bitmap? = null
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 2001
        private const val TAG = "CreateMemoryActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a root FrameLayout to hold the map and overlay UI.
        val rootLayout = FrameLayout(this)

        // Initialize MapView (full-screen background)
        val mapInitOptions = MapInitOptions(
            context = this,
            resourceOptions = ResourceOptions.Builder()
                .accessToken("sk.eyJ1IjoicnVpdWEiLCJhIjoiY200bXM4czU5MDBwZDJrcjJsZW9qNzVjOCJ9.TlDHWxGJe7rdI03udVud3w")
                .build()
        )
        mapView = MapView(this, mapInitOptions)
        rootLayout.addView(
            mapView,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        )

        // Create an overlay LinearLayout for UI elements (positioned at the bottom)
        val overlayLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        val overlayParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        // GPS TextView
        gpsTextView = TextView(this).apply {
            text = "GPS: Unknown"
            textSize = 18f
        }
        overlayLayout.addView(gpsTextView)

        // Button to take a selfie
        takeSelfieButton = Button(this).apply {
            text = "Take Selfie"
            setOnClickListener { openCamera() }
        }
        overlayLayout.addView(takeSelfieButton)

        // ImageView to display the captured selfie
        selfieImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 400
            )
        }
        overlayLayout.addView(selfieImageView)

        // Button to save the memory
        saveMemoryButton = Button(this).apply {
            text = "Save Memory"
            setOnClickListener { saveMemory() }
        }
        overlayLayout.addView(saveMemoryButton)

        // Add overlay layout to the root layout
        rootLayout.addView(overlayLayout, overlayParams)
        setContentView(rootLayout)

        // Register the camera launcher using the new ActivityResult API
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras = result.data?.extras
                if (extras != null) {
                    val imageBitmap = extras.get("data") as? Bitmap
                    if (imageBitmap != null) {
                        currentPhoto = imageBitmap
                        selfieImageView.setImageBitmap(imageBitmap)
                        Toast.makeText(this, "Selfie captured", Toast.LENGTH_SHORT).show()
                    } else {
                        Log.e(TAG, "No bitmap in extras")
                        Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Request location and camera permissions if needed
        if (hasLocationPermission()) {
            initializeMap()
        } else {
            requestLocationPermission()
        }
        if (!hasCameraPermission()) {
            requestCameraPermission()
        }
    }

    // Check for location permission
    private fun hasLocationPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // Request location permission
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_CODE
        )
    }

    // Check for camera permission
    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    // Request camera permission
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }

    // Initialize MapView and update GPS information using Mapbox's location component.
    private fun initializeMap() {
        mapboxMap = mapView.getMapboxMap()
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
            val locationComponent = mapView.location
            locationComponent.updateSettings {
                enabled = true
                pulsingEnabled = true
            }
            locationComponent.addOnIndicatorPositionChangedListener { point ->
                currentLatitude = point.latitude()
                currentLongitude = point.longitude()
                gpsTextView.text = "GPS: $currentLatitude, $currentLongitude"
                mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)
                        .build()
                )
            }
        }
    }

    // Open the camera using a simple intent (which returns a thumbnail in extras)
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            cameraLauncher.launch(intent)
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show()
        }
    }

    // Save memory (for now, simply display a Toast with the data)
    private fun saveMemory() {
        if (currentLatitude != null && currentLongitude != null && currentPhoto != null) {
            Toast.makeText(
                this,
                "Memory saved:\nGPS: ($currentLatitude, $currentLongitude)",
                Toast.LENGTH_LONG
            ).show()
        } else {
            Toast.makeText(this, "Missing data. Ensure you have a location and a selfie.", Toast.LENGTH_SHORT).show()
        }
    }

    // Handle permission request results
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


