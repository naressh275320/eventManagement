/*
 * (C) Copyright Elektrobit Automotive GmbH
 * All rights reserved
 */

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
