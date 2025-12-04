package com.mobdeve.s17.abary.inorafael.mco2


import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class PlantViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val ivPlantPhoto: ImageView = itemView.findViewById(R.id.ivPlantPhoto)
    private val tvPlantName: TextView = itemView.findViewById(R.id.tvPlantName)

    fun bindData(plant: PlantModel) {
        tvPlantName.text = plant.plantName

        if (!plant.plantPhoto.isNullOrBlank()) {
            val uri = plant.plantPhoto.toUri()
            ivPlantPhoto.setImageURI(uri)
        }
    }
}
