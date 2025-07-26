/*
 * (C) Copyright Elektrobit Automotive GmbH
 * All rights reserved
 */

package com.example.eventmanagement

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EventDao {
    @Query("SELECT * FROM events WHERE name = :name ORDER BY timestamp DESC")
    suspend fun getEventsByName(name: String): List<Event>

    @Query("SELECT DISTINCT name FROM events ORDER BY name")
    suspend fun getAllNames(): List<String>

    @Query("SELECT * FROM events WHERE date = :date AND time = :time")
    suspend fun getEventsAtDateTime(date: String, time: String): List<Event>

    @Insert
    suspend fun insertEvent(event: Event)

    @Insert
    suspend fun insertEvents(events: List<Event>)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Update
    suspend fun updateEvent(event: Event)
}