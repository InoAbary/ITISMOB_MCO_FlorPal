package com.mobdeve.s17.abary.inorafael.mco2

// nov 22 ver
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s17.abary.inorafael.mco2.databinding.ViewPlantDetailsBinding
import java.time.LocalDate
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Period
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

class ViewPlantDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ViewPlantDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ViewPlantDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val plant = intent.getSerializableExtra("plantModel") as? PlantModel
        if (plant != null) {
            displayPlantDetails(plant)
            setUrgencyBackground(plant)
            setupWaterButton(plant) // nov 22
        }

        binding.backBtn.setOnClickListener {
            finish()
        }

        binding.editPlantBtn.setOnClickListener {
            val intent = Intent(this, EditPlantActivity::class.java)
            intent.putExtra("plantModel", plant)
            startActivity(intent)
        }

        binding.deletePlantBtn.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Delete Plant")
                .setMessage("Are you sure you want to delete this plant?")
                .setPositiveButton("Yes") { dialog, _ ->
                    val db = FirebaseFirestore.getInstance()
                    val userRef = db.collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)
                    userRef.document(plant!!.plant_id!!).delete()
                    Toast.makeText(this, "Plant deleted successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .setNegativeButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun displayPlantDetails(plant: PlantModel) {
        binding.plantImg.setImageResource(plant.plantPhoto)
        binding.plantNameTv.text = plant.plantNickName
        binding.plantTypeTv.text = plant.plantName
        binding.fruitProductionTv.text = plant.fruitProductionRate
        binding.flowerColorTv.text = plant.flowerColor
        binding.locationTv.text = plant.location ?: "Unknown"
        binding.wateringAmountTv.text = "${plant.wateringAmount ?: 0.0} ml"

        binding.dateCreatedTv.text =
            "${plant.dateCreated.monthName} ${plant.dateCreated.day}, ${plant.dateCreated.year}"

        binding.lastWateredTv.text =
            "${plant.wateredDate.monthName} ${plant.wateredDate.day}, ${plant.wateredDate.year}"

        //show next watering sched
        if (plant.nextWateredDate != null) {
            val next = plant.nextWateredDate!!
            binding.nextWateringSchedTv.text =
                "${next.monthName} ${next.day}, ${next.year}"
        } else {
            binding.nextWateringSchedTv.text = "Not set"
        }
    }

    // water button uses frequency and also updates both last + next date
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
                    val nextWaterDate = if (freq > 0) {
                        today.plusDays(freq.toLong())
                    } else {
                        today
                    }

                    db.collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)
                        .document(plantId)
                        .update(
                            FlorPal_FireStoreRefs.WATERED_DATE_FIELD, today.format(dateFormatter),
                            FlorPal_FireStoreRefs.NEXT_WATER_DATE_FIELD, nextWaterDate.format(dateFormatter)
                        )
                        .addOnSuccessListener {
                            Toast.makeText(this, "Plant watered!", Toast.LENGTH_SHORT).show()

                            plant.wateredDate = CustomDate(
                                today.month.toString(),
                                today.dayOfMonth,
                                today.year
                            )

                            plant.nextWateredDate = CustomDate(
                                nextWaterDate.month.toString(),
                                nextWaterDate.dayOfMonth,
                                nextWaterDate.year
                            )

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

    // similar logic from data generator
    private fun setUrgencyBackground(plant: PlantModel) {
        val baseDate = if (plant.nextWateredDate != null) {
            LocalDate.of(
                plant.nextWateredDate!!.year,
                plant.nextWateredDate!!.monthInt,
                plant.nextWateredDate!!.day
            )
        } else {
            LocalDate.of(
                plant.wateredDate.year,
                plant.wateredDate.monthInt,
                plant.wateredDate.day
            )
        }

        val today = LocalDate.now()
        val diff = java.time.Period.between(baseDate, today).days

        val colorRes = when {
            diff == 0 -> R.color.yellow_upcoming
            diff > 0 -> R.color.red_overdue
            else -> R.color.green_due_today
        }

        val bgColor = ContextCompat.getColor(this, colorRes)
        binding.root.setBackgroundColor(bgColor)
    }
}
