package com.cbi.cmp_project.data.database

import androidx.room.*
import com.cbi.cmp_project.data.model.ESPBEntity

@Dao
interface EspbDao {
    @Insert
    suspend fun insert(user: ESPBEntity)

    @Update
    suspend fun update(user: ESPBEntity)

    @Delete
    suspend fun delete(user: ESPBEntity)

    @Query("SELECT * FROM espb_table")
    suspend fun getAllEntries(): List<ESPBEntity>

    @Query("SELECT * FROM espb_table WHERE id = :id")
    suspend fun getEntryById(id: Int): ESPBEntity?

    @Query("DELETE FROM espb_table WHERE id = :id")
    suspend fun deleteById(id: Int)
}
