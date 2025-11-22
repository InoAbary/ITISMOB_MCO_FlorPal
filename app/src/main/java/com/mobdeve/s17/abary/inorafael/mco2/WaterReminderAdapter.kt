package com.mobdeve.s17.abary.inorafael.mco2

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class WaterReminderAdapter(
    private var data: ArrayList<WaterReminderModel>,
    private val fullPlantList: ArrayList<PlantModel>,// added nov 5, to pass the plant model for viewplantdetails
    private val enableClick: Boolean = true, //added nov 5, to enable and disable click (can only access viewplantdetails on viewplantlist activity
    private val activity: Activity
    ): RecyclerView.Adapter<WaterReminderViewHolder>() {



    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WaterReminderViewHolder {


        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.water_reminder_layout, parent, false)


        return WaterReminderViewHolder(view)
    }

    /*
    override fun onBindViewHolder(holder: WaterReminderViewHolder, position: Int) {
        holder.bindData(data[position])
    }*/
    // edited nov 5
    override fun onBindViewHolder(holder: WaterReminderViewHolder, position: Int) {
        holder.bindData(data[position], fullPlantList, enableClick, activity)

    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun updateData(newList: ArrayList<WaterReminderModel>){
        data = newList
        notifyDataSetChanged()
    }
}