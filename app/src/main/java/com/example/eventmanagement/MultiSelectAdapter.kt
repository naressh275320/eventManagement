/*
 * (C) Copyright Elektrobit Automotive GmbH
 * All rights reserved
 */

package com.example.eventmanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView


class MultiSelectAdapter(
    private val nameTimeSelections: MutableList<NameTimeSelection>,
    private val onTimeClick: (Int) -> Unit // Callback for time selection
) : RecyclerView.Adapter<MultiSelectAdapter.ViewHolder>() {

    // Inner ViewHolder class
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val checkBox: CheckBox = itemView.findViewById(R.id.nameCheckBox)
        val timeButton: Button = itemView.findViewById(R.id.timeSelectButton)
        // If you had a separate TextView for name, it would be here too
        // val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
    }

    // Called when RecyclerView needs a new ViewHolder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.multi_select_item, parent, false)
        return ViewHolder(view)
    }

    // Called to bind data to an existing ViewHolder
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val selection = nameTimeSelections[position]

        holder.checkBox.text = selection.name
        holder.checkBox.isChecked = selection.isSelected

        holder.timeButton.text = if (selection.time.isEmpty()) "Select Time" else "Time: ${selection.time}"
        holder.timeButton.isEnabled = selection.isSelected

        // Set up listeners for CheckBox and Button
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            selection.isSelected = isChecked
            holder.timeButton.isEnabled = isChecked // Enable/disable time button based on checkbox
            if (!isChecked) {
                selection.time = "" // Clear time if unchecked
                holder.timeButton.text = "Select Time"
            }
            // Notify adapter that data set might have changed, for a full refresh if needed
            // notifyDataSetChanged() // Can be inefficient, use specific notify methods if possible
        }

        holder.timeButton.setOnClickListener {
            // Only allow time selection if the checkbox is checked
            if (selection.isSelected) {
                onTimeClick(holder.adapterPosition) // Use adapterPosition for correct position
            }
        }
    }

    // Return the total number of items in the data set
    override fun getItemCount(): Int = nameTimeSelections.size

    // Method to update data and notify adapter (optional but good practice)
    fun updateData(newData: List<NameTimeSelection>) {
        nameTimeSelections.clear()
        nameTimeSelections.addAll(newData)
        notifyDataSetChanged() // Reloads the entire list
    }
}