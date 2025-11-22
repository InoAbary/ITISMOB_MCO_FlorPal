package com.mobdeve.s17.abary.inorafael.mco2

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobdeve.s17.abary.inorafael.mco2.databinding.ViewPlantListBinding
import androidx.core.view.GravityCompat
import androidx.lifecycle.setViewTreeLifecycleOwner
import com.google.android.material.navigation.NavigationView
import com.google.firebase.firestore.FirebaseFirestore
import com.mobdeve.s17.abary.inorafael.mco2.databinding.FilterSidePanelBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

class ViewPlantListActivity: ComponentActivity (){
    private lateinit var binding: ViewPlantListBinding
    private lateinit var rvReminders: RecyclerView
    private lateinit var reminderAdapter: WaterReminderAdapter
    private lateinit var filterBinding: FilterSidePanelBinding

    private var plantList: ArrayList<PlantModel>  = ArrayList<PlantModel>()
    private var reminderList: ArrayList<WaterReminderModel> = ArrayList<WaterReminderModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ViewPlantListBinding.inflate(layoutInflater)
        setContentView(binding.root)



        rvReminders = binding.plantsRecyclerView
        // reminderAdapter = WaterReminderAdapter(reminderList)
        // edited nov 5
        reminderAdapter = WaterReminderAdapter(reminderList, plantList, enableClick = true, this)
        rvReminders.adapter = reminderAdapter
        rvReminders.layoutManager = LinearLayoutManager(this)

        loadPlants()



        binding.backBtn.setOnClickListener {

            finish()

        }

        // added nov 5
        binding.addPlantBtn.setOnClickListener {
            binding.addPlantBtn.setOnClickListener {
                val intent = Intent(this, AddPlantActivity::class.java)
                startActivity(intent)
            }
        }

        // drawer layout
        // open the drawer when the filter button is clicked
        binding.filterBtn.setOnClickListener {
            val drawer = binding.filterDrawerLayout
            if(drawer.isDrawerOpen(GravityCompat.START)){
                drawer.closeDrawer(GravityCompat.START)
            } else {
                drawer.openDrawer(GravityCompat.START)
            }
        }

        // access filter side panel
        val navView: NavigationView = binding.filterPanel
        val headerView = navView.getHeaderView(0)
        filterBinding = FilterSidePanelBinding.bind(headerView)

        // drawer just closes when apply button is clicked for now
        // clear also just clears the selections for now

        filterBinding.clearBtn.setOnClickListener {
            filterBinding.sortRadioGroup.clearCheck()
            filterBinding.filterWaterTodayCb.isChecked = false
            filterBinding.filterWaterWeekCb.isChecked = false
            filterBinding.filterPastDueCb.isChecked = false
            filterBinding.filterFavsCb.isChecked = false
        }


        filterBinding.applyBtn.setOnClickListener {
            applyFilters()
            binding.filterDrawerLayout.closeDrawer(GravityCompat.START)
        }



    }

    fun applyFilters(){
        var filteredList = ArrayList(reminderList)
        if (filterBinding.filterFavsCb.isChecked){
            filteredList = ArrayList(filteredList.filter{it.plant.favorite})
        }
        if (filterBinding.filterPastDueCb.isChecked){
            filteredList = ArrayList(filteredList.filter{it.lastWateredDaysAgo > 0})
        }

        reminderAdapter.updateData(filteredList)
    }

    override fun onResume(){
        super.onResume()
        val sp: SharedPreferences = this.getSharedPreferences("FLORPAL_FILTEROPTIONS", Context.MODE_PRIVATE)
        filterBinding.filterFavsCb.isChecked = sp.getBoolean("FLORPAL_FAVORITES", false)
        filterBinding.filterPastDueCb.isChecked = sp.getBoolean("FLORPAL_PASTDUE", false)
        filterBinding.filterWaterTodayCb.isChecked = sp.getBoolean("FLORPAL_WATERTODAY", false)
        filterBinding.filterWaterWeekCb.isChecked = sp.getBoolean("FLORPAL_WATERTHISWEEK", false)

        loadPlants()

    }
    override fun onPause(){
        super.onPause()
        val sp: SharedPreferences = this.getSharedPreferences("FLORPAL_FILTEROPTIONS", Context.MODE_PRIVATE)

        val editor: SharedPreferences.Editor = sp.edit()
        editor.putBoolean("FLORPAL_FAVORITES", filterBinding.filterFavsCb.isChecked)
        editor.putBoolean("FLORPAL_PASTDUE", filterBinding.filterPastDueCb.isChecked)
        editor.putBoolean("FLORPAL_WATERTODAY", filterBinding.filterWaterTodayCb.isChecked)
        editor.putBoolean("FLORPAL_WATERTHISWEEK", filterBinding.filterWaterWeekCb.isChecked)
        editor.apply()
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

            reminderList = DataGenerator().generateWaterReminderData(this, plantList)
            reminderAdapter.updateData(reminderList)
            applyFilters()
        }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to get plants. Reason: ${e.toString()}", Toast.LENGTH_SHORT).show()
            }

    }


}