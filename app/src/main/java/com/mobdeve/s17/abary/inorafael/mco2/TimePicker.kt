package com.mobdeve.s17.abary.inorafael.mco2

import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import java.util.*
import kotlin.text.format

// can be changed
// if you have suggestions/know a better alternative feel
// free to change datetime picker stuff
class TimePicker(private val onTimeSelected: (String) -> Unit) : DialogFragment(), TimePickerDialog.OnTimeSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val c = Calendar.getInstance()
        return TimePickerDialog(
            activity,
            this,
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE),
            DateFormat.is24HourFormat(activity))
    }

    override fun onTimeSet(view: TimePicker, hourOfDay: Int, minute: Int) {
        val formatted = String.format("%02d:%02d", hourOfDay, minute)
        onTimeSelected(formatted)
    }
}
