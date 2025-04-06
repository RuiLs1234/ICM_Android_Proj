package com.example.gps

import android.database.Cursor
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
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

// Data class representing one memory record.
data class Memory(
    val id: Int,
    val image: ByteArray,
    val latitude: Double,
    val longitude: Double,
    val message: String?
)

class BrowseMemoriesActivity : AppCompatActivity() {

    private lateinit var memoryDbHelper: MemoryDatabaseHelper
    private lateinit var memoryList: MutableList<Memory>
    private lateinit var listView: ListView
    private lateinit var mapView: MapView
    private lateinit var mapContainer: FrameLayout
    // Updated overlay: a horizontal LinearLayout with an ImageView and a TextView for message.
    private lateinit var messageOverlay: LinearLayout
    private lateinit var mapboxMap: MapboxMap
    private var pointAnnotationManager: PointAnnotationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a root layout dividing screen vertically.
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Create ListView for memories (occupies half the screen).
        listView = ListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            )
        }
        rootLayout.addView(listView)

        // Create a container for the MapView so we can add an overlay.
        mapContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            )
        }

        // Create the MapView.
        val mapInitOptions = MapInitOptions(
            context = this,
            resourceOptions = ResourceOptions.Builder()
                .accessToken("YOUR_MAPBOX_ACCESS_TOKEN")
                .build()
        )
        mapView = MapView(this, mapInitOptions)
        mapContainer.addView(mapView)

        // Create the overlay layout.
        // It always displays an ImageView for the photo and a TextView for the message.
        messageOverlay = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xAA000000.toInt())  // Semi-transparent black.
            setPadding(16, 16, 16, 16)
            visibility = View.GONE  // Initially hidden.
            // Position overlay at the bottom-center of the map.
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            )
        }

        // ImageView: always present.
        val overlayImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(250, 250).apply {
                rightMargin = 16
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        messageOverlay.addView(overlayImageView)

        // TextView for the optional message.
        val overlayMessageTextView = TextView(this).apply {
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            // If no message, this text will simply be empty.
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        messageOverlay.addView(overlayMessageTextView)

        mapContainer.addView(messageOverlay)
        rootLayout.addView(mapContainer)

        // Set the composed layout as the content view.
        setContentView(rootLayout)

        // Initialize the database helper and load memories.
        memoryDbHelper = MemoryDatabaseHelper(this)
        memoryList = mutableListOf()
        loadMemories()

        // Set up the custom adapter for the ListView.
        val adapter = MemoryAdapter(this, memoryList)
        listView.adapter = adapter

        // When a memory is clicked, update the MapView.
        listView.setOnItemClickListener { _, _, position, _ ->
            val memory = memoryList[position]
            val point = Point.fromLngLat(memory.longitude, memory.latitude)
            // Move camera to the memory's location.
            mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(15.0)
                    .build()
            )
            // Remove previous markers.
            pointAnnotationManager?.deleteAll()
            // Add a marker with the memory's (optional) message.
            val markerOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage("marker-15")
                .withTextField(memory.message ?: "")
                .withTextAnchor(TextAnchor.TOP)
                .withIconSize(1.5)
            pointAnnotationManager?.create(markerOptions)

            // Update overlay:
            // Always show the photo.
            val bitmap = BitmapFactory.decodeByteArray(memory.image, 0, memory.image.size)
            overlayImageView.setImageBitmap(bitmap)
            // If there is a nonblank message, show it; otherwise, set text to empty.
            if (!memory.message.isNullOrBlank()) {
                overlayMessageTextView.text = memory.message
            } else {
                overlayMessageTextView.text = ""
            }
            // Make overlay visible.
            messageOverlay.visibility = View.VISIBLE
        }

        // Initialize Mapbox map and create the annotation manager.
        mapboxMap = mapView.getMapboxMap()
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
            pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        }
    }

    // Loads all memories from the SQLite database.
    private fun loadMemories() {
        val db = memoryDbHelper.readableDatabase
        val cursor: Cursor = db.query(
            "memories", // Table name.
            null,       // All columns.
            null, null, null, null,
            "id DESC"   // Order from newest to oldest.
        )

        memoryList.clear()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val image = cursor.getBlob(cursor.getColumnIndexOrThrow("image"))
                val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))
                val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
                val message = cursor.getString(cursor.getColumnIndexOrThrow("message"))
                memoryList.add(Memory(id, image, latitude, longitude, message))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
    }

    // Custom adapter to display each memory in the ListView.
    class MemoryAdapter(context: BrowseMemoriesActivity, private val memories: List<Memory>) :
        ArrayAdapter<Memory>(context, 0, memories) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val layout = convertView as? LinearLayout ?: LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 16, 16, 16)
            }
            layout.removeAllViews()

            val memory = memories[position]

            // Create an ImageView for the memory photo.
            val imageView = ImageView(context).apply {
                val bitmap = BitmapFactory.decodeByteArray(memory.image, 0, memory.image.size)
                setImageBitmap(bitmap)
                layoutParams = LinearLayout.LayoutParams(200, 200)
            }
            layout.addView(imageView)

            // Create a vertical LinearLayout for text information (optional message only).
            val textLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            val messageTextView = TextView(context).apply {
                text = memory.message ?: "(No message)"
                textSize = 16f
            }
            textLayout.addView(messageTextView)

            layout.addView(textLayout)
            return layout
        }
    }
}

