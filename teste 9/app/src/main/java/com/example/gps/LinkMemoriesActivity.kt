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

// Data class que representa um registo de memória.
data class Memory1(
    val id: Int,
    val image: ByteArray,
    val latitude: Double,
    val longitude: Double,
    val message: String?
)

class LinkMemoriesActivity : AppCompatActivity() {

    private lateinit var memoryDbHelper: MemoryDatabaseHelper
    private lateinit var memoryList: MutableList<Memory1>
    private lateinit var listView: ListView
    private lateinit var mapView: MapView
    private lateinit var mapContainer: FrameLayout
    private lateinit var messageOverlay: LinearLayout
    private lateinit var mapboxMap: MapboxMap
    private var pointAnnotationManager: PointAnnotationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val backButton = Button(this).apply {
            text = "Back to Main"
            setOnClickListener {
                finish()
            }
        }
        rootLayout.addView(backButton)

        listView = ListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            )
        }
        rootLayout.addView(listView)

        mapContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0, 1f
            )
        }

        val mapInitOptions = MapInitOptions(
            context = this,
            resourceOptions = ResourceOptions.Builder()
                .accessToken("sk.eyJ1IjoicnVpdWEiLCJhIjoiY200bXM4czU5MDBwZDJrcjJsZW9qNzVjOCJ9.TlDHWxGJe7rdI03udVud3w")
                .build()
        )
        mapView = MapView(this, mapInitOptions)
        mapContainer.addView(mapView)

        messageOverlay = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(0xAA000000.toInt())
            setPadding(16, 16, 16, 16)
            visibility = View.GONE
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            )
        }

        val overlayImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(250, 250).apply {
                rightMargin = 16
            }
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        messageOverlay.addView(overlayImageView)

        val overlayMessageTextView = TextView(this).apply {
            textSize = 18f
            setTextColor(0xFFFFFFFF.toInt())
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        messageOverlay.addView(overlayMessageTextView)

        mapContainer.addView(messageOverlay)
        rootLayout.addView(mapContainer)

        setContentView(rootLayout)

        memoryDbHelper = MemoryDatabaseHelper(this)
        memoryList = mutableListOf()
        loadMemories()

        val adapter = MemoryAdapter(this, memoryList)
        listView.adapter = adapter

        listView.setOnItemClickListener { _, _, position, _ ->
            val memory = memoryList[position]
            val point = Point.fromLngLat(memory.longitude, memory.latitude)
            mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(15.0)
                    .build()
            )
            pointAnnotationManager?.deleteAll()

            val markerOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage("marker-15")
                .withTextField(memory.message ?: "")
                .withTextAnchor(TextAnchor.TOP)
                .withIconSize(1.5)
            pointAnnotationManager?.create(markerOptions)

            val bitmap = BitmapFactory.decodeByteArray(memory.image, 0, memory.image.size)
            overlayImageView.setImageBitmap(bitmap)
            overlayMessageTextView.text = memory.message ?: ""
            messageOverlay.visibility = View.VISIBLE
        }

        mapboxMap = mapView.getMapboxMap()
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
            pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        }
    }

    // Loads memories that do NOT belong to the current user.
    private fun loadMemories() {
        val currentUser = memoryDbHelper.getCurrentUser()
        if (currentUser == null) {
            Toast.makeText(this, "No current user found.", Toast.LENGTH_SHORT).show()
            return
        }

        val db = memoryDbHelper.readableDatabase
        // Query to load only memories that do NOT belong to the current user.
        val cursor: Cursor = db.query(
            "memories",
            null,
            "user_email <> ?",
            arrayOf(currentUser),
            null,
            null,
            "id DESC"
        )

        val allMemories = mutableListOf<Memory1>()
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getInt(cursor.getColumnIndexOrThrow("id"))
                val image = cursor.getBlob(cursor.getColumnIndexOrThrow("image"))
                val latitude = cursor.getDouble(cursor.getColumnIndexOrThrow("latitude"))
                val longitude = cursor.getDouble(cursor.getColumnIndexOrThrow("longitude"))
                val message = cursor.getString(cursor.getColumnIndexOrThrow("message"))
                allMemories.add(Memory1(id, image, latitude, longitude, message))
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()

        // Shuffle and take a subset (4 items) for display.
        memoryList = allMemories.shuffled().take(4).toMutableList()
    }

    class MemoryAdapter(context: LinkMemoriesActivity, private val memories: List<Memory1>) :
        ArrayAdapter<Memory1>(context, 0, memories) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val layout = convertView as? LinearLayout ?: LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 16, 16, 16)
            }
            layout.removeAllViews()

            val memory = memories[position]
            val imageView = ImageView(context).apply {
                val bitmap = BitmapFactory.decodeByteArray(memory.image, 0, memory.image.size)
                setImageBitmap(bitmap)
                layoutParams = LinearLayout.LayoutParams(200, 200)
            }
            layout.addView(imageView)

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
