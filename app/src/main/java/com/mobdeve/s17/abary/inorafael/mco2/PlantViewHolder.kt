package com.mobdeve.s17.abary.inorafael.mco2


import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val ivPlantPhoto: ImageView = itemView.findViewById(R.id.ivPlantPhoto)
    private val tvPlantName: TextView = itemView.findViewById(R.id.tvPlantName)

    fun bindData(plant: PlantModel) {
        tvPlantName.text = plant.plantName

        ivPlantPhoto.setImageResource(plant.plantPhoto)
    }
}
