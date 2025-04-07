package com.example.gps.models

import java.text.SimpleDateFormat
import java.util.*

data class Memory(
    var id: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val imgurLink: String? = null,
    val message: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val userEmail: String? = null  // Alterado para email ao invés de userId
) {
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}