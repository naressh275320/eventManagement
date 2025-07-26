package com.example.eventmanagement

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "names_table") // Choose a distinct table name, e.g., "names_table"
data class Name(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String // Use a distinct name for clarity, e.g., personName instead of just 'name'
)