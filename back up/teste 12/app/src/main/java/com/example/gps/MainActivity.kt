package com.example.gps

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
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
    private lateinit var takePhotoButton: Button
    private lateinit var browseMemoriesButton: Button
    private lateinit var linkMemoriesButton: Button
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

        // Add MapView as the background (match_parent)
        rootLayout.addView(mapView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Create an overlay LinearLayout for buttons and imageView
        val overlayLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
        }

        // Set layout params so the overlay fills the width
        val overlayLayoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }

        // Initialize buttons and imageView
        takePhotoButton = Button(this).apply {
            text = "Take Photo"
            setOnClickListener {
                Toast.makeText(context, "Take Photo clicked", Toast.LENGTH_SHORT).show()
                takePhoto()
            }
        }
        browseMemoriesButton = Button(this).apply {
            text = "Browse Memories"
            setOnClickListener { navigateToBrowseMemories() }
        }
        linkMemoriesButton = Button(this).apply {
            text = "Link Memories"
            setOnClickListener { navigateToLinkMemories() }
        }
        imageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(300, 300)
        }

        // Add views to overlay layout (vertical order)
        overlayLayout.addView(takePhotoButton)
        overlayLayout.addView(browseMemoriesButton)
        overlayLayout.addView(linkMemoriesButton)
        overlayLayout.addView(imageView)

        // Add overlay layout to the root layout
        rootLayout.addView(overlayLayout, overlayLayoutParams)

        // Set the root layout as the content view
        setContentView(rootLayout)

        // Check location permission and initialize map
        if (hasLocationPermission()) {
            initializeMap()
        } else {
            requestLocationPermission()
        }
    }

    private fun navigateToBrowseMemories() {
        // Navigate to BrowseMemoriesActivity (ensure this activity exists)
        //val intent = Intent(this, BrowseMemoriesActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToLinkMemories() {
        // Navigate to LinkMemoriesActivity (ensure this activity exists)
       // val intent = Intent(this, LinkMemoriesActivity::class.java)
        startActivity(intent)
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
            .withIconImage("marker-15")
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
            isPhotoTaken = true
            createWaypointForPhoto()
        }
    }

    private fun createWaypointForPhoto() {
        if (isPhotoTaken) {
            val locationComponentPlugin = mapView.location
            if (!listenerAdded) {
                locationComponentPlugin.addOnIndicatorPositionChangedListener { point ->
                    if (isPhotoTaken) {
                        val latitude = point.latitude()
                        val longitude = point.longitude()
                        createWaypoint(Point.fromLngLat(longitude, latitude))
                        isPhotoTaken = false
                        locationComponentPlugin.removeOnIndicatorPositionChangedListener {}
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
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE && grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            takePhoto()
        }
    }
}
