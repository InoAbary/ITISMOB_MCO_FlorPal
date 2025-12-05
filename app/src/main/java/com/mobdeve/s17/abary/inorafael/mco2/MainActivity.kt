package com.mobdeve.s17.abary.inorafael.mco2

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.mobdeve.s17.abary.inorafael.mco2.databinding.MainpageBinding
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var mainBinding: MainpageBinding
    private lateinit var rvPlants: RecyclerView
    private lateinit var rvReminders: RecyclerView
    private lateinit var plantAdapter: PlantAdapter
    private lateinit var reminderAdapter: WaterReminderAdapter
    private var plantList = ArrayList<PlantModel>()
    private var reminderList: ArrayList<WaterReminderModel> = ArrayList()
    private val handler = Handler(Looper.getMainLooper())
    private val FILTER_SP = "FLORPAL_MAIN_FILTER"
    private val KEY_FILTER = "MAIN_FILTER_VALUE"

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val reset = result.data?.getBooleanExtra("RESET_FILTER", false) ?: false
            if (reset) {
                val sp = getSharedPreferences(FILTER_SP, MODE_PRIVATE)
                sp.edit().putString(KEY_FILTER, "ALL").apply()
            }
            loadPlants()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = MainpageBinding.inflate(layoutInflater)
        setContentView(mainBinding.root)

        // Logo overlay
        val logoOverlay = findViewById<View>(R.id.logoOverlay)

        // Spotify Button
        mainBinding.btnSpotify.setOnClickListener {
            playRandomSpotify()
        }

        // Notifications
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }
        FlorPalNotificationHelper.createNotificationChannel(this)
        FlorPalAlarmScheduler.scheduleDailyAlarm(this)

        // RecyclerViews
        rvPlants = mainBinding.plantsRecyclerView
        plantAdapter = PlantAdapter(plantList)
        rvPlants.adapter = plantAdapter
        rvPlants.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        rvReminders = mainBinding.remindersRecyclerView
        reminderAdapter = WaterReminderAdapter(reminderList, plantList, enableClick = false, this) {
            loadPlants()
        }
        rvReminders.adapter = reminderAdapter
        rvReminders.layoutManager = LinearLayoutManager(this)

        // Load data
        loadPlants()

        // Buttons
        mainBinding.viewPlantsBtn.setOnClickListener {
            val i = Intent(this, ViewPlantListActivity::class.java)
            activityResultLauncher.launch(i)
        }
        mainBinding.addPlantBtn.setOnClickListener {
            activityResultLauncher.launch(Intent(this, AddPlantActivity::class.java))
        }
        mainBinding.filterBtn.setOnClickListener {
            showFilterPopup()
        }

        // Carousel
        startSmoothCarousel()

        // Hide logo after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            logoOverlay.animate()
                .alpha(0f)
                .setDuration(800)
                .withEndAction {
                    logoOverlay.visibility = View.GONE
                }
                .start()
        }, 1000)
    }

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


    private fun loadPlants() {
        val sp = getSharedPreferences("FlorPal_User_Prefs", MODE_PRIVATE)
        var userId = sp.getString("user_id", null)
        if (userId == null) {
            userId = java.util.UUID.randomUUID().toString()
            sp.edit().putString("user_id", userId).apply()
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
                updateRemindersView()
                return@addOnSuccessListener
            }

            for (doc in result) {
                val rawPhoto = doc.get(FlorPal_FireStoreRefs.PLANT_PHOTO_FIELD)
                Log.e("DEBUG", "raw plantPhoto = $rawPhoto, type = ${rawPhoto?.javaClass}")

                val photoPath: String = when (rawPhoto) {
                    is String -> rawPhoto
                    is Number -> rawPhoto.toString()
                    null -> ""
                    else -> rawPhoto.toString()
                }

                val dateCreated = doc.getString(FlorPal_FireStoreRefs.DATE_CREATED_FIELD) ?: "N/A"
                val wateredDate = doc.getString(FlorPal_FireStoreRefs.WATERED_DATE_FIELD) ?: "N/A"
                val nextDate = doc.getString(FlorPal_FireStoreRefs.NEXT_WATER_DATE_FIELD) ?: "N/A"

                val newDateCreated = LocalDate.parse(dateCreated, dateFormatter)
                val newLastWaterDate = LocalDate.parse(wateredDate, dateFormatter)
                val newNextWaterDate = LocalDate.parse(nextDate, dateFormatter)

                val plant = PlantModel(
                    doc.getString(FlorPal_FireStoreRefs.NICKNAME_FIELD) ?: "",
                    doc.getString(FlorPal_FireStoreRefs.NAME_FIELD) ?: "",
                    photoPath,
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
            applyMainFilterToReminders()
            updateRemindersView()
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to get plants. Reason: ${e}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateRemindersView() {
        if (reminderList.isEmpty()) {
            mainBinding.emptyRemindersIv.visibility = View.VISIBLE
            mainBinding.remindersRecyclerView.visibility = View.GONE
        } else {
            mainBinding.emptyRemindersIv.visibility = View.GONE
            mainBinding.remindersRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun startSmoothCarousel() {
        val layoutManager = rvPlants.layoutManager as LinearLayoutManager
        val scrollSpeed = 2

        handler.post(object : Runnable {
            override fun run() {
                rvPlants.scrollBy(scrollSpeed, 0)
                if (rvPlants.computeHorizontalScrollOffset() + rvPlants.width >= rvPlants.computeHorizontalScrollRange()) {
                    rvPlants.scrollToPosition(0)
                }
                handler.postDelayed(this, 16)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        val sp = getSharedPreferences(FILTER_SP, MODE_PRIVATE)
        if (intent.getBooleanExtra("RESET_FILTER", false)) {
            sp.edit().putString(KEY_FILTER, "ALL").apply()
            intent.removeExtra("RESET_FILTER")
        }
        loadPlants()
    }

    private fun applyMainFilterToReminders() {
        val sp = getSharedPreferences(FILTER_SP, MODE_PRIVATE)
        val filter = sp.getString(KEY_FILTER, "ALL") ?: "ALL"
        var filtered = ArrayList(reminderList)

        when (filter) {
            "ALL" -> mainBinding.viewPlantsBtn.text = "View: All Plants"
            "FAV" -> {
                filtered = ArrayList(filtered.filter { it.plant.favorite })
                mainBinding.viewPlantsBtn.text = "View: Favorites"
            }
            "TODAY" -> {
                filtered = ArrayList(filtered.filter { it.lastWateredDaysAgo == 0 })
                mainBinding.viewPlantsBtn.text = "View: Watering Today"
            }
            "WEEK" -> {
                filtered = ArrayList(filtered.filter { it.lastWateredDaysAgo <= 0 && it.lastWateredDaysAgo >= -7 })
                mainBinding.viewPlantsBtn.text = "View: Watering This Week"
            }
            "PAST" -> {
                filtered = ArrayList(filtered.filter { it.lastWateredDaysAgo > 0 })
                mainBinding.viewPlantsBtn.text = "View: Past Due"
            }
        }

        reminderAdapter.updateData(filtered)
    }

    private fun showFilterPopup() {
        val popupView = layoutInflater.inflate(R.layout.filter_popup, null)
        popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)

        val popupWindow = PopupWindow(
            popupView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.isOutsideTouchable = true
        popupWindow.elevation = 20f
        popupWindow.showAsDropDown(
            mainBinding.filterBtn,
            0,
            -(popupView.measuredHeight + mainBinding.filterBtn.height)
        )

        val group = popupView.findViewById<RadioGroup>(R.id.mainFilterRadioGroup)
        val applyBtn = popupView.findViewById<Button>(R.id.applyBtn)

        val sp = getSharedPreferences(FILTER_SP, MODE_PRIVATE)
        when (sp.getString(KEY_FILTER, "ALL")) {
            "ALL" -> group.check(R.id.filterAllRb)
            "FAV" -> group.check(R.id.filterFavsRb)
            "TODAY" -> group.check(R.id.filterTodayRb)
            "WEEK" -> group.check(R.id.filterWeekRb)
            "PAST" -> group.check(R.id.filterPastRb)
        }

        applyBtn.setOnClickListener {
            val selected = when (group.checkedRadioButtonId) {
                R.id.filterFavsRb -> "FAV"
                R.id.filterTodayRb -> "TODAY"
                R.id.filterWeekRb -> "WEEK"
                R.id.filterPastRb -> "PAST"
                else -> "ALL"
            }
            sp.edit().putString(KEY_FILTER, selected).apply()
            popupWindow.dismiss()
            loadPlants()
        }
    }
}
