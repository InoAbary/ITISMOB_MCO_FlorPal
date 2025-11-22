package com.mobdeve.s17.abary.inorafael.mco2

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.mobdeve.s17.abary.inorafael.mco2.databinding.AddPlantBinding
import java.time.LocalDate
import kotlin.text.isBlank
import kotlin.toString
import androidx.core.content.edit

import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

class AddPlantActivity : AppCompatActivity() {

    private lateinit var binding: AddPlantBinding
    private var imageCapture: ImageCapture? = null

//    private val requestCameraPermission =
//        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
//            if (granted) startCamera()
//            else Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
//        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AddPlantBinding.inflate(layoutInflater)
        setContentView(binding.root)





        // date pickers
        // to implement: validate dates (make sure next watering
        // sched is later than or equal to last watered)

        binding.lastWateredDateInput.setOnClickListener {
            DatePicker { formatted ->
                binding.lastWateredDateInput.setText(formatted)
            }.show(supportFragmentManager, "lastWateredDate")
        }

        binding.defaultPlantImg.setOnClickListener {

//            if (hasCameraPermission()) {
//                startCamera()
//            } else {
//                requestCameraPermission.launch(Manifest.permission.CAMERA)
//            }


        }

        binding.nextWateringDateInput.setOnClickListener {
            DatePicker { formatted ->
                binding.nextWateringDateInput.setText(formatted)
            }.show(supportFragmentManager, "nextWateringDate")
        }

        // cancel button
        binding.cancelBtn.setOnClickListener {
            finish()
        }

        // add plant button
        // only validates if required fields were answered for now
        // remember to make sure nickname of plant is unique (ex: roseanne)
        binding.addPlantBtn.setOnClickListener {
            val name = binding.plantNameInput.text.toString()
            val type = binding.plantTypeInput.text.toString()
            val lastDate = binding.lastWateredDateInput.text.toString()
            val nextDate = binding.nextWateringDateInput.text.toString()
            val fruitProductionRate = binding.fruitRateInput.text.toString().ifBlank { "" }
            val flowerColor = binding.flowerColorInput.text.toString()
            val wateringAmount = binding.wateringAmountInput.text.toString().ifBlank{"0"}
            val location = binding.locationInput.text.toString()
            val now = LocalDate.now()



            if (name.isBlank() || type.isBlank() || lastDate.isBlank() ||nextDate.isBlank()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            } else {
                val dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
                val newLastWaterDate = LocalDate.parse(lastDate, dateFormatter)
                val newNextWaterDate = LocalDate.parse(nextDate, dateFormatter)

                // added nov 22

                val wateringFrequency = java.time.temporal.ChronoUnit.DAYS.between(newLastWaterDate, newNextWaterDate).toInt()

                if (wateringFrequency <= 0) {
                    Toast.makeText(this, "Next watering must be after last watering", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                // end of nov 22 change

                val newPlant = PlantModel(name, name, 0,
                                            fruitProductionRate, flowerColor, CustomDate(now.month.toString(), now.dayOfMonth, now.year),
                                CustomDate(newLastWaterDate.month.toString(), newLastWaterDate.dayOfMonth, newLastWaterDate.year), wateringAmount.toDouble(),
                                            location, false, CustomDate(newNextWaterDate.month.toString(), newNextWaterDate.dayOfMonth, newNextWaterDate.year))
                val db = FirebaseFirestore.getInstance()
                val sp = getSharedPreferences("FlorPal_User_Prefs", MODE_PRIVATE)
                var userId = sp.getString("user_id", null)


                val plantDB = db.collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)
                val plantData = hashMapOf(
                    FlorPal_FireStoreRefs.USER_ID_FIELD to userId,
                    FlorPal_FireStoreRefs.DATE_CREATED_FIELD to newPlant.dateCreated.toString(),
                    FlorPal_FireStoreRefs.FLOWER_COLOR_FIELD to newPlant.flowerColor,
                    FlorPal_FireStoreRefs.FRUIT_PRODUCTION_FIELD to newPlant.fruitProductionRate.toString(),
                    FlorPal_FireStoreRefs.NAME_FIELD to newPlant.plantName,
                    FlorPal_FireStoreRefs.NICKNAME_FIELD to newPlant.plantNickName,
                    FlorPal_FireStoreRefs.PLANT_PHOTO_FIELD to newPlant.plantPhoto,
                    FlorPal_FireStoreRefs.WATERED_DATE_FIELD to newPlant.wateredDate.toString(),
                    FlorPal_FireStoreRefs.WATERING_AMOUT_FIELD to newPlant.wateringAmount.toString().toDouble(),
                    FlorPal_FireStoreRefs.LOCATION_FIELD to newPlant.location.toString(),
                    FlorPal_FireStoreRefs.FAVORITED_FIELD to newPlant.favorite,
                    FlorPal_FireStoreRefs.NEXT_WATER_DATE_FIELD to newPlant.nextWateredDate.toString(),
                    FlorPal_FireStoreRefs.WATERING_FREQUENCY_FIELD to wateringFrequency // added nov 22
                )

                plantDB
                    .add(plantData)
                    .addOnSuccessListener { documentReference ->
                        Toast.makeText(this, "Plant added successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to add plant. Reason: ${e.toString()}", Toast.LENGTH_SHORT).show()
                    }

                setResult(Activity.RESULT_OK)
                finish()
            }
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//
//        cameraProviderFuture.addListener({
//            val cameraProvider = cameraProviderFuture.get()
//
//            val preview = Preview.Builder().build().also {
//                it.setSurfaceProvider(previewView.surfaceProvider)
//            }
//
//            imageCapture = ImageCapture.Builder()
//                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
//                .build()
//
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//            cameraProvider.unbindAll()
//            cameraProvider.bindToLifecycle(
//                this,
//                cameraSelector,
//                preview,
//                imageCapture
//            )
//        }, ContextCompat.getMainExecutor(this))
//    }
//
//    private fun takePhoto() {
//        val capture = imageCapture ?: return
//
//        val name = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
//            .format(System.currentTimeMillis())
//
//        // Save to MediaStore (Gallery)
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
//            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-App")
//        }
//
//        val outputOptions = ImageCapture.OutputFileOptions
//            .Builder(contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
//            .build()
//
//        capture.takePicture(
//            outputOptions,
//            ContextCompat.getMainExecutor(this),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                    Toast.makeText(
//                        this@AddPlantActivity,
//                        "Saved to Gallery!",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//
//                override fun onError(exception: ImageCaptureException) {
//                    Toast.makeText(
//                        this@AddPlantActivity,
//                        "Error: ${exception.message}",
//                        Toast.LENGTH_SHORT
//                    ).show()
//                }
//            }
//        )
//    }
}
