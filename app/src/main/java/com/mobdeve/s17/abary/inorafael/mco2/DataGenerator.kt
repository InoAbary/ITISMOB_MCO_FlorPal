package com.mobdeve.s17.abary.inorafael.mco2

// nov 22 ver
import android.content.Context
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.Period

class DataGenerator {



    // generate dynamic water reminders based on next watering date
    fun generateWaterReminderData(context: Context, plants: ArrayList<PlantModel>): ArrayList<WaterReminderModel> {
        val reminderList = ArrayList<WaterReminderModel>()

        for (plant in plants) {
            val reminder = WaterReminderModel(
                plant = plant,
                statusText = "",
                cardColor = 0
            )

            val diff = reminder.lastWateredDaysAgo  // now = days past due (>, =, <)

            val (statusText, cardColorRes) = when {
                diff == 0 -> Pair("Due today!", R.color.yellow_upcoming)
                diff > 0 -> Pair("Overdue by ${diff} day(s)", R.color.red_overdue)
                else -> Pair("In ${-diff} day(s)", R.color.green_due_today)
            }

            val cardColor = ContextCompat.getColor(context, cardColorRes)

            reminder.statusText = statusText
            reminder.cardColor = cardColor

            reminderList.add(reminder)
        }

        return reminderList
    }

}
