package com.mobdeve.s17.abary.inorafael.mco2

// nov 22 ver
import android.content.Context
import androidx.core.content.ContextCompat
import java.time.LocalDate
import java.time.Period

class DataGenerator {

    fun generatePlantData(): ArrayList<PlantModel> {
        val tempList = ArrayList<PlantModel>()

        tempList.add(
            PlantModel(
                plantNickName = "Roseanne",
                plantName = "Rose",
                plantPhoto = R.drawable.rose,
                flowerColor = "Red",
                dateCreated = CustomDate("February", 14, 2025),
                wateredDate = CustomDate("October", 25, 2025), // Past due
                wateringAmount = 200.00, // added nov 5
                location = "Balcony" // added nov 5
            )
        )

        tempList.add(
            PlantModel(
                plantNickName = "Sunny",
                plantName = "Sunflower",
                plantPhoto = R.drawable.sunflower,
                fruitProductionRate = "Low",
                flowerColor = "Yellow",
                dateCreated = CustomDate("March", 21, 2025),
                wateredDate = CustomDate("November", 10, 2025), // Due today
                wateringAmount = 250.00, // added nov 5
                location = "Living Room" // added nov 5
            )
        )

        tempList.add(
            PlantModel(
                plantNickName = "Daphny",
                plantName = "Daffodil",
                plantPhoto = R.drawable.daffodil,
                fruitProductionRate = "High",
                flowerColor = "Yellow",
                dateCreated = CustomDate("April", 10, 2025),
                wateredDate = CustomDate("November", 2, 2025) // Tomorrow
            )
        )

        tempList.add(
            PlantModel(
                plantNickName = "Rosemarie",
                plantName = "Rose",
                plantPhoto = R.drawable.rose,
                fruitProductionRate = "High",
                flowerColor = "Red",
                dateCreated = CustomDate("February", 14, 2025),
                wateredDate = CustomDate("October", 25, 2025) // Past due
            )
        )

        tempList.add(
            PlantModel(
                plantNickName = "Sunshine",
                plantName = "Sunflower",
                plantPhoto = R.drawable.sunflower,
                fruitProductionRate = "Low",
                flowerColor = "Yellow",
                dateCreated = CustomDate("March", 21, 2025),
                wateredDate = CustomDate("November", 10, 2025) // Due today
            )
        )

        tempList.add(
            PlantModel(
                plantNickName = "Diana",
                plantName = "Daffodil",
                plantPhoto = R.drawable.daffodil,
                flowerColor = "Yellow",
                dateCreated = CustomDate("April", 10, 2025),
                wateredDate = CustomDate("November", 2, 2025) // Tomorrow
            )
        )

        return tempList
    }

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
