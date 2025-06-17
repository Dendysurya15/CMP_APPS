package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.mobile_plantation.data.model.InspectionDetailModel
import com.cbi.mobile_plantation.data.model.InspectionModel
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations

@Dao
abstract class InspectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(data: List<InspectionModel>): List<Long>

    @Insert
    suspend abstract fun insertInspection(inspection: InspectionModel): Long

    @Insert
    suspend abstract fun insertInspectionDetails(details: List<InspectionDetailModel>)

    @Transaction
    @Query("SELECT * FROM inspeksi")
    abstract fun getTPHHasBeenInspect(): List<InspectionModel>
}