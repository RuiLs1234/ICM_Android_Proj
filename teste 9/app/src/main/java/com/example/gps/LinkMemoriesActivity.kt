package com.example.gps

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.ResourceOptions
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.example.gps.models.Memory
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor


class LinkMemoriesActivity : AppCompatActivity() {

    private lateinit var memoryList: MutableList<Memory>
    private lateinit var listView: ListView
    private lateinit var mapView: MapView
    private lateinit var mapContainer: FrameLayout
    private lateinit var messageOverlay: LinearLayout
    private lateinit var mapboxMap: MapboxMap
    private var pointAnnotationManager: PointAnnotationManager? = null
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
        initializeMap()

        // Carregar 4 memórias aleatórias
        loadRandomMemoriesFromFirestore()
    }

    private fun setupUI() {
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Back Button
        rootLayout.addView(Button(this).apply {
            text = "Back to Main"
            setOnClickListener { finish() }
        })

        // ListView
        listView = ListView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0, 1f
            )
        }
        rootLayout.addView(listView)

        // Map Container
        mapContainer = FrameLayout(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0, 1f
            )
        }

        // MapView
        mapView = MapView(this, MapInitOptions(
            context = this@LinkMemoriesActivity,
            resourceOptions = ResourceOptions.Builder()
                .accessToken("YOUR_MAPBOX_ACCESS_TOKEN")
                .build()
        ))
        mapContainer.addView(mapView)

        // Message Overlay
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

            addView(ImageView(context).apply {
                layoutParams = LinearLayout.LayoutParams(250, 250).apply {
                    rightMargin = 16
                }
                scaleType = ImageView.ScaleType.CENTER_CROP
            })

            addView(TextView(context).apply {
                textSize = 18f
                setTextColor(0xFFFFFFFF.toInt())
            })
        }
        mapContainer.addView(messageOverlay)
        rootLayout.addView(mapContainer)
        setContentView(rootLayout)

        listView.adapter = MemoryAdapter(this, mutableListOf())
        listView.setOnItemClickListener { _, _, position, _ ->
            showMemoryOnMap(memoryList[position])
        }
    }

    private fun initializeMap() {
        mapboxMap = mapView.getMapboxMap()
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
            pointAnnotationManager = mapView.annotations.createPointAnnotationManager()
        }
    }

    private fun loadRandomMemoriesFromFirestore() {
        db.collection("memories")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val allMemories = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Memory::class.java)?.apply {
                        id = doc.id
                    }
                }

                // Embaralha as memórias e pega as 4 primeiras
                memoryList = allMemories.shuffled().take(4).toMutableList()

                (listView.adapter as? MemoryAdapter)?.apply {
                    clear()
                    addAll(memoryList)
                    notifyDataSetChanged()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error loading memories: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showMemoryOnMap(memory: Memory) {
        val point = Point.fromLngLat(memory.longitude, memory.latitude)
        mapboxMap.setCamera(CameraOptions.Builder()
            .center(point)
            .zoom(15.0)
            .build())

        pointAnnotationManager?.apply {
            deleteAll()
            create(PointAnnotationOptions()
                .withPoint(point)
                .withIconImage("marker-15")
                .withTextField(memory.message ?: "")
                .withTextAnchor(TextAnchor.TOP)
                .withIconSize(1.5))
        }

        messageOverlay.apply {
            (getChildAt(0) as ImageView).let { imageView ->
                Glide.with(this@LinkMemoriesActivity)
                    .load(memory.imgurLink)
                    .into(imageView)
            }
            (getChildAt(1) as TextView).text = memory.message ?: ""
            visibility = View.VISIBLE
        }
    }

    class MemoryAdapter(context: LinkMemoriesActivity, memories: List<Memory>) :
        ArrayAdapter<Memory>(context, 0, memories) {

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView as? LinearLayout ?: LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 16, 16, 16)
            }
            view.removeAllViews()

            getItem(position)?.let { memory ->
                view.addView(ImageView(context).apply {
                    layoutParams = LinearLayout.LayoutParams(200, 200)
                    Glide.with(context)
                        .load(memory.imgurLink)
                        .into(this)
                })

                view.addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(16, 0, 0, 0)

                    addView(TextView(context).apply {
                        text = memory.message ?: "(No message)"
                        textSize = 16f
                    })

                    addView(TextView(context).apply {
                        text = "Lat: %.4f, Lng: %.4f".format(memory.latitude, memory.longitude)
                        textSize = 14f
                    })
                })
            }
            return view
        }
    }
}
