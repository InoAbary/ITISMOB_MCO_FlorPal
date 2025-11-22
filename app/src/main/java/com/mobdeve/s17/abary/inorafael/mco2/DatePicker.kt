package com.mobdeve.s17.abary.inorafael.mco2

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import java.util.*
import kotlin.text.format

// can be changed
// if you have suggestions/know a better alternative feel
// free to change datetime picker stuff
class DatePicker(private val onDateSelected: (String) -> Unit) : DialogFragment(), DatePickerDialog.OnDateSetListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val c = Calendar.getInstance()
        return DatePickerDialog(
            requireContext(),
            this,
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH))
    }

    override fun onDateSet(view: DatePicker, year: Int, month: Int, day: Int) {
        val formatted = String.format("%02d/%02d/%04d", month + 1, day, year)
        onDateSelected(formatted)
    }
}
