package com.mobdeve.s17.abary.inorafael.mco2

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.mobdeve.s17.abary.inorafael.mco2.databinding.AddPlantBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.core.net.toUri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class AddPlantActivity : AppCompatActivity() {

    private lateinit var binding: AddPlantBinding
    private var capturedImage: Uri? = null

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data ?: result.data?.getStringExtra("captured_image")?.toUri()
                uri?.let {
                    capturedImage = it
                    binding.defaultPlantImg.setImageURI(capturedImage)
                    binding.addPhotoLabel.text = "Take New Photo"
                    binding.addPhotoIcon.visibility = View.GONE
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = AddPlantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ADDED: firestore formatter for consistent date saving
        val firestoreFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)

        binding.lastWateredDateInput.setOnClickListener {
            DatePicker { formatted -> binding.lastWateredDateInput.setText(formatted) }
                .show(supportFragmentManager, "lastWateredDate")
        }

        binding.nextWateringDateInput.setOnClickListener {
            DatePicker { formatted -> binding.nextWateringDateInput.setText(formatted) }
                .show(supportFragmentManager, "nextWateringDate")
        }

        binding.defaultPlantImg.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            activityResultLauncher.launch(intent)
        }

        binding.cancelBtn.setOnClickListener { finish() }

        binding.addPlantBtn.setOnClickListener {
            val nickname = binding.plantNameInput.text.toString()   // CHANGED: nickname = plantNameInput
            val type = binding.plantTypeInput.text.toString()       // CHANGED: type = plantTypeInput
            val lastDate = binding.lastWateredDateInput.text.toString()
            val nextDate = binding.nextWateringDateInput.text.toString()
            val fruitProductionRate = binding.fruitRateInput.text.toString().ifBlank { "" }
            val flowerColor = binding.flowerColorInput.text.toString()
            val wateringAmount = binding.wateringAmountInput.text.toString().ifBlank { "0" }
            val location = binding.locationInput.text.toString()
            val now = LocalDate.now()

            if (capturedImage == null) {
                Toast.makeText(this, "Please add a photo.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (nickname.isBlank() || type.isBlank() || lastDate.isBlank() || nextDate.isBlank()) {
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val inputFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
            val newLastWaterDate = LocalDate.parse(lastDate, inputFormatter)
            val newNextWaterDate = LocalDate.parse(nextDate, inputFormatter)

            val wateringFrequency =
                java.time.temporal.ChronoUnit.DAYS.between(newLastWaterDate, newNextWaterDate).toInt()

            if (wateringFrequency <= 0) {
                Toast.makeText(this, "Next watering must be after last watering", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val imagePath = imageToStorage(capturedImage!!)
            if (imagePath == null) {
                Toast.makeText(this, "Failed to save image.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // ADDED: format dates before saving to Firestore
            val dateCreatedStr = now.format(firestoreFormatter)
            val lastWaterStr = newLastWaterDate.format(firestoreFormatter)
            val nextWaterStr = newNextWaterDate.format(firestoreFormatter)

            val db = FirebaseFirestore.getInstance()
            val sp = getSharedPreferences("FlorPal_User_Prefs", MODE_PRIVATE)
            var userId = sp.getString("user_id", null)

            val plantDB = db.collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)

            val plantData = hashMapOf(
                FlorPal_FireStoreRefs.USER_ID_FIELD to userId,
                FlorPal_FireStoreRefs.DATE_CREATED_FIELD to dateCreatedStr,     // CHANGED
                FlorPal_FireStoreRefs.NAME_FIELD to type,                       // CHANGED: store plant type
                FlorPal_FireStoreRefs.NICKNAME_FIELD to nickname,               // CHANGED: store nickname
                FlorPal_FireStoreRefs.PLANT_PHOTO_FIELD to imagePath,
                FlorPal_FireStoreRefs.FLOWER_COLOR_FIELD to flowerColor,
                FlorPal_FireStoreRefs.FRUIT_PRODUCTION_FIELD to fruitProductionRate,
                FlorPal_FireStoreRefs.WATERING_AMOUT_FIELD to wateringAmount.toDouble(),
                FlorPal_FireStoreRefs.LOCATION_FIELD to location,
                FlorPal_FireStoreRefs.WATERED_DATE_FIELD to lastWaterStr,       // CHANGED
                FlorPal_FireStoreRefs.NEXT_WATER_DATE_FIELD to nextWaterStr,    // CHANGED
                FlorPal_FireStoreRefs.FAVORITED_FIELD to false,
                FlorPal_FireStoreRefs.WATERING_FREQUENCY_FIELD to wateringFrequency
            )

            plantDB.add(plantData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Plant added successfully!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to add plant. Reason: $e", Toast.LENGTH_SHORT).show()
                }

            setResult(Activity.RESULT_OK)
            finish()
        }
    }

    private fun imageToStorage(uri: Uri): String? {
        return try {
            val fileName = "plant_${UUID.randomUUID()}.jpg"
            val directory = File(filesDir, "plant_images")
            if (!directory.exists()) directory.mkdirs()

            val file = File(directory, fileName)
            val inputStream = contentResolver.openInputStream(uri) ?: return null

            inputStream.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
