package com.cbi.cmp_project.data.database

import androidx.room.*
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.PanenEntity
import com.cbi.cmp_project.utils.AppLogger

@Dao
abstract class ESPBDao {
    @Insert
    abstract fun insert(espb: List<ESPBEntity>)

    @Update
    abstract fun update(espb: List<ESPBEntity>)

    @Delete
    abstract fun deleteAll(espb: List<ESPBEntity>)

    @Query("SELECT * FROM espb_table WHERE id = :id")
    abstract fun getById(id: Int): ESPBEntity?

    @Query("SELECT * FROM espb_table")
    abstract fun getAll(): List<ESPBEntity>

    @Query("SELECT * FROM espb_table WHERE  DATE(created_at) = DATE('now', 'localtime')")
    abstract fun getAllESPBUploaded(): List<ESPBEntity>


    @Query("SELECT * FROM espb_table WHERE archive = 1")
    abstract fun getAllArchived(): List<ESPBEntity>

    @Query("SELECT * FROM espb_table WHERE archive = 0")
    abstract fun getAllActive(): List<ESPBEntity>

    @Query("DELETE FROM espb_table WHERE id = :id")
    abstract fun deleteByID(id: Int): Int

    @Query("DELETE FROM espb_table WHERE id IN (:id)")
    abstract fun deleteByListID(id: List<Int>): Int

    @Query("UPDATE espb_table SET archive = 1 WHERE id = :id")
    abstract fun archiveByID(id: Int): Int

    @Query("UPDATE espb_table SET archive = 1 WHERE id IN (:id)")
    abstract fun archiveByListID(id: List<Int>): Int

    @Transaction
    open fun updateOrInsert(espb: List<ESPBEntity>) {

        insert(espb)
    }

    @Insert
    abstract fun insert(espb: ESPBEntity): Long


    @Transaction
    open suspend fun insertWithTransaction(espb: ESPBEntity): Result<Long> {
        return try {
            val id = insert(espb)  // Uses the single entity insert
            Result.success(id)
        } catch (e: Exception) {
            e.printStackTrace()
            AppLogger.e("insertWithTransaction failed", e.toString())
            Result.failure(e)
        }
    }

    @Query("SELECT COUNT(*) FROM espb_table WHERE  DATE(created_at) = DATE('now', 'localtime')")
    abstract suspend fun countESPBUploaded(): Int

}
