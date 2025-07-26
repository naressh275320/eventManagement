package com.example.eventmanagement

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Delete // If you want to delete names later

@Dao
interface NameDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE) // IGNORE if name already exists
    suspend fun insertName(name: Name)

    @Query("SELECT personName FROM names_table ORDER BY personName ASC")
    suspend fun getAllNames(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM names_table WHERE personName = :name LIMIT 1)")
    suspend fun nameExists(name: String): Boolean

    @Delete
    suspend fun deleteName(name: Name) // For future name deletion functionality
}