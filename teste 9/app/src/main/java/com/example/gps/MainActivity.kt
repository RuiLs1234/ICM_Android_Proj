package com.example.gps

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Bundle
import android.widget.Button
import android.widget.FrameLayout
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
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location
import android.content.Intent
import android.provider.MediaStore

class MainActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var pointAnnotationManager: PointAnnotationManager
    private var waypointCounter = 1

    private val waypointList = mutableListOf<Pair<Double, Double>>()

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val REQUEST_CODE_TAKE_PHOTO = 101
    }

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

        val cameraButton = Button(this).apply {
            text = "Take Photo"
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginStart = 16
                topMargin = 240 // position below the clear button
            }
        }

        cameraButton.setOnClickListener {
            takePhoto()
        }

        mapView.addView(cameraButton)

        if (hasLocationPermission()) {
            initializeMap()
        } else {
            requestLocationPermission()
        }
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
            PERMISSION_REQUEST_CODE
        )
    }

    private fun takePhoto() {
        if (hasCameraPermission()) {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            startActivityForResult(intent, REQUEST_CODE_TAKE_PHOTO)
        } else {
            requestCameraPermission()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_TAKE_PHOTO && resultCode == RESULT_OK) {
            val imageUri = data?.data
            imageUri?.let {
                // You can display or save the photo
                Toast.makeText(this, "Photo taken successfully", Toast.LENGTH_SHORT).show()
            }
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

    private fun initializeMap() {
        mapboxMap = mapView.getMapboxMap() // Initialize mapboxMap here

        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) { style ->
            // Enable the LocationComponent for GPS tracking
            val locationComponentPlugin = mapView.location
            locationComponentPlugin.updateSettings {
                enabled = true
                pulsingEnabled = true
            }

            // Set camera position to user's location when available
            locationComponentPlugin.addOnIndicatorPositionChangedListener { point ->
                mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)
                        .build()
                )
            }

            // Initialize the PointAnnotationManager
            pointAnnotationManager = mapView.annotations.createPointAnnotationManager()

            // Use a touch listener to detect map clicks
            mapView.setOnTouchListener { _, event ->
                if (event.action == android.view.MotionEvent.ACTION_UP) {
                    val screenPoint = ScreenCoordinate(event.x.toDouble(), event.y.toDouble())
                    val mapPoint = mapboxMap.coordinateForPixel(screenPoint)
                    if (mapPoint != null) {
                        createWaypoint(mapPoint)
                    }
                }
                false // Return false to allow further event processing
            }
        }
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

        Toast.makeText(this, "Waypoint $waypointCounter added at: ${point.latitude()}, ${point.longitude()}", Toast.LENGTH_SHORT).show()

        waypointCounter++
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            initializeMap()
        }
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }
}
