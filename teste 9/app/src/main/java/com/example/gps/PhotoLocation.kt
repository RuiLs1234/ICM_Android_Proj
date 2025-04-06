package com.example.gps

import java.util.Date

data class PhotoLocation(
    var id: String = "",
    var imgurLink: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var title: String = "",
    var timestamp: Date = Date()
)