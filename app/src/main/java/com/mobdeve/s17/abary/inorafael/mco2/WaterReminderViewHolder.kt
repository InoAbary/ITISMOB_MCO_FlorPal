package com.mobdeve.s17.abary.inorafael.mco2

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale

class WaterReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val ivPlantPhoto: ImageView = itemView.findViewById(R.id.ivPlantPhoto)
    private val tvPlantNickName: TextView = itemView.findViewById(R.id.tvPlantNickName)
    private val tvPlantName: TextView = itemView.findViewById(R.id.tvPlantName)
    private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
    private val tvHeart: TextView = itemView.findViewById(R.id.tvHeart)

    val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
    val btnWater: Button = itemView.findViewById(R.id.btnWater)

    private fun toggleFavorite(plant: PlantModel) {
        val db = FirebaseFirestore.getInstance()
        db.collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)
            .document(plant.plant_id!!)
            .update(FlorPal_FireStoreRefs.FAVORITED_FIELD, plant.favorite)
    }

    fun bindData(
        reminder: WaterReminderModel,
        plantList: ArrayList<PlantModel>,
        enableClick: Boolean,
        activity: Activity,
        onWatered: () -> Unit
    ) {

        // display photo
        if (!reminder.plant.plantPhoto.isNullOrBlank()) {
            ivPlantPhoto.setImageURI(reminder.plant.plantPhoto.toUri())
        }

        tvPlantNickName.text = reminder.plant.plantNickName
        tvPlantName.text = reminder.plant.plantName
        tvStatus.text = reminder.statusText

        tvHeart.text = if (reminder.plant.favorite) "♥" else "\uD83E\uDD0D"

        itemView.background = itemView.context.getDrawable(R.drawable.round_image_bg)
        itemView.background.setTint(reminder.cardColor)



        tvHeart.setOnClickListener {
            reminder.plant.favorite = !reminder.plant.favorite
            tvHeart.text = if (reminder.plant.favorite) "♥" else "\uD83E\uDD0D"
            toggleFavorite(reminder.plant)
        }

        // Edit button
        btnEdit.setOnClickListener {
            val matchedPlant = plantList.find { it.plantNickName == reminder.plant.plantNickName }
            if (matchedPlant != null) {
                val intent = Intent(activity, EditPlantActivity::class.java)
                intent.putExtra("plantModel", matchedPlant)
                activity.startActivity(intent)
            }
        }

        // Water Button
        btnWater.setOnClickListener {
            val db = FirebaseFirestore.getInstance()
            val plantId = reminder.plant.plant_id ?: return@setOnClickListener

            val today = LocalDate.now()

            val dateFormatter = java.time.format.DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("MMMM d, yyyy")
                .toFormatter(Locale.ENGLISH)

            db.collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)
                .document(plantId)
                .get()
                .addOnSuccessListener { doc ->

                    val freq = doc.getLong(FlorPal_FireStoreRefs.WATERING_FREQUENCY_FIELD)?.toInt() ?: 0
                    val nextDate = today.plusDays(freq.toLong())

                    // CHANGED: fixed Firestore update syntax using mapOf
                    db.collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)
                        .document(plantId)
                        .update(
                            mapOf(
                                FlorPal_FireStoreRefs.WATERED_DATE_FIELD to today.format(dateFormatter),
                                FlorPal_FireStoreRefs.NEXT_WATER_DATE_FIELD to nextDate.format(dateFormatter)
                            )
                        )
                        .addOnSuccessListener {
                            Toast.makeText(activity, "Plant watered!", Toast.LENGTH_SHORT).show()

                            // ADDED: update local watered date
                            reminder.plant.wateredDate = CustomDate(
                                today.month.toString(),
                                today.dayOfMonth,
                                today.year
                            )

                            // ADDED: update local nextWateredDate
                            reminder.plant.nextWateredDate = CustomDate(
                                nextDate.month.toString(),
                                nextDate.dayOfMonth,
                                nextDate.year
                            )

                            // ADDED: compute updated status
                            val diff = ChronoUnit.DAYS.between(nextDate, today).toInt()
                            tvStatus.text = when {
                                diff > 0 -> "${diff} day(s) overdue"
                                diff == 0 -> "Water today"
                                else -> "Water in ${-diff} day(s)"
                            }

                            onWatered() // refresh list
                        }
                }
        }

        // open details screen when card is clicked
        if (enableClick) {
            itemView.setOnClickListener {
                val matchedPlant =
                    plantList.find { it.plantNickName == reminder.plant.plantNickName }
                if (matchedPlant != null) {
                    val intent = Intent(activity, ViewPlantDetailsActivity::class.java)
                    intent.putExtra("plantModel", matchedPlant)
                    activity.startActivity(intent)
                }
            }
        } else {
            itemView.setOnClickListener(null)
        }
    }
}
