package com.example.gps

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

class MemoryDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "memories.db", null, 2) { // versão 2 para garantir o upgrade

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE memories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                image BLOB,
                latitude REAL,
                longitude REAL,
                message TEXT
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS memories")
        onCreate(db)
    }

    fun insertMemory(image: ByteArray, lat: Double, lon: Double, message: String): Long {
        val values = ContentValues().apply {
            put("image", image)
            put("latitude", lat)
            put("longitude", lon)
            put("message", message)
        }
        return writableDatabase.insert("memories", null, values)
    }
}

