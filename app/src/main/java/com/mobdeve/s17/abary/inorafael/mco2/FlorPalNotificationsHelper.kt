package com.mobdeve.s17.abary.inorafael.mco2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object FlorPalNotificationHelper {

    private const val CHANNEL_ID = "FlorPalChannel"
    private const val CHANNEL_NAME = "Plant Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showWaterTodayNotification(context: Context, plantNickname: String) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Water Today!")
            .setContentText("Don't forget to water $plantNickname today!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // hashCode for unique ID per plant
        manager.notify(plantNickname.hashCode(), builder.build())
    }

    fun showOverdueNotification(context: Context, plantNickname: String, overdueDays: Int) {
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Plant Overdue")
            .setContentText("$plantNickname is overdue by $overdueDays day(s)! Please water it soon. It feels neglected :((.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify((plantNickname + "_overdue").hashCode(), builder.build())
    }
}
