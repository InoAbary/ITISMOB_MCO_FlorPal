package com.mobdeve.s17.abary.inorafael.mco2

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import java.time.LocalDate
import java.time.Period
import java.util.Locale

class FlorPalAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        FlorPalNotificationHelper.createNotificationChannel(context)

        val sp = context.getSharedPreferences("FlorPal_User_Prefs", Context.MODE_PRIVATE)
        val userId = sp.getString("user_id", null)

        if (userId == null) {
            Log.d("FlorPalAlarmReceiver", "No user_id found.")
            return
        }

        val db = FirebaseFirestore.getInstance()
        val userRef = db.collection(FlorPal_FireStoreRefs.PLANTS_COLLECTION)

        val dateFormatter = java.time.format.DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("MMMM d, yyyy")
            .toFormatter(Locale.ENGLISH)

        val today = LocalDate.now()

        userRef.whereEqualTo(FlorPal_FireStoreRefs.USER_ID_FIELD, userId)
            .get()
            .addOnSuccessListener {
                result ->
                for (doc in result) {
                    val nickname = doc.getString(FlorPal_FireStoreRefs.NICKNAME_FIELD) ?: "Your plant"
                    val nextWaterDateStr = doc.getString(FlorPal_FireStoreRefs.NEXT_WATER_DATE_FIELD) ?: continue

                    try {
                        val nextWaterDate = LocalDate.parse(nextWaterDateStr, dateFormatter)

                        when {
                            today.isEqual(nextWaterDate) -> {
                                // due today
                                FlorPalNotificationHelper.showWaterTodayNotification(context, nickname)


                            }
                            today.isAfter(nextWaterDate) -> {
                                // overdue
                                val overdueDays = Period.between(nextWaterDate, today).days
                                FlorPalNotificationHelper.showOverdueNotification(context, nickname, overdueDays)
                            }
                            else -> {
                                // not due yet sp do nothing
                            }
                        }

                    } catch (e: Exception) {
                        Log.e("FlorPalAlarmReceiver", "Error parsing date for plant $nickname: ${e.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("FlorPalAlarmReceiver", "Failed to load plants for alarm: ${e.message}")
            }
    }
}
