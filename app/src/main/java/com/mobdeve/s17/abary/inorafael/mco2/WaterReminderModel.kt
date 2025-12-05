package com.mobdeve.s17.abary.inorafael.mco2

// nov 22 ver
import java.time.LocalDate
import java.time.temporal.ChronoUnit   // CHANGED: use ChronoUnit
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

    // CHANGED: changed to ChronoUnit.DAYS
    private val diff: Int = ChronoUnit.DAYS.between(nextWaterDate, today).toInt()   // CHANGED

    var lastWateredDaysAgo: Int = diff


}
