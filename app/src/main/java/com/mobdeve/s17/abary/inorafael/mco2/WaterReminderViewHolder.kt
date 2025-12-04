package com.mobdeve.s17.abary.inorafael.mco2


import android.app.Activity
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.time.LocalDate
import java.util.Locale

class WaterReminderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val ivPlantPhoto: ImageView = itemView.findViewById(R.id.ivPlantPhoto)
    private val tvPlantNickName: TextView = itemView.findViewById(R.id.tvPlantNickName)
    private val tvPlantName: TextView = itemView.findViewById(R.id.tvPlantName)
    private val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)

    private val tvHeart: TextView = itemView.findViewById(R.id.tvHeart)

    // added nov 6
    val btnEdit: Button = itemView.findViewById(R.id.btnEdit)

    // added nov 22
    val btnWater: Button = itemView.findViewById(R.id.btnWater)


    // edited nov 5
    // fun bindData(reminder: WaterReminderModel) {


    private fun toggleFavorite(plant: PlantModel){


        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)
        userRef.document(plant.plant_id!!).update(FlorPal_FireStoreRefs.FAVORITED_FIELD, plant.favorite)
    }


    fun bindData(reminder: WaterReminderModel, plantList: ArrayList<PlantModel>, enableClick: Boolean, activity: Activity, onWatered: () -> Unit) {

        if (!reminder.plant.plantPhoto.isNullOrBlank()) {
            val uri = reminder.plant.plantPhoto.toUri()
            ivPlantPhoto.setImageURI(uri)
        }
        tvPlantNickName.text = reminder.plant.plantNickName
        tvPlantName.text = reminder.plant.plantName
        tvStatus.text = reminder.statusText
        tvHeart.text = "\uD83E\uDD0D"
        if (reminder.plant.favorite)
            tvHeart.text = "♥"


        // Change background color based on reminder card color
        itemView.setBackgroundColor(reminder.cardColor)



        tvHeart.setOnClickListener {
            // Toggle heart icon ♡ → ♥
            reminder.plant.favorite = !reminder.plant.favorite
            tvHeart.text = "\uD83E\uDD0D"
            if (reminder.plant.favorite)
                tvHeart.text = "♥"

            toggleFavorite(reminder.plant)


        }

        // added nov 6
        // edit button
        btnEdit.setOnClickListener {
            val matchedPlant = plantList.find {
                it.plantNickName == reminder.plant.plantNickName
            }
            if(matchedPlant != null){
                val intent = Intent(activity, EditPlantActivity::class.java)
                intent.putExtra("plantModel", matchedPlant)
                activity.startActivity(intent)
            }
        }

        // added nov 22
        btnWater.setOnClickListener {
            val db = FirebaseFirestore.getInstance()
            val plantId = reminder.plant.plant_id ?: return@setOnClickListener

            val today = LocalDate.now()
            val dateFormatter = java.time.format.DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("MMMM d, yyyy")
                .toFormatter(Locale.ENGLISH)

            // get freq
            db.collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)
                .document(plantId)
                .get()
                .addOnSuccessListener { doc ->
                    val freq = doc.getLong(FlorPal_FireStoreRefs.WATERING_FREQUENCY_FIELD)?.toInt() ?: 0
                    val nextDate = today.plusDays(freq.toLong())

                    db.collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)
                        .document(plantId)
                        .update(
                            FlorPal_FireStoreRefs.WATERED_DATE_FIELD,
                            today.format(dateFormatter),
                            FlorPal_FireStoreRefs.NEXT_WATER_DATE_FIELD,
                            nextDate.format(dateFormatter)
                        )
                        .addOnSuccessListener {
                            Toast.makeText(activity, "Plant watered!", Toast.LENGTH_SHORT).show()

                            reminder.plant.wateredDate = CustomDate(
                                today.month.toString(),
                                today.dayOfMonth,
                                today.year
                            )

                            tvStatus.text = "Watered today"
                            onWatered()
                        }
                }
        }



        // added nov 5
        // to enable navigating to viewplantdetails activity
        // when a certain plant is clicked
        if (enableClick) {
            itemView.setOnClickListener {
                val matchedPlant = plantList.find { it.plantNickName == reminder.plant.plantNickName }
                if (matchedPlant != null) {
                    val intent = Intent(activity, ViewPlantDetailsActivity::class.java)
                    intent.putExtra("plantModel", matchedPlant)
                    activity.startActivity(intent)

                }
            }
        } else {
            itemView.setOnClickListener(null) // disables click
        }

    }
}