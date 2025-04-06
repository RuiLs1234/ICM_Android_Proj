package com.example.gps

import android.database.Cursor
import android.graphics.BitmapFactory
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

// Data class representing one memory
data class Memory(
    val id: Int,
    val image: ByteArray,
    val latitude: Double,
    val longitude: Double,
    val message: String?
)

class BrowseMemoriesActivity : AppCompatActivity() {

    private lateinit var memoryDbHelper: MemoryDatabaseHelper
    private lateinit var listView: ListView
    private lateinit var adapter: MemoryAdapter
    private val memoryList = mutableListOf<Memory>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Create a ListView dynamically (or you could use an XML layout)
        listView = ListView(this)
        setContentView(listView)

        // Initialize the database helper
        memoryDbHelper = MemoryDatabaseHelper(this)

        // Load memories from the database
        loadMemories()

        // Check if there are any memories
        if (memoryList.isNotEmpty()) {
            // Set up the adapter and attach it to the ListView
            adapter = MemoryAdapter(this, memoryList)
            listView.adapter = adapter
        } else {
            // If no memories, display a "No memories" message
            val noMemoriesTextView = TextView(this).apply {
                text = "No memories available."
                textSize = 20f
                setPadding(16, 16, 16, 16)
                gravity = android.view.Gravity.CENTER
            }
            setContentView(noMemoriesTextView)
        }
    }

    private fun loadMemories() {
        val db = memoryDbHelper.readableDatabase
        val cursor: Cursor = db.query(
            "memories",   // Table name
            null,         // All columns
            null, null, null, null,
            "id DESC"     // Order from newest to oldest
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
}

// Custom adapter to display each memory
class MemoryAdapter(context: BrowseMemoriesActivity, private val memories: List<Memory>) :
    ArrayAdapter<Memory>(context, 0, memories) {

    override fun getView(position: Int, convertView: android.view.View?, parent: android.view.ViewGroup): android.view.View {
        // Create or reuse the view for the list item
        val itemView = convertView ?: LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(16, 16, 16, 16)
        }

        // Clear any previous content if reusing a view
        if (itemView is LinearLayout) {
            itemView.removeAllViews()

            val memory = memories[position]

            // ImageView for the memory photo
            val imageView = ImageView(context).apply {
                val bitmap = BitmapFactory.decodeByteArray(memory.image, 0, memory.image.size)
                setImageBitmap(bitmap)
                layoutParams = LinearLayout.LayoutParams(200, 200)
            }
            itemView.addView(imageView)

            // Vertical layout to hold the message and coordinates
            val textLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 0, 0, 0)
            }

            // TextView for the user message (or fallback text)
            val messageTextView = TextView(context).apply {
                text = memory.message ?: "(No message)"
                textSize = 16f
            }
            // TextView for the coordinates
            val coordsTextView = TextView(context).apply {
                text = "Lat: ${memory.latitude}, Lon: ${memory.longitude}"
                textSize = 14f
            }

            textLayout.addView(messageTextView)
            textLayout.addView(coordsTextView)
            itemView.addView(textLayout)
        }

        return itemView
    }
}
