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
import com.mapbox.maps.*
import com.mapbox.maps.plugin.locationcomponent.location
import java.io.ByteArrayOutputStream

class CreateMemoryActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var mapboxMap: MapboxMap
    private lateinit var gpsTextView: TextView
    private lateinit var takeSelfieButton: Button
    private lateinit var saveMemoryButton: Button
    private lateinit var selfieImageView: ImageView
    private lateinit var messageEditText: EditText    // Campo para mensagem do usuário
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var memoryDbHelper: MemoryDatabaseHelper

    private var currentPhoto: Bitmap? = null
    private var currentLatitude: Double? = null
    private var currentLongitude: Double? = null

    companion object {
        private const val PERMISSION_REQUEST_CODE = 2001
        private const val TAG = "CreateMemoryActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        memoryDbHelper = MemoryDatabaseHelper(this)

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
            setOnClickListener { saveMemory() }
        }
        overlayLayout.addView(saveMemoryButton)

        rootLayout.addView(overlayLayout, overlayParams)
        setContentView(rootLayout)

        // Regista o launcher da câmera usando a API ActivityResult
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

        // Verifica e solicita as permissões de localização e câmera se necessário
        if (hasLocationPermission()) {
            initializeMap()
        } else {
            requestLocationPermission()
        }
        if (!hasCameraPermission()) {
            requestCameraPermission()
        }
    }

    // Verifica se a permissão de localização foi concedida
    private fun hasLocationPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    // Solicita a permissão de localização
    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            PERMISSION_REQUEST_CODE
        )
    }

    // Verifica se a permissão de câmera foi concedida
    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        this, Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    // Solicita a permissão de câmera
    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.CAMERA),
            PERMISSION_REQUEST_CODE
        )
    }

    // Inicializa o MapView e atualiza as informações de GPS
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

    // Abre a câmera para capturar a foto
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(packageManager) != null) {
            cameraLauncher.launch(intent)
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show()
        }
    }

    // Salva a memória (foto, localização e mensagem) no banco de dados e retorna para a MainActivity
    private fun saveMemory() {
        if (currentLatitude != null && currentLongitude != null && currentPhoto != null) {
            val stream = ByteArrayOutputStream()
            currentPhoto?.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()

            // Captura a mensagem digitada pelo usuário (ex: "My home :)")
            val userMessage = messageEditText.text.toString()

            val id = memoryDbHelper.insertMemory(byteArray, currentLatitude!!, currentLongitude!!, userMessage)

            if (id != -1L) {
                Toast.makeText(
                    this,
                    "Memory saved to database!\nGPS: ($currentLatitude, $currentLongitude)",
                    Toast.LENGTH_LONG
                ).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "Failed to save memory.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Missing data. Ensure you have a location, a selfie and a message.", Toast.LENGTH_SHORT).show()
        }
    }



    // Tratamento do resultado da solicitação de permissões
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
                Toast.makeText(this, "Location permission is required", Toast.LENGTH_SHORT).show()
            }
        }
    }
}


