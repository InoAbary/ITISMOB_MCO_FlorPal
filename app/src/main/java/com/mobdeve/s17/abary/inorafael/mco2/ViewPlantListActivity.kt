package com.mobdeve.s17.abary.inorafael.mco2

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
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

    private var firstOpen = true




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ViewPlantListBinding.inflate(layoutInflater)
        setContentView(binding.root)



        rvReminders = binding.plantsRecyclerView
        // reminderAdapter = WaterReminderAdapter(reminderList)
        // edited nov 5
        reminderAdapter = WaterReminderAdapter(reminderList, plantList, enableClick = true, this) {loadPlants()}
        rvReminders.adapter = reminderAdapter
        rvReminders.layoutManager = LinearLayoutManager(this)

        binding.backBtn.setOnClickListener {

            val data = Intent()
            data.putExtra("RESET_FILTER", true)
            setResult(Activity.RESULT_OK, data)
            finish()

        }

        // added nov 5
        binding.addPlantBtn.setOnClickListener {
            binding.addPlantBtn.setOnClickListener {
                val intent = Intent(this, AddPlantActivity::class.java)
                startActivity(intent)
            }
        }

        // spotify
        binding.btnSpotify.setOnClickListener {
            playRandomSpotify()
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

        // prefill drawer based on main page filter BEFORE you load data
        val mainSp = getSharedPreferences("FLORPAL_MAIN_FILTER", MODE_PRIVATE)
        when (mainSp.getString("MAIN_FILTER_VALUE", "ALL")) {
            "FAV" -> filterBinding.filterFavsCb.isChecked = true
            "TODAY" -> filterBinding.filterWaterTodayCb.isChecked = true
            "WEEK" -> filterBinding.filterWaterWeekCb.isChecked = true
            "PAST" -> filterBinding.filterPastDueCb.isChecked = true
        }

        applyFilters()

        if (firstOpen) {
            val sp = getSharedPreferences("FLORPAL_FILTEROPTIONS", MODE_PRIVATE)
            val editor = sp.edit()

            when (mainSp.getString("MAIN_FILTER_VALUE", "ALL")) {
                "FAV" -> {
                    filterBinding.filterFavsCb.isChecked = true
                    editor.putBoolean("FLORPAL_FAVORITES", true)
                }
                "TODAY" -> {
                    filterBinding.filterWaterTodayCb.isChecked = true
                    editor.putBoolean("FLORPAL_WATERTODAY", true)
                }
                "WEEK" -> {
                    filterBinding.filterWaterWeekCb.isChecked = true
                    editor.putBoolean("FLORPAL_WATERTHISWEEK", true)
                }
                "PAST" -> {
                    filterBinding.filterPastDueCb.isChecked = true
                    editor.putBoolean("FLORPAL_PASTDUE", true)
                }
            }

            editor.apply()
        }


        loadPlants()

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

    // --- PLAY RANDOM SPOTIFY TRACK
    private fun playRandomSpotify() {
        val randomTrack = SpotifyTracks.allTracks.random()
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(randomTrack))
            intent.setPackage("com.spotify.music")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Spotify app is not installed.", Toast.LENGTH_SHORT).show()
        }
    }

    fun applyFilters(){
        var filteredList = ArrayList(reminderList)

        // favorites filter
        if (filterBinding.filterFavsCb.isChecked){
            filteredList = ArrayList(filteredList.filter{it.plant.favorite})
        }

        // past due filter
        if (filterBinding.filterPastDueCb.isChecked){
            filteredList = ArrayList(filteredList.filter{it.lastWateredDaysAgo > 0})
        }

        // due today filter
        if (filterBinding.filterWaterTodayCb.isChecked){
            filteredList = ArrayList(filteredList.filter{it.lastWateredDaysAgo == 0})
        }

        // ADDED: due within next 7 days filter
        // NOTE: lastWateredDaysAgo is diff = days between nextWaterDate and today
        // upcoming within 7 days => diff in [-7, 0] ( example: nextWaterDate is within 7 days including today)
        if (filterBinding.filterWaterWeekCb.isChecked) {
            filteredList = ArrayList(filteredList.filter { it.lastWateredDaysAgo <= 0 && it.lastWateredDaysAgo >= -7 })
        }

        val checkedId = filterBinding.sortRadioGroup.checkedRadioButtonId

        if (checkedId != -1) {
            val checkedBtn = filterBinding.root.findViewById<RadioButton?>(checkedId)

            if (checkedBtn != null) {
                when (checkedBtn.text.toString()) {
                    "Alphabetical Ascending" -> filteredList.sortBy { it.plant.plantNickName }
                    "Alphabetical Descending" -> filteredList.sortByDescending { it.plant.plantNickName }
                    "Last Watered" -> filteredList.sortBy { it.lastWateredDaysAgo }
                }
            } else {
                Log.e("FILTER", "Sort RadioButton ID not found in layout!")
            }
        }



        reminderAdapter.updateData(filteredList)
    }

    override fun onResume() {
        super.onResume()

        if (!firstOpen) {
            val sp = getSharedPreferences("FLORPAL_FILTEROPTIONS", MODE_PRIVATE)
            filterBinding.filterFavsCb.isChecked = sp.getBoolean("FLORPAL_FAVORITES", false)
            filterBinding.filterPastDueCb.isChecked = sp.getBoolean("FLORPAL_PASTDUE", false)
            filterBinding.filterWaterTodayCb.isChecked = sp.getBoolean("FLORPAL_WATERTODAY", false)
            filterBinding.filterWaterWeekCb.isChecked = sp.getBoolean("FLORPAL_WATERTHISWEEK", false)
            filterBinding.sortRadioGroup.check(sp.getInt("FLORPAL_RADIOBUTTON", -1))
        }

        firstOpen = false

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
        editor.putInt("FLORPAL_RADIOBUTTON", filterBinding.sortRadioGroup.checkedRadioButtonId)
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
                    doc.getString(FlorPal_FireStoreRefs.PLANT_PHOTO_FIELD)!!.toString(),
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

    private fun applyMainFilterCarryOver(filter: String?): ArrayList<WaterReminderModel> {

        var filtered = ArrayList(reminderList)

        when (filter) {
            "FAV" -> filtered = ArrayList(filtered.filter { it.plant.favorite })
            "TODAY" -> filtered = ArrayList(filtered.filter { it.lastWateredDaysAgo == 0 })
            "WEEK" -> filtered = ArrayList(filtered.filter { it.lastWateredDaysAgo in -7..0 })
            "PAST" -> filtered = ArrayList(filtered.filter { it.lastWateredDaysAgo > 0 })
            else -> {  }
        }

        return filtered
    }




}
