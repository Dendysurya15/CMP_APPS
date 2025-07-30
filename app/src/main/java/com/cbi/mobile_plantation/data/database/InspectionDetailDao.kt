package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cbi.mobile_plantation.data.model.InspectionDetailModel
import com.cbi.mobile_plantation.utils.AppUtils

@Dao
abstract class InspectionDetailDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(details: List<InspectionDetailModel>): List<Long>

    @Insert
    abstract suspend fun insert(detail: InspectionDetailModel): Long

    @Update
    abstract suspend fun update(details: List<InspectionDetailModel>)

    @Query("SELECT * FROM ${AppUtils.DatabaseTables.INSPEKSI_DETAIL} WHERE id = :id")
    abstract suspend fun getById(id: Int): InspectionDetailModel?

    @Query("SELECT * FROM ${AppUtils.DatabaseTables.INSPEKSI_DETAIL} WHERE id_inspeksi = :inspectionId")
    abstract suspend fun getByInspectionId(inspectionId: String): List<InspectionDetailModel>

    @Query("DELETE FROM ${AppUtils.DatabaseTables.INSPEKSI_DETAIL} WHERE id = :id")
    abstract suspend fun deleteById(id: Int)

    @Query("DELETE FROM ${AppUtils.DatabaseTables.INSPEKSI_DETAIL} WHERE id_inspeksi = :inspectionId")
    abstract suspend fun deleteByInspectionId(inspectionId: String)

    // In InspectionDetailDao
    @Query("SELECT * FROM inspeksi_detail WHERE created_date = :createdDate AND nik = :nik AND nama = :nama AND kode_inspeksi = :kodeInspeksi AND temuan_inspeksi = :temuanInspeksi LIMIT 1")
    abstract suspend fun getDataInspeksiDetail(
        createdDate: String,
        nik: String,
        nama: String,
        kodeInspeksi: Int,
        temuanInspeksi: Double
    ): InspectionDetailModel?


    @Query("""
    UPDATE inspeksi_detail SET 
    temuan_inspeksi = :temuanInspeksi,
    foto_pemulihan = :fotoPemulihan,
    komentar_pemulihan = :komentarPemulihan,
    latPemulihan = :latPemulihan,
    lonPemulihan = :lonPemulihan,
    status_pemulihan = :statusPemulihan,
    updated_date = :updatedDate,
    updated_name = :updatedName,
    updated_by = :updatedBy
    WHERE id = :inspectionDetailId
""")
    abstract suspend fun updateInspectionDetailForFollowUpById(
        inspectionDetailId: Int,
        temuanInspeksi: Double,
        fotoPemulihan: String?,
        komentarPemulihan: String?,
        latPemulihan: Double,
        lonPemulihan: Double,
        statusPemulihan : Int,
        updatedDate: String,
        updatedName: String,
        updatedBy: String
    )
    // Helper method for transaction-based insert with result handling
    suspend fun insertWithTransaction(detail: InspectionDetailModel): Result<Long> {
        return try {
            val id = insert(detail)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}