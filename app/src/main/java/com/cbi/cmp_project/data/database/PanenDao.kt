package com.cbi.cmp_project.data.database

import androidx.room.*
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.PanenEntity
import com.cbi.cmp_project.data.model.PanenEntityWithRelations

@Dao
abstract class PanenDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(panen: PanenEntity): Long // Returns row ID of inserted item

    @Transaction
    open suspend fun insertWithTransaction(panen: PanenEntity): Result<Long> {
        return try {
            val id = insert(panen)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @Update
    abstract fun update(panen: List<PanenEntity>)

    @Delete
    abstract fun deleteAll(panen: List<PanenEntity>)

    @Query("SELECT * FROM panen_table WHERE id = :id")
    abstract fun getById(id: Int): PanenEntity?

    @Query("SELECT COUNT(*) FROM panen_table")
    abstract suspend fun getCount(): Int

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
    @Query("SELECT * FROM panen_table WHERE archive = 0")
    abstract  fun getAllActiveWithRelations(): List<PanenEntityWithRelations>

//    @Transaction
//    open fun updateOrInsert(panen: List<PanenEntity>) {
//
//        insert(panen)
//    }
}