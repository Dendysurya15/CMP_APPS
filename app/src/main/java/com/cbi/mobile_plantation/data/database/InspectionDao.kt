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

    @Query("SELECT COUNT(DISTINCT id_path) FROM inspeksi WHERE archive = :archive AND date(created_date) = date('now', 'localtime')")
    abstract suspend fun countCard(archive: Int): Int
}