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

    @Query("UPDATE panen_table SET dataIsZipped = :status WHERE id IN (:ids)")
    abstract  suspend fun updateDataIsZippedPanen(ids: List<Int>, status: Int)

    @Query("UPDATE panen_table SET archive = 1 WHERE id IN (:id)")
    abstract fun archiveByListID(id: List<Int>): Int

    @Transaction
    @Query("SELECT * FROM panen_table WHERE archive = 0 AND status_espb = 0")
    abstract fun getAllActiveWithRelations(): List<PanenEntityWithRelations>

    @Transaction
    @Query("SELECT * FROM panen_table WHERE dataIsZipped = 0 AND status_espb = 0")
    abstract fun getAllActivePanenESPBWithRelations(): List<PanenEntityWithRelations>

    @Transaction
    @Query("SELECT * FROM panen_table WHERE archive != 1")
    abstract fun getAllTPHHasBeenSelected(): List<PanenEntityWithRelations>

    @Transaction
    @Query("SELECT * FROM panen_table WHERE status_espb = :status")
    abstract fun getAllAPanenRestan(status: Int = 0): List<PanenEntityWithRelations>

    @Transaction
    @Query("SELECT * FROM panen_table WHERE no_espb = :no_espb")
    abstract fun getAllPanenWhereESPB(no_espb: String): List<PanenEntityWithRelations>
    // Add this to your PanenDao
    @Query("UPDATE panen_table SET status_espb = :status, no_espb = :no_espb WHERE id IN (:ids)")
    abstract suspend fun updateESPBStatusByIds(ids: List<Int>, status: Int, no_espb: String): Int

    @Query("UPDATE panen_table SET status_pengangkutan = :status WHERE id IN (:ids)")
    abstract suspend fun panenUpdateStatusAngkut(ids: List<Int>, status: Int): Int
}