package com.example.gps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class MemoriesAdapter(private var memories: List<Memory>) :
    RecyclerView.Adapter<MemoriesAdapter.MemoryViewHolder>() {

    inner class MemoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.memory_image)
        val messageView: TextView = itemView.findViewById(R.id.memory_message)
        val locationView: TextView = itemView.findViewById(R.id.memory_location)
        val dateView: TextView = itemView.findViewById(R.id.memory_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_memory, parent, false)
        return MemoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: MemoryViewHolder, position: Int) {
        val memory = memories[position]

        Glide.with(holder.itemView.context)
            .load(memory.imgurLink)
            .into(holder.imageView)

        holder.messageView.text = memory.message
        holder.locationView.text = "Lat: %.4f, Lng: %.4f".format(memory.latitude, memory.longitude)
        holder.dateView.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(memory.timestamp))
    }

    override fun getItemCount() = memories.size

    fun updateMemories(newMemories: List<Memory>) {
        memories = newMemories
        notifyDataSetChanged()
    }
}

data class Memory(
    val imgurLink: String,
    val latitude: Double,
    val longitude: Double,
    val message: String,
    val timestamp: Long
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}
