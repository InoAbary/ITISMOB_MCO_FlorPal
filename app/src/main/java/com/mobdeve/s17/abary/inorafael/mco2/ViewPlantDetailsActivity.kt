package com.mobdeve.s17.abary.inorafael.mco2

// nov 22 ver + corrected date handling + Unknown fallback
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.google.firebase.firestore.FirebaseFirestore
import com.mobdeve.s17.abary.inorafael.mco2.databinding.ViewPlantDetailsBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

class ViewPlantDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ViewPlantDetailsBinding

    private val editPlantLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                reloadPlantFromFirestore()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ViewPlantDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val plant = intent.getSerializableExtra("plantModel") as? PlantModel
        if (plant != null) {
            displayPlantDetails(plant)
            setUrgencyBackground(plant)
            setupWaterButton(plant)
        }

        binding.backBtn.setOnClickListener { finish() }

        binding.editPlantBtn.setOnClickListener {
            val intent = Intent(this, EditPlantActivity::class.java)
            intent.putExtra("plantModel", plant)
            editPlantLauncher.launch(intent)
        }

        binding.deletePlantBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Plant")
                .setMessage("Are you sure you want to delete this plant?")
                .setPositiveButton("Yes") { _, _ ->
                    FirebaseFirestore.getInstance()
                        .collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)
                        .document(plant!!.plant_id!!)
                        .delete()

                    Toast.makeText(this, "Plant deleted successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun displayPlantDetails(plant: PlantModel) {

        if (!plant.plantPhoto.isNullOrBlank()) {
            val uri = plant.plantPhoto.toUri()
            binding.plantImg.setImageURI(uri)
        }

        binding.plantNameTv.text = plant.plantNickName
        binding.plantTypeTv.text = plant.plantName.ifBlank { "Unknown" }

        binding.fruitProductionTv.text =
            plant.fruitProductionRate?.ifBlank { "Unknown" } ?: "Unknown"

        binding.flowerColorTv.text =
            plant.flowerColor.ifBlank { "Unknown" }

        binding.locationTv.text =
            plant.location?.ifBlank { "Unknown" } ?: "Unknown"

        binding.wateringAmountTv.text =
            if (plant.wateringAmount == null || plant.wateringAmount == 0.0)
                "Unknown"
            else
                "${plant.wateringAmount} ml"

        // date created
        binding.dateCreatedTv.text =
            "${plant.dateCreated.monthName} ${plant.dateCreated.day}, ${plant.dateCreated.year}"

        // last watered
        binding.lastWateredTv.text =
            "${plant.wateredDate.monthName} ${plant.wateredDate.day}, ${plant.wateredDate.year}"

        // next watering schedule
        if (plant.nextWateredDate != null) {
            val next = plant.nextWateredDate!!
            binding.nextWateringSchedTv.text =
                "${next.monthName} ${next.day}, ${next.year}"
        } else {
            binding.nextWateringSchedTv.text = "Unknown"
        }
    }

    private fun setupWaterButton(plant: PlantModel) {
        binding.waterBtn.setOnClickListener {

            val db = FirebaseFirestore.getInstance()
            val plantId = plant.plant_id ?: return@setOnClickListener

            val today = LocalDate.now()

            val dateFormatter = DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("MMMM d, yyyy")
                .toFormatter(Locale.ENGLISH)

            db.collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)
                .document(plantId)
                .get()
                .addOnSuccessListener { doc ->

                    val freq = doc.getLong(FlorPal_FireStoreRefs.WATERING_FREQUENCY_FIELD)?.toInt() ?: 0

                    val nextWaterDate =
                        if (freq > 0) today.plusDays(freq.toLong())
                        else today

                    db.collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)
                        .document(plantId)
                        .update(
                            FlorPal_FireStoreRefs.WATERED_DATE_FIELD, today.format(dateFormatter),
                            FlorPal_FireStoreRefs.NEXT_WATER_DATE_FIELD, nextWaterDate.format(dateFormatter)
                        )
                        .addOnSuccessListener {
                            Toast.makeText(this, "Plant watered!", Toast.LENGTH_SHORT).show()

                            plant.wateredDate =
                                CustomDate(today.month.toString(), today.dayOfMonth, today.year)

                            plant.nextWateredDate =
                                CustomDate(nextWaterDate.month.toString(), nextWaterDate.dayOfMonth, nextWaterDate.year)

                            binding.lastWateredTv.text =
                                "${plant.wateredDate.monthName} ${plant.wateredDate.day}, ${plant.wateredDate.year}"

                            val next = plant.nextWateredDate!!
                            binding.nextWateringSchedTv.text =
                                "${next.monthName} ${next.day}, ${next.year}"

                            setUrgencyBackground(plant)
                        }
                }
        }
    }

    private fun setUrgencyBackground(plant: PlantModel) {

        val baseDate =
            if (plant.nextWateredDate != null)
                LocalDate.of(plant.nextWateredDate!!.year, plant.nextWateredDate!!.monthInt, plant.nextWateredDate!!.day)
            else
                LocalDate.of(plant.wateredDate.year, plant.wateredDate.monthInt, plant.wateredDate.day)

        val today = LocalDate.now()

        // CHANGED: use ChronoUnit.DAYS instead of Period.days
        val diff = java.time.temporal.ChronoUnit.DAYS.between(baseDate, today).toInt()   // CHANGED

        val colorRes = when {
            diff == 0 -> R.color.yellow_upcoming
            diff > 0 -> R.color.red_overdue
            else -> R.color.green_due_today
        }

        val bgColor = ContextCompat.getColor(this, colorRes)
        binding.root.setBackgroundColor(bgColor)
    }

    private fun reloadPlantFromFirestore() {

        val original = intent.getSerializableExtra("plantModel") as? PlantModel ?: return
        val plantId = original.plant_id ?: return

        val db = FirebaseFirestore.getInstance()

        db.collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)
            .document(plantId)
            .get()
            .addOnSuccessListener { doc ->
                if (!doc.exists()) return@addOnSuccessListener

                // reconstruct updated PlantModel
                val dateFormatter = DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("MMMM d, yyyy")
                    .toFormatter(Locale.ENGLISH)

                val newPlant = PlantModel(
                    doc.getString(FlorPal_FireStoreRefs.NICKNAME_FIELD) ?: "",
                    doc.getString(FlorPal_FireStoreRefs.NAME_FIELD) ?: "",
                    doc.getString(FlorPal_FireStoreRefs.PLANT_PHOTO_FIELD) ?: "",
                    doc.getString(FlorPal_FireStoreRefs.FRUIT_PRODUCTION_FIELD) ?: "",
                    doc.getString(FlorPal_FireStoreRefs.FLOWER_COLOR_FIELD) ?: "",
                    // created date
                    CustomDate.fromString(doc.getString(FlorPal_FireStoreRefs.DATE_CREATED_FIELD)!!),
                    // last watered
                    CustomDate.fromString(doc.getString(FlorPal_FireStoreRefs.WATERED_DATE_FIELD)!!),
                    doc.getDouble(FlorPal_FireStoreRefs.WATERING_AMOUT_FIELD),
                    doc.getString(FlorPal_FireStoreRefs.LOCATION_FIELD),
                    doc.getBoolean(FlorPal_FireStoreRefs.FAVORITED_FIELD) ?: false,
                    // next water
                    CustomDate.fromString(doc.getString(FlorPal_FireStoreRefs.NEXT_WATER_DATE_FIELD)!!)
                ).apply {
                    plant_id = plantId
                }

                // update UI instantly
                displayPlantDetails(newPlant)
                setUrgencyBackground(newPlant)
            }
    }
}
