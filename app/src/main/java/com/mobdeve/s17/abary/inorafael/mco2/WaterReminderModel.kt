package com.mobdeve.s17.abary.inorafael.mco2

// nov 22 ver
import java.time.LocalDate
import java.time.Period

data class WaterReminderModel(
    val plant: PlantModel,
    var statusText: String,
    var cardColor: Int
) {
    private val today: LocalDate = LocalDate.now()

    private val nextWaterDate: LocalDate = LocalDate.of(
        plant.nextWateredDate!!.year,
        plant.nextWateredDate!!.monthInt,
        plant.nextWateredDate!!.day
    )

    // positive = days past due, 0 = due today, negative = days until due
    private val diff: Int = Period.between(nextWaterDate, today).days

    var lastWateredDaysAgo: Int = diff
}
