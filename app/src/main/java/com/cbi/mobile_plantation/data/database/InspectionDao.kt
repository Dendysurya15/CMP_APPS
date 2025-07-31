package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend abstract fun insertInspectionDetails(details: List<InspectionDetailModel>)

    @Update
    abstract suspend fun update(inspections: List<InspectionModel>)

    @Query("SELECT * FROM ${AppUtils.DatabaseTables.INSPEKSI} WHERE id = :id")
    abstract suspend fun getById(id: Int): InspectionModel?

    @Query("""
    SELECT * FROM ${AppUtils.DatabaseTables.INSPEKSI}
    WHERE (:datetime IS NULL OR strftime('%Y-%m-%d', created_date) = :datetime)
    AND (:isPushedToServer IS NULL OR isPushedToServer = :isPushedToServer)
    ORDER BY created_date DESC
""")
    abstract suspend fun getInspectionData(
        datetime: String? = null,
        isPushedToServer: Int? = null
    ): List<InspectionWithDetailRelations>

    @Query("""
    SELECT * FROM ${AppUtils.DatabaseTables.INSPEKSI}
    WHERE id = :inspectionId
""")
    abstract suspend fun getInspectionById(
        inspectionId: String
    ): List<InspectionWithDetailRelations>

    @Query("SELECT * FROM inspeksi WHERE created_date = :createdDate AND tph_id = :tphId AND dept_abbr = :deptAbbr AND divisi_abbr = :divisiAbbr LIMIT 1")
    abstract suspend fun getDataInspeksi(createdDate: String, tphId: Int, deptAbbr: String?, divisiAbbr: String?): InspectionModel?

    @Query("UPDATE inspeksi SET status_upload = :status WHERE id IN (:ids)")
    abstract suspend fun updateStatusUploadInspeksiPanen(ids: List<Int>, status: Int)

    @Query("UPDATE inspeksi_detail SET status_upload = :status WHERE id IN (:ids)")
    abstract suspend fun updateStatusUploadInspeksiDetailPanen(ids: List<Int>, status: Int)

    @Query("UPDATE inspeksi SET dataIsZipped = :status WHERE id IN (:ids)")
    abstract suspend fun updateDataIsZippedHP(ids: List<Int>, status: Int)

    @Query("""
    SELECT COUNT(*) FROM ${AppUtils.DatabaseTables.INSPEKSI}
    WHERE (:datetime IS NULL OR strftime('%Y-%m-%d', created_date) = :datetime)
    AND (:isPushedToServer IS NULL OR isPushedToServer = :isPushedToServer)
""")
    abstract suspend fun getInspectionCount(
        datetime: String? = null,
        isPushedToServer: Int? = null
    ): Int


    @Query("UPDATE inspeksi SET status_uploaded_image = :status WHERE id IN (:ids)")
    abstract suspend fun updateStatusUploadedImageInspeksi(ids: List<Int>, status: String): Int

    @Query("UPDATE inspeksi_detail SET status_uploaded_image = :status WHERE id IN (:ids)")
    abstract suspend fun updateStatusUploadedImageInspeksiDetail(ids: List<Int>, status: String): Int

    @Transaction
    @Query("SELECT * FROM inspeksi")
    abstract fun getTPHHasBeenInspect(): List<InspectionModel>

    // Helper method for transaction-based insert with result handling
    suspend fun insertWithTransaction(inspection: InspectionModel): Result<Long> {
        return try {
            val id = insertInspection(inspection)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    @Insert
    abstract suspend fun insert(inspection: InspectionModel): Long

    @Query("""
    UPDATE inspeksi SET
    tracking_path_pemulihan = :tracking_path_pemulihan,
    inspeksi_putaran = :inspeksi_putaran,
    updated_date_start = :updated_date_start,
    updated_date_end = :updated_date_end,
    foto_user_pemulihan = :foto_user_pemulihan,
    updated_by = :updated_by,
    updated_name = :updated_name,
    app_version_pemulihan = :app_version_pemulihan
    WHERE id = :inspectionId
""")
    abstract suspend fun updateInspectionForFollowUp(
        inspectionId: Int,
        tracking_path_pemulihan: String?,
        inspeksi_putaran: Int,
        updated_date_start: String,
        updated_date_end: String,
        updated_by: String,
        updated_name: String,
        foto_user_pemulihan : String,
        app_version_pemulihan:String
    )

}