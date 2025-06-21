package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.mobile_plantation.data.model.InspectionDetailModel
import com.cbi.mobile_plantation.data.model.InspectionModel
import com.cbi.mobile_plantation.data.model.InspectionWithDetailRelations
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.utils.AppUtils

@Dao
abstract class InspectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(data: List<InspectionModel>): List<Long>

    @Insert
    suspend abstract fun insertInspection(inspection: InspectionModel): Long

    @Insert
    suspend abstract fun insertInspectionDetails(details: List<InspectionDetailModel>)

    @Query("""
        SELECT * FROM ${AppUtils.DatabaseTables.INSPEKSI}
        WHERE (:datetime IS NULL OR strftime('%Y-%m-%d', created_date_start) = :datetime)
        ORDER BY created_date_start DESC
    """)
    abstract  suspend fun getInspectionData(
        datetime: String? = null
    ): List<InspectionWithDetailRelations>

    @Query("""
    SELECT COUNT(*) FROM ${AppUtils.DatabaseTables.INSPEKSI}
    WHERE (:datetime IS NULL OR strftime('%Y-%m-%d', created_date_start) = :datetime)
""")
    abstract suspend fun getInspectionCount(
        datetime: String? = null
    ): Int


    @Transaction
    @Query("SELECT * FROM inspeksi")
    abstract fun getTPHHasBeenInspect(): List<InspectionModel>
}