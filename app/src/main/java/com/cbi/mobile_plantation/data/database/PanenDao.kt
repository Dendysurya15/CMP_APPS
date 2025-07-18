package com.cbi.mobile_plantation.data.database

import androidx.room.*
import com.cbi.mobile_plantation.data.model.PanenEntity
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations
import com.cbi.mobile_plantation.utils.AppLogger

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

    @Query("SELECT EXISTS(SELECT 1 FROM panen_table WHERE tph_id = :tphId AND date_created = :dateCreated)")
    abstract suspend fun exists(tphId: String, dateCreated: String): Boolean

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

    @Query("""
    SELECT COUNT(*) FROM panen_table 
    WHERE archive = :archive 
    AND status_espb = :statusEspb 
    AND scan_status = :scanStatus
    AND (:date IS NULL OR strftime('%Y-%m-%d', date_created) = :date)
""")
    abstract suspend fun getCountTPHESPB(archive: Int, statusEspb: Int, scanStatus: Int, date: String?): Int


    @Query("SELECT COUNT(*) FROM panen_table WHERE archive = 1 AND status_espb = 0 AND date(date_created) = date('now', 'localtime')")
    abstract suspend fun getCountArchive(): Int

    @Query("SELECT COUNT(*) FROM panen_table WHERE archive = 0 AND status_espb = 0 AND scan_status = 1")
    abstract suspend fun getCountApproval(): Int


    @Query("""
    SELECT * FROM panen_table 
    WHERE archive = :archive 
    AND status_espb = :statusEspb 
    AND scan_status = :scanStatus
    AND (:date IS NULL OR strftime('%Y-%m-%d', date_created) = :date)
""")
    abstract suspend fun loadESPB(archive: Int, statusEspb: Int, scanStatus: Int, date: String?): List<PanenEntityWithRelations>

    @Query("""
    SELECT COUNT(*) FROM panen_table 
    WHERE archive = :archive 
    AND status_espb = :statusEspb 
    AND scan_status = :scanStatus
    AND (:date IS NULL OR strftime('%Y-%m-%d', date_created) = :date)
""")
    abstract suspend fun countESPB(archive: Int, statusEspb: Int, scanStatus: Int, date: String?): Int

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

    @Transaction
    @Query("SELECT * FROM panen_table WHERE status_espb = 0 AND status_scan_mpanen = 0 AND isPushedToServer = 0")
    abstract fun getAllActivePanenESPBAll(): List<PanenEntityWithRelations>

    @Transaction
    @Query("SELECT * FROM panen_table WHERE archive != 1")
    abstract fun getAllTPHHasBeenSelected(): List<PanenEntityWithRelations>

    @Transaction
    @Query("SELECT * FROM panen_table WHERE datetime(date_created) >= datetime('now', '-7 days')")
    abstract fun getAllTPHinWeek(): List<PanenEntityWithRelations>

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