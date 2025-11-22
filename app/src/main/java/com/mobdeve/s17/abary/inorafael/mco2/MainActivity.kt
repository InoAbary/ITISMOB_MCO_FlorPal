package com.mobdeve.s17.abary.inorafael.mco2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.mobdeve.s17.abary.inorafael.mco2.databinding.MainpageBinding
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var mainBinding: MainpageBinding
    private lateinit var rvPlants: RecyclerView
    private lateinit var rvReminders: RecyclerView

    private lateinit var plantAdapter: PlantAdapter
    private lateinit var reminderAdapter: WaterReminderAdapter

    private var plantList = ArrayList<PlantModel>()

    private var reminderList: ArrayList<WaterReminderModel> = ArrayList<WaterReminderModel>()

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) {
        result ->
        if (result.resultCode == Activity.RESULT_OK){
            loadPlants()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainBinding = MainpageBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        // --- FOR NOTIFS
        // to ask permission first
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        // notifs + daily alarm
        FlorPalNotificationHelper.createNotificationChannel(this)
        FlorPalAlarmScheduler.scheduleDailyAlarm(this)

        rvPlants = mainBinding.plantsRecyclerView
        plantAdapter = PlantAdapter(plantList)
        rvPlants.adapter = plantAdapter
        rvPlants.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        // --- Second RecyclerView: Watering Reminders (Vertical)
        rvReminders = mainBinding.remindersRecyclerView
        reminderAdapter = WaterReminderAdapter(reminderList, plantList, enableClick = false, this) // edited nov 6
        rvReminders.adapter = reminderAdapter
        rvReminders.layoutManager = LinearLayoutManager(this)

        loadPlants()
        // ONLY FOR TESTING. change later
        // button to go to viewplantlist
         mainBinding.viewPlantsBtn.setOnClickListener {
             val intent = Intent(this, ViewPlantListActivity::class.java)
             startActivity(intent)
         }

        // nov 6 ver
        // button to go to add plant activity
        mainBinding.addPlantBtn.setOnClickListener {
            val intent = Intent(this, AddPlantActivity::class.java)
            activityResultLauncher.launch(intent)
        }



    }

    private fun loadPlants(){

        val sp = getSharedPreferences("FlorPal_User_Prefs", MODE_PRIVATE)
        var userId = sp.getString("user_id", null)

        if(userId == null){
            userId = java.util.UUID.randomUUID().toString()
            sp.edit { putString("user_id", userId) }
        }

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)
        val query = userRef.whereEqualTo(FlorPal_FireStoreRefs.USER_ID_FIELD, userId)
        val dateFormatter = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMMM d, yyyy")
            .toFormatter(Locale.ENGLISH)

        query.get().addOnSuccessListener { result ->
            plantList.clear()

            if (result.isEmpty) {
                plantAdapter.notifyDataSetChanged()

                reminderList.clear()
                reminderAdapter.notifyDataSetChanged()
                return@addOnSuccessListener
            }
            for(doc in result){


                val dateCreated = doc.getString(FlorPal_FireStoreRefs.DATE_CREATED_FIELD) ?: "N/A"
                var wateredDate = doc.getString(FlorPal_FireStoreRefs.WATERED_DATE_FIELD) ?: "N/A"

                var nextDate = doc.getString(FlorPal_FireStoreRefs.NEXT_WATER_DATE_FIELD) ?: "N/A"

                val newDateCreated = LocalDate.parse(dateCreated, dateFormatter)
                val newLastWaterDate = LocalDate.parse(wateredDate, dateFormatter)
                val newNextWaterDate = LocalDate.parse(nextDate, dateFormatter)
                var plant = PlantModel(
                    doc.getString(FlorPal_FireStoreRefs.NICKNAME_FIELD) ?: "",
                    doc.getString(FlorPal_FireStoreRefs.NAME_FIELD) ?: "",
                    doc.getLong(FlorPal_FireStoreRefs.PLANT_PHOTO_FIELD)!!.toInt(),
                    doc.getString(FlorPal_FireStoreRefs.FRUIT_PRODUCTION_FIELD) ?: "",
                    doc.getString(FlorPal_FireStoreRefs.FLOWER_COLOR_FIELD) ?: "",
                    CustomDate(newDateCreated.month.toString(), newDateCreated.dayOfMonth, newDateCreated.year),
                    CustomDate(newLastWaterDate.month.toString(), newLastWaterDate.dayOfMonth, newLastWaterDate.year),
                    doc.getDouble(FlorPal_FireStoreRefs.WATERING_AMOUT_FIELD),
                    doc.getString(FlorPal_FireStoreRefs.LOCATION_FIELD) ?: "",
                    doc.getBoolean(FlorPal_FireStoreRefs.FAVORITED_FIELD) ?: false,
                    CustomDate(newNextWaterDate.month.toString(), newNextWaterDate.dayOfMonth, newNextWaterDate.year)

                )
                plant.plant_id = doc.id
                plantList.add(plant)

            }

            plantAdapter.notifyDataSetChanged()
            reminderList = DataGenerator().generateWaterReminderData(this, plantList)
            reminderAdapter.updateData(reminderList)
        }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to get plants. Reason: ${e.toString()}", Toast.LENGTH_SHORT).show()
            }

    }

    override fun onResume(){
        super.onResume()
        loadPlants()
    }


}
