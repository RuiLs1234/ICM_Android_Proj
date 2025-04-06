package com.example.gps

import android.database.Cursor
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.Gravity
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
    private lateinit var mapboxMap: MapboxMap
    private var pointAnnotationManager: PointAnnotationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a root LinearLayout that divides the screen vertically.
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Create a ListView to display memories (occupies half the screen).
        listView = ListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            )
        }
        rootLayout.addView(listView)

        // Create a MapView for Mapbox (occupies half the screen).
        val mapInitOptions = MapInitOptions(
            context = this,
            resourceOptions = ResourceOptions.Builder()
                .accessToken("YOUR_MAPBOX_ACCESS_TOKEN")
                .build()
        )
        mapView = MapView(this, mapInitOptions).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            )
        }
        rootLayout.addView(mapView)

        // Set the composed layout as the content view.
        setContentView(rootLayout)

        // Initialize the database helper and load memories.
        memoryDbHelper = MemoryDatabaseHelper(this)
        memoryList = mutableListOf()
        loadMemories()

        // Set up the custom adapter for the ListView.
        val adapter = MemoryAdapter(this, memoryList)
        listView.adapter = adapter

        // When a memory is clicked, update the MapView to center on its coordinates.
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
            // Remove any previous markers.
            pointAnnotationManager?.deleteAll()
            // Optionally, add a marker with the memory's message.
            val markerOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage("marker-15") // default marker image; ensure this asset exists.
                .withTextField(memory.message ?: "")
                .withTextAnchor(TextAnchor.TOP)
                .withIconSize(1.5)
            pointAnnotationManager?.create(markerOptions)
        }

        // Initialize the Mapbox map and create the annotation manager.
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

        override fun getView(position: Int, convertView: android.view.View?, parent: ViewGroup): android.view.View {
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

            // Create a vertical LinearLayout for the text information.
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
            val coordsTextView = TextView(context).apply {
                text = "Lat: ${memory.latitude}, Lon: ${memory.longitude}"
                textSize = 14f
            }
            textLayout.addView(messageTextView)
            textLayout.addView(coordsTextView)

            layout.addView(textLayout)
            return layout
        }
    }

    // Lifecycle handling for the MapView.

}

