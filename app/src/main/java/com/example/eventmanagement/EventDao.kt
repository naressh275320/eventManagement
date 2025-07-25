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

    @Insert
    suspend fun insertEvent(event: Event)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Update
    suspend fun updateEvent(event: Event)

    @Query("DELETE FROM events WHERE date < :currentDate")
    suspend fun deleteExpiredEvents(currentDate: String)

    @Query("SELECT * FROM events WHERE name = :name AND date >= :currentDate ORDER BY timestamp DESC")
    suspend fun getFutureEventsByName(name: String, currentDate: String): List<Event>
}
