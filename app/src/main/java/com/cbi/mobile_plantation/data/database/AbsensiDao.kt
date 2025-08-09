package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SupportSQLiteQuery
import com.cbi.mobile_plantation.data.model.AbsensiKemandoranRelations
import com.cbi.mobile_plantation.data.model.AbsensiModel
import com.cbi.mobile_plantation.data.model.ESPBEntity
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.ui.adapter.AbsensiDataRekap
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils
import com.github.junrar.Archive

@Dao
abstract class AbsensiDao {


    @Insert
    abstract fun insert(absensi: AbsensiModel): Long

    //    @Query("SELECT * FROM espb_table WHERE  DATE(created_at) = DATE('now', 'localtime')")
    @Query("SELECT * FROM absensi WHERE archive = 0 AND status_scan == :status_scan AND date(date_absen) = date('now', 'localtime')")
    abstract fun getAllDataAbsensi(status_scan:Int): List<AbsensiKemandoranRelations>

    @Query("SELECT * FROM absensi WHERE status_scan == :status_scan")
    abstract fun getAllData(status_scan:Int): List<AbsensiKemandoranRelations>

    @Query("""
    SELECT * FROM absensi 
    WHERE (:date IS NULL OR strftime('%Y-%m-%d', date_absen) = :date)
    AND archive = :archive
""")
    abstract suspend fun getAllRekapAbsensi(date: String? = null, archive: Int): List<AbsensiKemandoranRelations>

    @Delete
    abstract fun deleteAll(espb: List<AbsensiModel>)

    @Query("DELETE FROM absensi")
    abstract suspend fun dropAllData()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAbsensiData(absensiData: AbsensiModel)

    @Query("UPDATE absensi SET status_upload = :status WHERE id IN (:ids)")
    abstract suspend fun updateStatusUploadAbsensiPanen(ids: List<Int>, status: Int)

    @Query("UPDATE absensi SET status_uploaded_image = :status WHERE id IN (:ids)")
    abstract suspend fun updateStatusUploadedImage(ids: List<Int>, status: String): Int

//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    abstract suspend fun insertAbsensiDataLokal(absensiDataLokal: AbsensiModelScan)

    @Query("SELECT COUNT(*) FROM absensi WHERE archive = 1 AND status_scan == :load_status_scan AND date(date_absen) = date('now', 'localtime')")
    abstract suspend fun getCountArchiveAbsensi(load_status_scan: Int): Int

    @Query("SELECT COUNT(*) FROM absensi WHERE archive = 0 AND date(date_absen) = date('now', 'localtime')")
    abstract suspend fun getCountAbsensi(): Int

    @Transaction
    @Query("SELECT * FROM absensi WHERE archive = 0 AND date(date_absen) = date('now', 'localtime')")
    abstract fun getAllActiveAbsensiWithRelations(): List<AbsensiKemandoranRelations>

    @Query("DELETE FROM absensi WHERE id IN (:id)")
    abstract fun deleteByListID(id: List<Int>): Int

    @Query("""
    UPDATE ${AppUtils.DatabaseTables.ABSENSI}
    SET 
        karyawan_msk_id = :mskId,
        karyawan_msk_nama = :mskNama,
        karyawan_msk_nik = :mskNik,
        karyawan_msk_work_location = :mskWork,
        karyawan_tdk_msk_id = :tdkMskId,
        karyawan_tdk_msk_nama = :tdkMskNama,
        karyawan_tdk_msk_nik = :tdkMskNik,
        karyawan_tdk_msk_work_location = :tdkMskWork,
        status_upload = :statusUpload
    WHERE id = :id
""")
    abstract suspend fun updateAbsensiFields(
        id: Int,
        mskId: String,
        mskNama: String,
        mskNik: String,
        mskWork: String,
        tdkMskId: String,
        tdkMskNama: String,
        tdkMskNik: String,
        tdkMskWork: String,
        statusUpload: Int
    )

    @Transaction
    @Query("SELECT * FROM absensi WHERE archive = 1 AND date(date_absen) = date('now', 'localtime')")
    abstract fun getAllArchivedAbsensiWithRelations(): List<AbsensiKemandoranRelations>

    @Query("UPDATE absensi SET archive = 1 WHERE id = :id")
    abstract fun archiveAbsensiByID(id: Int): Int

    @Query("UPDATE absensi SET dataIsZipped = :status WHERE id IN (:ids)")
    abstract  suspend fun updateDataIsZippedAbsensi(ids: List<Int>, status: Int)

    @Query("""
    SELECT COUNT(*) FROM absensi 
    WHERE date_absen = :dateAbsen 
    AND (
        EXISTS (
            SELECT 1 FROM absensi 
            WHERE date_absen = :dateAbsen 
            AND (
                ',' || karyawan_msk_id || ',' LIKE '%,' || :karyawanMskId || ',%'
            )
        )
    )
""")
    abstract fun checkIfExists(dateAbsen: String, karyawanMskId: String): Int

    @Transaction
    open suspend fun insertWithTransaction(absensi: AbsensiModel): Result<Long> {
        return try {
            val id = insert(absensi)  // Uses the single entity insert
            Result.success(id)
        } catch (e: Exception) {
            e.printStackTrace()
            AppLogger.e("insertWithTransaction failed", e.toString())
            Result.failure(e)
        }
    }
}