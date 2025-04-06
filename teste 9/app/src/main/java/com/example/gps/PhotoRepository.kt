package com.example.gps

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class PhotoRepository {
    private val db = FirebaseFirestore.getInstance()
    private val COLLECTION_NAME = "photos"

    fun addPhotoLocation(photo: PhotoLocation): Task<Void> {
        return if (photo.id.isEmpty()) {
            db.collection(COLLECTION_NAME).add(photo).continueWithTask { task ->
                val docId = task.result?.id ?: throw Exception("Failed to add document")
                photo.id = docId
                db.collection(COLLECTION_NAME).document(docId).set(photo)
            }
        } else {
            db.collection(COLLECTION_NAME).document(photo.id).set(photo)
        }
    }

    fun getPhotoLocations(): Task<QuerySnapshot> {
        return db.collection(COLLECTION_NAME)
            .orderBy("timestamp")
            .get()
    }
}