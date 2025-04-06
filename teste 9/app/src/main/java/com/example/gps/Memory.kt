package com.example.gps.models

import java.text.SimpleDateFormat
import java.util.*

data class Memory(
    var id: String = "",  // Adicionei o ID para o Firestore
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