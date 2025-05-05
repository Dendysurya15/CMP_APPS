package com.cbi.mobile_plantation.data.database

import androidx.room.*
import com.cbi.mobile_plantation.data.model.ESPBEntity
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations

@Dao
abstract class ESPBDao {
    @Insert
    abstract fun insert(espb: List<ESPBEntity>)

    @Update
    abstract fun update(espb: List<ESPBEntity>)

    @Delete
    abstract fun deleteAll(espb: List<ESPBEntity>)

    @Query("SELECT * FROM espb_table WHERE id = :id")
    abstract fun getById(id: Int): ESPBEntity?

    @Query("SELECT * FROM espb_table")
    abstract fun getAll(): List<ESPBEntity>

    //    @Query("SELECT * FROM espb_table WHERE  DATE(created_at) = DATE('now', 'localtime')")
    @Query("SELECT * FROM espb_table where scan_status = 1")
    abstract fun getAllESPBUploaded(): List<ESPBEntity>

    @Query("SELECT * FROM espb_table WHERE archive = 1")
    abstract fun getAllArchived(): List<ESPBEntity>

    @Query("SELECT * FROM espb_table WHERE status_upload = 0")
    abstract fun getAllActive(): List<ESPBEntity>

    @Query("SELECT * FROM espb_table WHERE dataIsZipped = 0 AND id IN (:ids)")
    abstract fun getActiveESPBByIds(ids: List<Int>): List<ESPBEntity>

    @Query("DELETE FROM espb_table WHERE id = :id")
    abstract fun deleteByID(id: Int): Int

    @Query("DELETE FROM espb_table WHERE id IN (:id)")
    abstract fun deleteByListID(id: List<Int>): Int

    @Query("UPDATE espb_table SET archive = 1 WHERE id = :id")
    abstract fun archiveByID(id: Int): Int

    @Query("UPDATE espb_table SET archive = 1 WHERE id IN (:id)")
    abstract fun archiveByListID(id: List<Int>): Int

    @Query("SELECT COUNT(*) FROM espb_table WHERE status_draft = 1 AND scan_status = 0")
    abstract fun getCountDraft(): Int

    @Query("UPDATE espb_table SET status_upload = :status WHERE id IN (:ids)")
    abstract suspend fun updateStatusUploadEspb(ids: List<Int>, status: Int)

    @Transaction
    open fun updateOrInsert(espb: List<ESPBEntity>) {

        insert(espb)
    }

    // In your DAO interface
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract  suspend fun insertAndGetId(espbData: ESPBEntity): Long

    @Insert
    abstract fun insert(espb: ESPBEntity): Long

    @Query("SELECT COUNT(*) FROM espb_table WHERE noESPB = :noESPB")
    abstract suspend fun isNoESPBExists(noESPB: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertESPBData(espbData: ESPBEntity)

    @Query("SELECT COUNT(*) FROM espb_table where scan_status = 1")
    abstract suspend fun countESPBUploaded(): Int

    @Query(
        """
    UPDATE espb_table 
    SET uploader_info_wb = :uploaderInfoWb, 
        uploaded_by_id_wb = :uploadedByIdWb, 
        uploaded_at_ppro_wb = :uploadedAtWb,
        uploaded_ppro_response = :response,
        status_upload_ppro_wb = :status 
    WHERE id = :id
"""
    )
    abstract suspend fun updateUploadStatusPPRO(
        id: Int,
        status: Int,
        uploaderInfoWb: String,
        uploadedAtWb: String,
        uploadedByIdWb: Int,
        response:String?
    ): Int

    @Query(
        """
    UPDATE espb_table 
    SET uploader_info_wb = :uploaderInfoWb, 
        uploaded_by_id_wb = :uploadedByIdWb, 
        uploaded_at_wb = :uploadedAtWb, 
        uploaded_wb_response = :response,
        status_upload_cmp_wb = :status
    WHERE id = :id
"""
    )
    abstract suspend fun updateUploadStatusCMP(
        id: Int,
        status: Int,
        uploaderInfoWb: String,
        uploadedAtWb: String,
        uploadedByIdWb: Int,
        response:String?
    ): Int


    @Query("DELETE FROM espb_table")
    abstract suspend fun dropAllData()


    @Query("UPDATE espb_table SET dataIsZipped = :status WHERE id IN (:ids)")
    abstract suspend fun updateDataIsZippedESPB(ids: List<Int>, status: Int)

    @Query("SELECT * FROM espb_table WHERE scan_status = 0")
    abstract fun getAllESPBNonScan(): List<ESPBEntity>

//    @Query("SELECT * FROM espb_table")
//    abstract fun getAllESPBS(): List<ESPBEntity>

    @Query("""
    SELECT * FROM espb_table 
    WHERE (:date IS NULL OR strftime('%Y-%m-%d', created_at) = :date)
""")
    abstract suspend fun getAllESPBS(date: String? = null): List<ESPBEntity>


    @Query("SELECT COUNT(*) FROM espb_table WHERE strftime('%Y-%m-%d', created_at) = strftime('%Y-%m-%d', 'now', 'localtime')")
    abstract suspend fun getCountCreatedToday(): Int



}
