package com.mobdeve.s17.abary.inorafael.mco2

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.google.firebase.firestore.FirebaseFirestore
import com.mobdeve.s17.abary.inorafael.mco2.databinding.EditPlantBinding
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.FormatFlagsConversionMismatchException
import java.util.Locale

class EditPlantActivity : AppCompatActivity() {

    private lateinit var binding: EditPlantBinding
    private var capturedImage: Uri? = null


    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
            result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri = result.data?.data ?: result.data?.getStringExtra("captured_image")?.toUri()

            uri?.let {

                try {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: SecurityException) {

                }

                capturedImage = it
                binding.plantImg.setImageURI(capturedImage)
            }

        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditPlantBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val dateFormatterIn = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMMM d, yyyy")
            .toFormatter(Locale.ENGLISH)

        val dateFormatterOut = DateTimeFormatter.ofPattern("MM/dd/yyyy")




        val plant = intent.getSerializableExtra("plantModel") as? PlantModel
        if (plant != null) {

            if (!plant.plantPhoto.isNullOrBlank()) {
                val uri = plant.plantPhoto.toUri()
                capturedImage = uri
                binding.plantImg.setImageURI(uri)
            }

            binding.plantNameTv.setText(plant.plantNickName)


            binding.wateringAmountInput.setText(plant.wateringAmount?.toString() ?: "")
            binding.locationInput.setText(plant.location ?: "")
            binding.fruitRateInput.setText(plant.fruitProductionRate ?: "")
            binding.flowerColorInput.setText(plant.flowerColor ?: "")

            var lastWateredDate = LocalDate.parse(plant.wateredDate.toString(), dateFormatterIn)
            lastWateredDate.format(dateFormatterOut)

            binding.lastWateredDateInput.setText(lastWateredDate.toString())

        }

        // temporary values for date/time fields. change later to get actual data
        // to fix later: date and time only show up after clicking
        // on the fields. fix this
        binding.lastWateredDateInput.setOnClickListener {
            DatePicker { formatted ->
                binding.lastWateredDateInput.setText(formatted)
            }.show(supportFragmentManager, "lastWateredDate")
        }



        binding.addPhotoIcon.setOnClickListener {

            val intent = Intent(this, CameraActivity::class.java)
            activityResultLauncher.launch(intent)

        }

        binding.removePhotoIcon.setOnClickListener {
            capturedImage = null
            binding.plantImg.setImageURI(null)
        }

        binding.nextWateringDateInput.setOnClickListener {
            binding.nextWateringDateInput.setText("2025-11-08")
        }

        // image buttons
        // nothing yet




        // cancel button
        binding.cancelBtn.setOnClickListener {
            finish()
        }

        // save Changes button
        // to implement: input validation (especially on watering scheds)
        binding.addPlantBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Confirm Changes")
                .setMessage("Are you sure you want to save these changes?")
                .setPositiveButton("Yes") { _, _ ->
                    val db = FirebaseFirestore.getInstance()
                    val userRef = db.collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)

                    val updates = hashMapOf<String, Any>(
                        FlorPal_FireStoreRefs.NICKNAME_FIELD to binding.plantNameTv.text.toString(),
                        FlorPal_FireStoreRefs.LOCATION_FIELD to binding.locationInput.text.toString(),
                        FlorPal_FireStoreRefs.WATERING_AMOUT_FIELD to binding.wateringAmountInput.text.toString().toDouble(),
                        FlorPal_FireStoreRefs.FRUIT_PRODUCTION_FIELD to binding.fruitRateInput.text.toString(),
                        FlorPal_FireStoreRefs.FLOWER_COLOR_FIELD to binding.flowerColorInput.text.toString()

                    )

                    capturedImage?.let{
                        updates[FlorPal_FireStoreRefs.PLANT_PHOTO_FIELD] = it.toString()
                    }

                    userRef.document(plant!!.plant_id!!).update(updates)
                    Toast.makeText(this, "Changes saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }
}
