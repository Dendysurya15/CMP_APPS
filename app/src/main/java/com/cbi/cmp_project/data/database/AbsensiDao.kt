package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Transaction
import com.cbi.cmp_project.data.model.AbsensiModel
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.utils.AppLogger

@Dao
abstract class AbsensiDao {


    @Insert
    abstract fun insert(espb: AbsensiModel): Long

    @Transaction
    open suspend fun insertWithTransaction(espb: AbsensiModel): Result<Long> {
        return try {
            val id = insert(espb)  // Uses the single entity insert
            Result.success(id)
        } catch (e: Exception) {
            e.printStackTrace()
            AppLogger.e("insertWithTransaction failed", e.toString())
            Result.failure(e)
        }
    }

}