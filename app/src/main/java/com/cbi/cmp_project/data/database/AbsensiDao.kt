package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.cmp_project.data.model.AbsensiModel
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.utils.AppLogger

@Dao
abstract class AbsensiDao {


    @Insert
    abstract fun insert(espb: AbsensiModel): Long

    //    @Query("SELECT * FROM espb_table WHERE  DATE(created_at) = DATE('now', 'localtime')")
    @Query("SELECT * FROM absensi")
    abstract fun getAllDataAbsensi(): List<AbsensiModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAbsensiData(absensiData: AbsensiModel)

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