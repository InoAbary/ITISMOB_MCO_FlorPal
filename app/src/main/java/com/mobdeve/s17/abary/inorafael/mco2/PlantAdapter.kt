package com.mobdeve.s17.abary.inorafael.mco2

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

class PlantAdapter(private val data: ArrayList<PlantModel>) :
    RecyclerView.Adapter<PlantViewHolder>() {

    override fun getItemCount(): Int {
        // Return 0 if list is empty to avoid divide-by-zero
        return if (data.isEmpty()) 0 else Int.MAX_VALUE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.plants_layout, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        if (data.isEmpty()) return // safety check
        val actualPosition = position % data.size
        holder.bindData(data[actualPosition])
    }
}
