package com.cbi.cmp_project.data.database

import androidx.room.*
import com.cbi.cmp_project.data.model.PanenEntity

@Dao
interface PanenDao {

    @Insert
    suspend fun insert(panen: PanenEntity)

    @Update
    suspend fun update(panen: PanenEntity)

    @Delete
    suspend fun delete(panen: PanenEntity)

    @Query("SELECT * FROM panen_table WHERE id = :id")
    suspend fun getById(id: Int): PanenEntity?

    @Query("SELECT * FROM panen_table")
    suspend fun getAll(): List<PanenEntity>
}