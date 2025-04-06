package com.example.gps

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

class ImgurUploader(private val clientId: String) {

    interface UploadCallback {
        fun onSuccess(imageUrl: String)
        fun onError(error: String)
    }

    fun uploadImage(imageBytes: ByteArray, callback: UploadCallback) {
        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                "image.jpg",
                imageBytes.toRequestBody("image/*".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("https://api.imgur.com/3/image")
            .post(requestBody)
            .addHeader("Authorization", "Client-ID $clientId")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback.onError(e.message ?: "Unknown error")
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    val json = JSONObject(response.body?.string() ?: "")
                    if (json.getBoolean("success")) {
                        val link = json.getJSONObject("data").getString("link")
                        callback.onSuccess(link)
                    } else {
                        callback.onError("Upload failed")
                    }
                } catch (e: Exception) {
                    callback.onError(e.message ?: "JSON parsing error")
                }
            }
        })
    }
}