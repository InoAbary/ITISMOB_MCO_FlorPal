package com.mobdeve.s17.abary.inorafael.mco2

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.mobdeve.s17.abary.inorafael.mco2.databinding.EditPlantBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.FormatFlagsConversionMismatchException
import java.util.Locale

class EditPlantActivity : AppCompatActivity() {

    private lateinit var binding: EditPlantBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = EditPlantBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val dateFormatterIn = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMMM dd, yyyy")
            .toFormatter(Locale.ENGLISH)

        val dateFormatterOut = DateTimeFormatter.ofPattern("MM/dd/yyyy")



        // retrieve plant model from intent
        val plant = intent.getSerializableExtra("plantModel") as? PlantModel
        if (plant != null) {
            // load image and name
            binding.plantImg.setImageResource(plant.plantPhoto)
            binding.plantNameTv.setText(plant.plantNickName)

            // load editable fields
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



        binding.nextWateringDateInput.setOnClickListener {
            binding.nextWateringDateInput.setText("2025-11-08")
        }

        // image buttons
        // nothing yet
        binding.addPhotoIcon.setOnClickListener {
        }

        binding.removePhotoIcon.setOnClickListener {
        }

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
                    userRef.document(plant!!.plant_id!!).update(
                        FlorPal_FireStoreRefs.NICKNAME_FIELD, binding.plantNameTv.text.toString(),
                        FlorPal_FireStoreRefs.LOCATION_FIELD, binding.locationInput.text.toString(),
                        FlorPal_FireStoreRefs.WATERING_AMOUT_FIELD, binding.wateringAmountInput.text.toString().toDouble(),
                        FlorPal_FireStoreRefs.FRUIT_PRODUCTION_FIELD, binding.fruitRateInput.text.toString(),
                        FlorPal_FireStoreRefs.FLOWER_COLOR_FIELD, binding.flowerColorInput.text.toString())
                    Toast.makeText(this, "Changes saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("No", null)
                .show()
        }
    }
}
