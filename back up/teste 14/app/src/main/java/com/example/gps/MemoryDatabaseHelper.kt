package com.example.gps

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.content.ContentValues

class MemoryDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "memories.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE memories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                image BLOB,
                latitude REAL,
                longitude REAL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS memories")
        onCreate(db)
    }

    fun insertMemory(image: ByteArray, lat: Double, lon: Double): Long {
        val values = ContentValues().apply {
            put("image", image)
            put("latitude", lat)
            put("longitude", lon)
        }
        return writableDatabase.insert("memories", null, values)
    }
}
