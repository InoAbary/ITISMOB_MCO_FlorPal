package com.mobdeve.s17.abary.inorafael.mco2

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.google.firebase.firestore.FirebaseFirestore
import com.mobdeve.s17.abary.inorafael.mco2.databinding.EditPlantBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

class EditPlantActivity : AppCompatActivity() {

    private lateinit var binding: EditPlantBinding
    private var capturedImage: Uri? = null

    // CHANGED: so camera/gallery activity now correctly reads returned URI
    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uriStr = result.data?.getStringExtra("captured_image")
                capturedImage = uriStr?.toUri()
                binding.plantImg.setImageURI(capturedImage)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditPlantBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ADDED: firestore formatter "MMMM d, yyyy"
        val firestoreFormatter =
            DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)

        // ADDED:parser for firestore date to LocalDate
        val firestoreParser =
            DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("MMMM d, yyyy")
                .toFormatter(Locale.ENGLISH)

        val plant = intent.getSerializableExtra("plantModel") as? PlantModel
        if (plant != null) {

            binding.plantNameTv.setText(plant.plantNickName)

            if (!plant.plantPhoto.isNullOrBlank()) {
                capturedImage = plant.plantPhoto.toUri()
                binding.plantImg.setImageURI(capturedImage)
            }

            binding.wateringAmountInput.setText(plant.wateringAmount?.toString() ?: "")
            binding.locationInput.setText(plant.location ?: "")
            binding.fruitRateInput.setText(plant.fruitProductionRate ?: "")
            binding.flowerColorInput.setText(plant.flowerColor ?: "")

            // CHANGED: for converting firestore "MMMM d, yyyy" → LocalDate → "MM/dd/yyyy" for inputs
            val lastLocal = LocalDate.parse(plant.wateredDate.toString(), firestoreParser)
            binding.lastWateredDateInput.setText(
                lastLocal.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
            )

            val nextLocal = LocalDate.parse(plant.nextWateredDate.toString(), firestoreParser)
            binding.nextWateringDateInput.setText(
                nextLocal.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
            )
        }

        // Date pickers
        binding.lastWateredDateInput.setOnClickListener {
            DatePicker { formatted -> binding.lastWateredDateInput.setText(formatted) }
                .show(supportFragmentManager, "lastWateredDate")
        }

        binding.nextWateringDateInput.setOnClickListener {
            DatePicker { formatted -> binding.nextWateringDateInput.setText(formatted) }
                .show(supportFragmentManager, "nextWateringDate")
        }

        // Change photo
        binding.addPhotoIcon.setOnClickListener {
            val intent = Intent(this, CameraActivity::class.java)
            activityResultLauncher.launch(intent)
        }

        // Cancel
        binding.cancelBtn.setOnClickListener { finish() }

        // Save changes
        binding.addPlantBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirm Changes")
                .setMessage("Are you sure you want to save these changes?")
                .setPositiveButton("Yes") { _, _ ->

                    val nickname = binding.plantNameTv.text.toString().trim()

                    if (plant == null) return@setPositiveButton

                    val wateringAmount =
                        binding.wateringAmountInput.text.toString().toDoubleOrNull() ?: 0.0
                    val location = binding.locationInput.text.toString()
                    val fruitRate = binding.fruitRateInput.text.toString()
                    val flowerColor = binding.flowerColorInput.text.toString()

                    val lastStr = binding.lastWateredDateInput.text.toString()
                    val nextStr = binding.nextWateringDateInput.text.toString()

                    // CHANGED: read user input using "MM/dd/yyyy"
                    val inputFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")

                    val lastLocal = LocalDate.parse(lastStr, inputFormatter)
                    val nextLocal = LocalDate.parse(nextStr, inputFormatter)

                    // CHANGED: recalc frequency when either date is modified
                    val frequency =
                        java.time.temporal.ChronoUnit.DAYS.between(lastLocal, nextLocal).toInt()

                    if (frequency <= 0) {
                        Toast.makeText(
                            this,
                            "Next watering must be after last watering",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@setPositiveButton
                    }

                    val db = FirebaseFirestore.getInstance()
                    val plantId = plant.plant_id ?: return@setPositiveButton

                    // CHANGED: convert dates back to firestore format
                    val lastFS = lastLocal.format(firestoreFormatter)
                    val nextFS = nextLocal.format(firestoreFormatter)

                    val updateMap = hashMapOf(
                        FlorPal_FireStoreRefs.NICKNAME_FIELD to nickname,
                        FlorPal_FireStoreRefs.WATERING_AMOUT_FIELD to wateringAmount,
                        FlorPal_FireStoreRefs.LOCATION_FIELD to location,
                        FlorPal_FireStoreRefs.FRUIT_PRODUCTION_FIELD to fruitRate,
                        FlorPal_FireStoreRefs.FLOWER_COLOR_FIELD to flowerColor,

                        // CHANGED: formatted date strings
                        FlorPal_FireStoreRefs.WATERED_DATE_FIELD to lastFS,
                        FlorPal_FireStoreRefs.NEXT_WATER_DATE_FIELD to nextFS,
                        FlorPal_FireStoreRefs.WATERING_FREQUENCY_FIELD to frequency
                    ) as MutableMap<String, Any>

                    // CHANGED: updated photo handling
                    // does not overwrite old photo if not changed
                    if (capturedImage != null) {
                        updateMap[FlorPal_FireStoreRefs.PLANT_PHOTO_FIELD] = capturedImage.toString()
                    }

                    db.collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)
                        .document(plantId)
                        .update(updateMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Changes saved!", Toast.LENGTH_SHORT).show()
                            setResult(Activity.RESULT_OK, intent)
                            finish()
                        }
                }
                .setNegativeButton("No", null)
                .show()
        }
    }
}
