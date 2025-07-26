package com.example.eventmanagement

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete
import androidx.room.Update

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<Event>)

    // This query gets all distinct names from the Event table
    @Query("SELECT DISTINCT name FROM events ORDER BY name ASC")
    suspend fun getAllNames(): List<String>

    // Get future events for a specific name
    @Query("SELECT * FROM events WHERE name = :name AND date >= :currentDate ORDER BY date ASC, time ASC")
    suspend fun getFutureEventsByName(name: String, currentDate: String): List<Event>

    // New query to get ALL events (you'll filter for "future" in the Activity)
    @Query("SELECT * FROM events")
    suspend fun getAllEvents(): List<Event>

    // Delete expired events
    @Query("DELETE FROM events WHERE date < :currentDate")
    suspend fun deleteExpiredEvents(currentDate: String)

    @Delete
    suspend fun deleteEvent(event: Event)

    @Query("SELECT * FROM events WHERE id = :eventId")
    suspend fun getEventById(eventId: Int): Event?

    @Update
    suspend fun updateEvent(event: Event)
}