package com.mobdeve.s17.abary.inorafael.mco2

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mobdeve.s17.abary.inorafael.mco2.databinding.EditPlantBinding
import com.mobdeve.s17.abary.inorafael.mco2.databinding.FilterSidePanelBinding
import com.mobdeve.s17.abary.inorafael.mco2.databinding.MainpageBinding

class FilterPlantActivity: AppCompatActivity() {

    private lateinit var binding: FilterSidePanelBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = FilterSidePanelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.applyBtn.setOnClickListener {
            finish()
        }

        binding.clearBtn.setOnClickListener {
            binding.sortRadioGroup.clearCheck()
            binding.filterWaterTodayCb.isChecked = false
            binding.filterWaterWeekCb.isChecked = false
            binding.filterPastDueCb.isChecked = false
            binding.filterFavsCb.isChecked = false
        }
    }

    override fun onResume(){
        super.onResume()
        val sp: SharedPreferences = this.getSharedPreferences("FLORPAL_FILTEROPTIONS", Context.MODE_PRIVATE)
        binding.filterFavsCb.isChecked = sp.getBoolean("FLORPAL_FAVORITES", false)
        binding.filterPastDueCb.isChecked = sp.getBoolean("FLORPAL_PASTDUE", false)
        binding.filterWaterTodayCb.isChecked = sp.getBoolean("FLORPAL_WATERTODAY", false)
        binding.filterWaterWeekCb.isChecked = sp.getBoolean("FLORPAL_WATERTHISWEEK", false)

        binding.sortRadioGroup.check(sp.getInt("FLORPAL_RADIOBUTTON", -1))


    }
    override fun onPause(){
        super.onPause()
        val sp: SharedPreferences = this.getSharedPreferences("FLORPAL_FILTEROPTIONS", Context.MODE_PRIVATE)

        val editor: SharedPreferences.Editor = sp.edit()
        editor.putBoolean("FLORPAL_FAVORITES", binding.filterFavsCb.isChecked)
        editor.putBoolean("FLORPAL_PASTDUE", binding.filterPastDueCb.isChecked)
        editor.putBoolean("FLORPAL_WATERTODAY", binding.filterWaterTodayCb.isChecked)
        editor.putBoolean("FLORPAL_WATERTHISWEEK", binding.filterWaterWeekCb.isChecked)
        editor.putInt("FLORPAL_RADIOBUTTON", binding.sortRadioGroup.checkedRadioButtonId)
        editor.apply()
    }
}