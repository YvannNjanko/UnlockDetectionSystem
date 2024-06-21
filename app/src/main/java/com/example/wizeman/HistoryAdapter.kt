package com.example.wizeman

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso

class HistoryAdapter(private val dataList: List<Violation>) : RecyclerView.Adapter<HistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.findViewById(R.id.imageView)
        val timestampView: TextView = view.findViewById(R.id.timestampView)
        val locationView: TextView = view.findViewById(R.id.locationView)
        val weatherView: TextView = view.findViewById(R.id.weatherView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.history_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val violation = dataList[position]

        if (violation.imageUrl.isNotEmpty()) {
            Picasso.get().load(violation.imageUrl).into(holder.imageView)
        } else {
            // Handle empty URL case, you could set a placeholder image or hide the ImageView
            holder.imageView.setImageResource(R.drawable.placeholder_image) // Set your placeholder image here
        }

        holder.timestampView.text = violation.timestamp.toString()
        holder.locationView.text = "Lat: ${violation.latitude}, Lon: ${violation.longitude}"
        holder.weatherView.text = violation.weatherInfo
    }

    override fun getItemCount() = dataList.size
}
