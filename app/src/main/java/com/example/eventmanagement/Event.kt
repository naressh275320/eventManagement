package com.example.eventmanagement

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "events")
data class Event(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val date: String,
    val time: String,
    val timestamp: Long = System.currentTimeMillis()
)

// Data class to hold selected names and their assigned times
data class NameTimeSelection(
    val name: String,
    var time: String = "",
    var isSelected: Boolean = false
)
