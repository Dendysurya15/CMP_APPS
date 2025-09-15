package com.cbi.mobile_plantation.data.database

import androidx.room.*
import com.cbi.mobile_plantation.data.model.InspectionWithDetailRelations
import com.cbi.mobile_plantation.data.model.PanenEntity
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.data.model.TPHNewModel
import com.cbi.mobile_plantation.utils.AppLogger
import com.cbi.mobile_plantation.utils.AppUtils

@Dao
abstract class PanenDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(panen: PanenEntity): Long // Returns row ID of inserted item

    @Transaction
    open suspend fun insertWithTransaction(panen: PanenEntity): Result<Long> {
        return try {
            val id = insert(panen)
            AppLogger.d("insertWithTransaction success: ID = $id") // Debug log
            Result.success(id)
        } catch (e: Exception) {
            e.printStackTrace() // Print stack trace
            AppLogger.e("insertWithTransaction failed", e.toString())
            Result.failure(e)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertBatch(entities: List<PanenEntity>): List<Long>


    @Query("""
        SELECT * FROM ${AppUtils.DatabaseTables.PANEN} 
        WHERE tph_id = :tphId 
        AND date_created = :createdDate 
        AND created_by = :createdBy 
        AND no_espb = :spbKode 
        AND ancak = :ancak 
        LIMIT 1
    """)
    abstract suspend fun checkExistingRecord(
        tphId: String,
        createdDate: String,
        createdBy: Int,
        spbKode: String,
        ancak: Int
    ): PanenEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM panen_table WHERE tph_id = :tphId AND date_created = :dateCreated)")
    abstract suspend fun exists(tphId: String, dateCreated: String): Boolean

    @Query("SELECT * FROM panen_table WHERE tph_id = :tphId AND date_created = :dateCreated LIMIT 1")
    abstract suspend fun existsModel(tphId: String, dateCreated: String): PanenEntityWithRelations?

    @Query("SELECT id FROM panen_table WHERE tph_id = :tphId AND date_created = :dateCreated LIMIT 1")
    abstract suspend fun getIdByTphIdAndDateCreated(tphId: String, dateCreated: String): Int

    @Query("DELETE FROM panen_table WHERE id = :id")
    abstract suspend fun deleteById(id: Int)

    @Update
    abstract fun update(panen: List<PanenEntity>)

    @Delete
    abstract fun deleteAll(panen: List<PanenEntity>)

    @Query("DELETE FROM panen_table")
    abstract suspend fun dropAllData()

    @Query("SELECT * FROM panen_table WHERE id = :id")
    abstract fun getById(id: Int): PanenEntity?

    @Query("SELECT COUNT(*) FROM panen_table WHERE archive = 0 AND status_espb = 0 AND date(date_created) = date('now', 'localtime') AND scan_status = 0")
    abstract suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM panen_table WHERE archive_transfer_inspeksi = 0 AND status_espb = 0 AND date(date_created) = date('now', 'localtime') AND status_scan_inspeksi = 0")
    abstract suspend fun getCountForTransferInspeksi(): Int

    @Query("""
    SELECT COUNT(*) FROM panen_table 
    WHERE archive = :archive 
    AND status_espb = :statusEspb 
    AND scan_status = :scanStatus
    AND (:date IS NULL OR strftime('%Y-%m-%d', date_created) = :date)
""")
    abstract suspend fun getCountTPHESPB(archive: Int, statusEspb: Int, scanStatus: Int, date: String?): Int


    @Query("""
    UPDATE panen_table 
    SET status_espb = 0,
        no_espb = '',
        status_pengangkutan = 0
    WHERE tph_id = :tphId 
      AND date_created = :dateCreated 
      AND jjg_json = :kpJson 
      AND nomor_pemanen = :nomorPemanen
""")
    abstract suspend fun resetEspbStatus(
        tphId: String,
        dateCreated: String,
        kpJson: String,
        nomorPemanen: String
    ): Int

    @Query("""
    UPDATE panen_table 
    SET status_espb = 1,
        no_espb = :noEspb,
        status_pengangkutan = 2
    WHERE tph_id = :tphId 
      AND date_created = :dateCreated 
      AND jjg_json = :kpJson 
      AND nomor_pemanen = :nomorPemanen
""")
    abstract suspend fun setEspbStatus(
        tphId: String,
        dateCreated: String,
        kpJson: String,
        nomorPemanen: String,
        noEspb: String
    ): Int




    @Query("""
    SELECT * FROM ${AppUtils.DatabaseTables.PANEN}
    WHERE (:date IS NULL OR strftime('%Y-%m-%d', date_created) = :date)
    AND archive_transfer_inspeksi = :archive_transfer_inspeksi
""")
    abstract suspend fun getPanenForTransferInspeksi(
        date: String? = null,
        archive_transfer_inspeksi: Int? = null
    ): List<PanenEntityWithRelations>

    @Query("""
    SELECT COUNT(*) FROM panen_table 
    WHERE (:date IS NULL OR strftime('%Y-%m-%d', date_created) = :date)
    AND archive_transfer_inspeksi = :archive_transfer_inspeksi
""")
    abstract suspend fun getCountPanenForTransferInspeksi(
        date: String? = null,
        archive_transfer_inspeksi: Int
    ): Int

    @Query("SELECT COUNT(*) FROM panen_table WHERE archive = 1 AND status_espb = 0 AND date(date_created) = date('now', 'localtime')")
    abstract suspend fun getCountArchive(): Int

    @Query("""
    SELECT COUNT(*) 
    FROM panen_table p
    INNER JOIN tph t ON p.tph_id = t.id
    WHERE p.archive = 0 
    AND p.status_transfer_restan = 0 
    AND date(p.date_created) = date('now', 'localtime') 
    AND (p.no_espb IS NULL OR p.no_espb = '' OR p.no_espb = 'NULL')
    AND p.scan_status = 1
    AND t.divisi = :afdelingId
""")
    abstract suspend fun getCountApprovalByAfdeling(afdelingId: Int): Int


    @Query("""
    SELECT * FROM panen_table 
    WHERE archive = :archive
    AND (no_espb IS NULL OR no_espb = '' OR no_espb = 'NULL') = :hasNoEspb
    AND status_transfer_restan = :statusTransferRestan
    AND scan_status = :scanStatus
    AND (:date IS NULL OR strftime('%Y-%m-%d', date_created) = :date)
    ORDER BY date_created DESC
""")
    abstract suspend fun loadESPB(archive: Int, statusTransferRestan: Int, hasNoEspb: Boolean, scanStatus: Int, date: String?): List<PanenEntityWithRelations>

    @Query("""
    SELECT COUNT(*) FROM panen_table 
    WHERE archive = :archive 
    AND (no_espb IS NULL OR no_espb = '' OR no_espb = 'NULL') = :hasNoEspb
    AND status_transfer_restan = :statusTransferRestan
    AND scan_status = :scanStatus
    AND (:date IS NULL OR strftime('%Y-%m-%d', date_created) = :date)
""")
    abstract suspend fun countESPB(archive: Int, statusTransferRestan: Int, hasNoEspb: Boolean, scanStatus: Int, date: String?): Int

    @Query("""
    UPDATE panen_table
     
    SET karyawan_id = :karyawanId,
        karyawan_nik = :karyawanNik,
        karyawan_nama = :karyawanNama,
        kemandoran_id = :kemandoranId
    WHERE id = :id
""")
    abstract suspend fun updatePemanenWorkers(
        id: Int,
        karyawanId: String,
        karyawanNik: String,
        karyawanNama: String,
        kemandoranId: String
    )

    @Query("SELECT * FROM panen_table")
    abstract fun getAll(): List<PanenEntity>

    @Query("SELECT * FROM panen_table WHERE archive = 1")
    abstract fun getAllArchived(): List<PanenEntity>

    @Transaction
    @Query("SELECT * FROM panen_table WHERE archive = 1 ")
    abstract fun getAllArchivedWithRelations(): List<PanenEntityWithRelations>

    @Query("SELECT * FROM panen_table WHERE archive = 0")
    abstract fun getAllActive(): List<PanenEntity>

    @Query("DELETE FROM panen_table WHERE id = :id")
    abstract fun deleteByID(id: Int): Int

    @Query("DELETE FROM panen_table WHERE id IN (:id)")
    abstract fun deleteByListID(id: List<Int>): Int

    @Query("UPDATE panen_table SET archive = 1 WHERE id = :id")
    abstract fun archiveByID(id: Int): Int

    @Query("UPDATE panen_table SET status_transfer_restan = 1 WHERE id = :id")
    abstract fun changeStatusTransferRestan(id: Int): Int

    @Query("UPDATE panen_table SET archive_transfer_inspeksi = 1 WHERE id = :id")
    abstract fun changeStatusTransferInspeksiPanen(id: Int): Int

    @Query("UPDATE panen_table SET archive_mpanen = 1 WHERE id = :id")
    abstract fun archiveMpanenByID(id: Int): Int

    @Query("UPDATE panen_table SET dataIsZipped = :status WHERE id IN (:ids)")
    abstract  suspend fun updateDataIsZippedPanen(ids: List<Int>, status: Int)

    @Query("UPDATE panen_table SET archive = 1 WHERE id IN (:id)")
    abstract fun archiveByListID(id: List<Int>): Int

    @Query("UPDATE panen_table SET status_upload = :status WHERE id IN (:ids)")
    abstract suspend fun updateStatusUploadPanen(ids: List<Int>, status: Int)

    @Transaction
    @Query("SELECT * FROM panen_table WHERE archive = 0 AND status_espb = 0")
    abstract fun getAllActiveWithRelations(): List<PanenEntityWithRelations>

    @Transaction
    @Query("SELECT * FROM panen_table WHERE status_upload = 0 AND status_espb = 0")
    abstract fun getAllActivePanenESPBWithRelations(): List<PanenEntityWithRelations>

    @Query(
        """
    SELECT 
        id,
        blok_ppro
    FROM tph
    WHERE id = :id
"""
    )
    abstract suspend fun getTPHBlokPpro(id: Int): TPHNewModel?

    @Transaction
    @Query("""
    SELECT * FROM panen_table 
    WHERE status_espb = 0 
    AND status_scan_mpanen = 0 
    AND status_transfer_restan = 0 
    AND isPushedToServer = 0
    AND status_upload = 0
    AND date(date_created) BETWEEN date('now', 'localtime', '-7 days') AND date('now', 'localtime')
""")
    abstract fun getAllActivePanenESPBAll(): List<PanenEntityWithRelations>

    @Transaction
    @Query("SELECT * FROM panen_table WHERE archive != 1")
    abstract fun getAllTPHHasBeenSelected(): List<PanenEntityWithRelations>

    @Query("""
    SELECT p.* FROM panen_table p
    INNER JOIN tph t ON p.tph_id = t.id
    WHERE datetime(p.date_created) >= datetime('now', '-7 days')
    AND p.karyawan_nik IS NOT NULL 
    AND p.karyawan_nik != ''
    AND p.karyawan_nik != 'NULL'
    AND p.karyawan_nama IS NOT NULL 
    AND p.karyawan_nama != ''
    AND p.karyawan_nama != 'NULL'
    AND t.dept = :estateId
""")
    abstract fun getAllTPHinWeek(estateId: Int): List<PanenEntityWithRelations>

    @Transaction
    @Query("SELECT * FROM panen_table WHERE scan_status = 0 and status_restan = 0")
    abstract fun getAllPanenForInspection(): List<PanenEntityWithRelations>

    @Transaction
    @Query("SELECT * FROM panen_table WHERE status_espb = :status")
    abstract fun getAllAPanenRestan(status: Int = 0): List<PanenEntityWithRelations>

    // Add this to your PanenDao
    @Query("UPDATE panen_table SET status_espb = :status, no_espb = :no_espb WHERE id IN (:ids)")
    abstract suspend fun updateESPBStatusByIds(ids: List<Int>, status: Int, no_espb: String): Int

    @Query("UPDATE panen_table SET status_pengangkutan = :status WHERE id IN (:ids)")
    abstract suspend fun panenUpdateStatusAngkut(ids: List<Int>, status: Int): Int

    @Query("UPDATE panen_table SET status_uploaded_image = :status WHERE id IN (:ids)")
    abstract suspend fun updateStatusUploadedImage(ids: List<Int>, status: String): Int

    @Transaction
    @Query("SELECT * FROM panen_table WHERE date(date_created) < date(:cutoffDate)")
    abstract suspend fun getPanenOlderThan(cutoffDate: String): List<PanenEntityWithRelations>

    //getall where status_scan_mpanen = 0
    @Query("SELECT * FROM panen_table WHERE status_scan_mpanen = :status_scan_mpanen")
    abstract fun getAllScanMPanen(status_scan_mpanen: Int = 0): List<PanenEntity>

    @Query("SELECT * FROM panen_table WHERE archive_mpanen = :status_scan_mpanen AND strftime('%Y-%m-%d', date_created) = :date")
    abstract fun getAllScanMPanenByDateWithFilter(status_scan_mpanen: Int, date: String): List<PanenEntityWithRelations>

    @Query("SELECT * FROM panen_table WHERE tph_id = :tphId AND date_created = :dateCreated LIMIT 1")
    abstract  suspend fun findByTphAndDate(tphId: String, dateCreated: String): PanenEntity?

    // Update status_scan_inspeksi
    @Query("UPDATE panen_table SET status_scan_inspeksi = :status WHERE id = :id")
    abstract  suspend fun updateScanInspeksiStatus(id: Int, status: Int): Int

    @Query("SELECT * FROM panen_table WHERE archive_mpanen = :status_scan_mpanen")
    abstract fun getAllScanMPanenWithoutDateFilter(status_scan_mpanen: Int): List<PanenEntityWithRelations>

    // Then create a wrapper function to handle the logic
    fun getAllScanMPanenByDate(archiveMpanen: Int, date: String? = null): List<PanenEntityWithRelations> {
        return if (date == null) {
            getAllScanMPanenWithoutDateFilter(archiveMpanen)
        } else {
            getAllScanMPanenByDateWithFilter(archiveMpanen, date)
        }
    }

    //count where status_scan_mpanen = 0 date now
    @Query("SELECT COUNT(*) FROM panen_table WHERE archive_mpanen = :archive_mpanen AND date(date_created) = date('now', 'localtime')")
    abstract suspend fun getCountScanMPanen(archive_mpanen: Int): Int

}