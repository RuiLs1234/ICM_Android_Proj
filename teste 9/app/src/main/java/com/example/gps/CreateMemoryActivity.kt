package com.example.gps

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.maps.*
import com.mapbox.maps.plugin.locationcomponent.location
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class CreateMemoryActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var gpsTextView: TextView
    private lateinit var takeSelfieButton: Button
    private lateinit var saveMemoryButton: Button
    private lateinit var selfieImageView: ImageView
    private lateinit var messageEditText: EditText
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    // Firebase e Imgur
    private val db = FirebaseFirestore.getInstance()
    private val imgurId = "89528b049eb7c05"
    private val client = OkHttpClient()

    private var currentPhoto: Bitmap? = null
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 2001
        private const val TAG = "CreateMemoryActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cria o layout raiz
        val rootLayout = FrameLayout(this)

        // Configuração do MapView
        val mapInitOptions = MapInitOptions(
            context = this,
            resourceOptions = ResourceOptions.Builder()
                .accessToken("YOUR_MAPBOX_ACCESS_TOKEN")
                .build()
        )
        mapView = MapView(this, mapInitOptions)
        rootLayout.addView(mapView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        ))

        // Layout de sobreposição
        val overlayLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
        }
        val overlayParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM
        }

        // TextView para exibir o GPS
        gpsTextView = TextView(this).apply {
            text = "GPS: Unknown"
            textSize = 18f
        }
        overlayLayout.addView(gpsTextView)

        // Botão para tirar selfie
        takeSelfieButton = Button(this).apply {
            text = "Take Selfie"
            setOnClickListener { openCamera() }
        }
        overlayLayout.addView(takeSelfieButton)

        // ImageView para exibir a selfie capturada
        selfieImageView = ImageView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 400
            )
        }
        overlayLayout.addView(selfieImageView)

        // EditText para a mensagem pessoal
        messageEditText = EditText(this).apply {
            hint = "Enter your personal message"
        }
        overlayLayout.addView(messageEditText)

        // Botão para salvar a memória
        saveMemoryButton = Button(this).apply {
            text = "Save Memory"
            setOnClickListener {
                if (validateData()) {
                    uploadPhotoToImgur()
                } else {
                    Toast.makeText(this@CreateMemoryActivity,
                        "Preencha todos os dados", Toast.LENGTH_SHORT).show()
                }
            }
        }
        overlayLayout.addView(saveMemoryButton)

        rootLayout.addView(overlayLayout, overlayParams)
        setContentView(rootLayout)

        // Registra o launcher da câmera
        cameraLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val extras = result.data?.extras
                val imageBitmap = extras?.get("data") as? Bitmap
                if (imageBitmap != null) {
                    currentPhoto = imageBitmap
                    selfieImageView.setImageBitmap(imageBitmap)
                    Toast.makeText(this, "Selfie captured", Toast.LENGTH_SHORT).show()
                } else {
                    Log.e(TAG, "No bitmap in extras")
                    Toast.makeText(this, "Failed to capture image", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Verifica permissões
        if (hasLocationPermission()) {
            initializeMap()
        } else {
            requestLocationPermission()
        }
        if (!hasCameraPermission()) {
            requestCameraPermission()
        }
    }

    private fun validateData(): Boolean {
        return currentPhoto != null &&
                currentLatitude != null &&
                currentLongitude != null &&
                messageEditText.text.isNotEmpty()
    }

    private fun uploadPhotoToImgur() {
        val stream = ByteArrayOutputStream()
        currentPhoto?.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        val imageBytes = stream.toByteArray()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "image",
                "memory.jpg",
                imageBytes.toRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("https://api.imgur.com/3/image")
            .addHeader("Authorization", "Client-ID $imgurId")  // Header atualizado
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@CreateMemoryActivity,
                        "Falha no upload: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        runOnUiThread {
                            Toast.makeText(this@CreateMemoryActivity,
                                "Erro no upload", Toast.LENGTH_SHORT).show()
                        }
                        return
                    }

                    val responseData = response.body?.string()
                    try {
                        val json = JSONObject(responseData ?: "")
                        val imgurLink = json.getJSONObject("data").getString("link")
                        saveMemoryToFirestore(imgurLink)
                    } catch (e: Exception) {
                        runOnUiThread {
                            Toast.makeText(this@CreateMemoryActivity,
                                "Erro ao processar resposta", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }

    private fun saveMemoryToFirestore(imgurLink: String) {
        val memoryData = hashMapOf(
            "imgurLink" to imgurLink,
            "latitude" to currentLatitude,
            "longitude" to currentLongitude,
            "message" to messageEditText.text.toString(),
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("memories")
            .add(memoryData)
            .addOnSuccessListener {
                runOnUiThread {
                    Toast.makeText(this@CreateMemoryActivity,
                        "Memória salva com sucesso!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                runOnUiThread {
                    Toast.makeText(this@CreateMemoryActivity,
                        "Erro ao salvar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun hasLocationPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }

    private fun initializeMap() {
        mapboxMap = mapView.getMapboxMap()
        mapboxMap.loadStyleUri(Style.MAPBOX_STREETS) {
            val locationComponent = mapView.location
            locationComponent.updateSettings {
                enabled = true
                pulsingEnabled = true
            }
            locationComponent.addOnIndicatorPositionChangedListener { point ->
                currentLatitude = point.latitude()
                currentLongitude = point.longitude()
                gpsTextView.text = "GPS: $currentLatitude, $currentLongitude"
                mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)
                        .build()
                )
            }
        }
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            cameraLauncher.launch(intent)
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeMap()
            } else {
                Toast.makeText(this,
                    "Location permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }
}