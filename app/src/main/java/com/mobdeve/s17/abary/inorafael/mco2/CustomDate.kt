package com.mobdeve.s17.abary.inorafael.mco2

// nov 22 ver
import java.io.Serializable // added nov 5

class CustomDate(month: String, day: Int, year: Int) : Serializable {

    var monthName: String = month.lowercase().replaceFirstChar { it.uppercase() }
        private set

    // Convert month name to an integer (1–12), case-insensitive
    var monthInt: Int = when (monthName.lowercase()) {
        "january" -> 1
        "february" -> 2
        "march" -> 3
        "april" -> 4
        "may" -> 5
        "june" -> 6
        "july" -> 7
        "august" -> 8
        "september" -> 9
        "october" -> 10
        "november" -> 11
        "december" -> 12
        else -> 1
    }
        private set

    var day: Int = day
        private set

    var year: Int = year
        private set

    override fun toString(): String {
        return "$monthName $day, $year"
    }

    fun isBefore(other: CustomDate): Boolean {
        return when {
            year < other.year -> true
            year == other.year && monthInt < other.monthInt -> true
            year == other.year && monthInt == other.monthInt && day < other.day -> true
            else -> false
        }
    }

    fun isSameDay(other: CustomDate): Boolean {
        return year == other.year && monthInt == other.monthInt && day == other.day
    }

    fun daysUntil(other: CustomDate): Int {
        val thisDate = java.time.LocalDate.of(year, monthInt, day)
        val otherDate = java.time.LocalDate.of(other.year, other.monthInt, other.day)
        return java.time.Period.between(thisDate, otherDate).days
    }
}

