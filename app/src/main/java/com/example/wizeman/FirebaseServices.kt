package com.example.wizeman

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.storage
import kotlinx.coroutines.CoroutineScope

class FirebaseServices(context: CoroutineScope) {

    //firebase
    fun saveImageToFirebase(imageBytes: ByteArray, latitude: Double, longitude: Double, weatherInfo: String) {
        val storage = Firebase.storage
        val storageRef = storage.reference
        val imagesRef = storageRef.child("images/${System.currentTimeMillis()}.jpg")

        val uploadTask = imagesRef.putBytes(imageBytes)
        uploadTask.addOnSuccessListener { taskSnapshot ->
            imagesRef.downloadUrl.addOnSuccessListener { uri ->
                saveMetadataToFirestore(uri.toString(), latitude, longitude, weatherInfo)
            }
        }.addOnFailureListener {
            Log.e(CameraService.TAG, "Failed to upload image")
        }
    }

    private fun saveMetadataToFirestore(imageUrl: String, latitude: Double, longitude: Double, weatherInfo: String) {
        val db = FirebaseFirestore.getInstance()
        val data = hashMapOf(
            "timestamp" to FieldValue.serverTimestamp(),
            "imageUrl" to imageUrl,
            "latitude" to latitude,
            "longitude" to longitude,
            "weatherInfo" to weatherInfo
        )

        db.collection("violations")
            .add(data)
            .addOnSuccessListener { documentReference ->
                Log.d(CameraService.TAG, "DocumentSnapshot written with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                Log.w(CameraService.TAG, "Error adding document", e)
            }
    }
}