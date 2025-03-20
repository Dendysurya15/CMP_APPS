package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cbi.mobile_plantation.data.model.InspectionModel

@Dao
abstract class InspectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(data: List<InspectionModel>): List<Long>

    @Query("DELETE FROM inspeksi WHERE id = :id")
    abstract fun deleteByID(id: String): Int

    @Query("SELECT COUNT(DISTINCT id_path) FROM inspeksi WHERE archive = 1 AND date(created_date) = date('now', 'localtime')")
    abstract suspend fun getCountUploaded(): Int
}