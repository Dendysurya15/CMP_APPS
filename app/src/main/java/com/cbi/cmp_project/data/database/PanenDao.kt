package com.cbi.cmp_project.data.database

import androidx.room.*
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.PanenEntity

@Dao
abstract class PanenDao {

    @Insert
    abstract fun insert(panen: List<PanenEntity>)

    @Update
    abstract fun update(panen: List<PanenEntity>)

    @Delete
    abstract fun deleteAll(panen: List<PanenEntity>)

    @Query("SELECT * FROM panen_table WHERE id = :id")
    abstract fun getById(id: Int): PanenEntity?

    @Query("SELECT * FROM panen_table")
    abstract fun getAll(): List<PanenEntity>

    @Query("SELECT * FROM panen_table WHERE archive = 1")
    abstract fun getAllArchived(): List<PanenEntity>

    @Query("SELECT * FROM panen_table WHERE archive = 0")
    abstract fun getAllActive(): List<PanenEntity>

    @Query("DELETE FROM panen_table WHERE id = :id")
    abstract fun deleteByID(id: Int): Int

    @Query("DELETE FROM panen_table WHERE id IN (:id)")
    abstract fun deleteByListID(id: List<Int>): Int

    @Query("UPDATE panen_table SET archive = 1 WHERE id = :id")
    abstract fun archiveByID(id: Int): Int

    @Query("UPDATE panen_table SET archive = 1 WHERE id IN (:id)")
    abstract fun archiveByListID(id: List<Int>): Int

    @Transaction
    open fun updateOrInsert(panen: List<PanenEntity>) {
        deleteAll(panen)
        insert(panen)
    }
}