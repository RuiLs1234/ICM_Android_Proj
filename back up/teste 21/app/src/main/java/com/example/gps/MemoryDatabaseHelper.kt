package com.example.gps

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MemoryDatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "memories.db", null, 2) {

    override fun onCreate(db: SQLiteDatabase) {
        // Existing table for memories
        db.execSQL("""
            CREATE TABLE memories (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                image BLOB,
                latitude REAL,
                longitude REAL,
                message TEXT
            )
        """.trimIndent())

        // New table for user credentials
        db.execSQL("""
            CREATE TABLE user_credentials (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                email TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL
            )
        """.trimIndent())

        // New table for current user (stores only one record)
        db.execSQL("""
            CREATE TABLE current_user (
                email TEXT UNIQUE NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS memories")
        db.execSQL("DROP TABLE IF EXISTS user_credentials")
        db.execSQL("DROP TABLE IF EXISTS current_user")
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

    fun insertUserCredential(email: String, password: String): Long {
        val values = ContentValues().apply {
            put("email", email)
            put("password", password)
        }
        return writableDatabase.insert("user_credentials", null, values)
    }

    fun updateCurrentUser(email: String) {
        val db = writableDatabase
        db.delete("current_user", null, null)
        val values = ContentValues().apply {
            put("email", email)
        }
        db.insert("current_user", null, values)
    }

    fun getCurrentUser(): String? {
        val cursor = readableDatabase.rawQuery("SELECT email FROM current_user LIMIT 1", null)
        return if (cursor.moveToFirst()) {
            val email = cursor.getString(0)
            cursor.close()
            email
        } else {
            cursor.close()
            null
        }
    }

    // Check if the email and password match a record in the user_credentials table.
    fun checkUserCredential(email: String, password: String): Boolean {
        val query = "SELECT id FROM user_credentials WHERE email = ? AND password = ?"
        val cursor = readableDatabase.rawQuery(query, arrayOf(email, password))
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }
}

