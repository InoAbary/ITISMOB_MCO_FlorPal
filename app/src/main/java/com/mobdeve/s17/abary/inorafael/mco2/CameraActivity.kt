package com.mobdeve.s17.abary.inorafael.mco2

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*

import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.toString

class CameraActivity : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var captureButton: ImageButton
    private lateinit var returnFromCam: Button

    private lateinit var getFromGallery: Button
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private var imageCapture: ImageCapture? = null

    private val requestCameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }

    private val requestStoragePermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else Toast.makeText(this, "Camera saving denied", Toast.LENGTH_SHORT).show()
        }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_view)

        imagePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {

            result ->
            if (result.resultCode == Activity.RESULT_OK){

                val uri = result.data?.data
                uri?.let {


                    try {
                        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        contentResolver.takePersistableUriPermission(it, takeFlags)
                    } catch (e: SecurityException) {

                    }

                    val intent = Intent()
                    intent.putExtra("captured_image", it.toString())
                    intent.data = it
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setResult(RESULT_OK, intent)

                    finish()
                }

            }
        }

        previewView = findViewById(R.id.previewView)
        captureButton = findViewById(R.id.btnCapture)
        returnFromCam = findViewById(R.id.returnFromCam)
        getFromGallery = findViewById(R.id.getFromGallery)

        // Request camera permission
        if (hasCameraPermission()) {
            startCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }

        // Capture button listener
        captureButton.setOnClickListener {
            if (hasStoragePermission()) {
                takePhoto()
            } else {
                requestStoragePermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        returnFromCam.setOnClickListener {
            finish()
        }

        getFromGallery.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }
            imagePickerLauncher.launch(intent)
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                this,
                cameraSelector,
                preview,
                imageCapture
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun hasStoragePermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT <= 28) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        } else true
    }
    private fun takePhoto() {
        val capture = imageCapture ?: return

        val name = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
            .format(System.currentTimeMillis())

        // Save to MediaStore (Gallery)
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        }


        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            .build()

        capture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    Toast.makeText(
                        this@CameraActivity,
                        "Saved to Gallery!",
                        Toast.LENGTH_SHORT
                    ).show()
                    val savedUri = output.savedUri

                    if (savedUri != null) {

                        try {
                            contentResolver.takePersistableUriPermission(savedUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        } catch (e: SecurityException) {

                        }

                        val intent = Intent()
                        intent.putExtra("captured_image", savedUri.toString())
                        intent.data = savedUri
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        setResult(Activity.RESULT_OK, intent)
                    }
                    finish()
                }

                override fun onError(exception: ImageCaptureException) {
                    Toast.makeText(
                        this@CameraActivity,
                        "Error: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
    }
}
