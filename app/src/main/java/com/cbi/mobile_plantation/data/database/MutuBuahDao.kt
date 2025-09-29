package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.mobile_plantation.data.model.MutuBuahEntity
import com.cbi.mobile_plantation.data.model.MutuBuahWithRelations
import com.cbi.mobile_plantation.data.model.PanenEntityWithRelations

@Dao
interface MutuBuahDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(mutuBuah: MutuBuahEntity): Long // Returns row ID of inserted item

    @Query("SELECT COUNT(*) FROM mutu_buah WHERE tanggal = strftime('%Y-%m-%d', 'now', 'localtime')")
    abstract suspend fun getCountCreatedToday(): Int

    // In MutuBuahDao
    @Query("""
    SELECT * FROM mutu_buah 
    WHERE  date(createdDate) BETWEEN date('now', 'localtime', '-7 days') AND date('now', 'localtime')
    AND status_upload = 0
    ORDER BY tanggal DESC
""")
    abstract suspend fun getAllMutuBuah(): List<MutuBuahEntity>
    @Transaction
    @Query("SELECT * FROM mutu_buah WHERE date(createdDate) = date('now')")
    abstract fun getAllTPHHasBeenSelectedMB(): List<MutuBuahWithRelations>

    @Query("UPDATE mutu_buah SET dataIsZipped = :status WHERE id IN (:ids)")
    abstract  suspend fun updateDataIsZippedMutuBuah(ids: List<Int>, status: Int)

    @Query("UPDATE mutu_buah SET status_upload = :status WHERE id IN (:ids)")
    abstract suspend fun updateStatusUploadMutuBuah(ids: List<Int>, status: Int)

    @Query("UPDATE mutu_buah SET status_uploaded_image = :status WHERE id IN (:ids)")
    abstract suspend fun updateStatusUploadedImage(ids: List<Int>, status: String): Int

    @Query("UPDATE mutu_buah SET status_uploaded_image_selfie = :status WHERE id IN (:ids)")
    abstract suspend fun updateStatusUploadedImageSelfie(ids: List<Int>, status: String): Int

    @Query("""
        SELECT * 
        FROM mutu_buah
        WHERE status_upload = :statusUpload
          AND (:date IS NULL OR strftime('%Y-%m-%d', tanggal) = :date)
    """)
    suspend fun loadMutuBuahByStatusUploadAndDate(
        statusUpload: Int,
        date: String? = null
    ): List<MutuBuahEntity>

    @Query("""
    SELECT COUNT(*) 
    FROM mutu_buah
    WHERE status_upload = :statusUpload
      AND (:date IS NULL OR strftime('%Y-%m-%d', tanggal) = :date)
""")
    suspend fun countMutuBuahByStatusUploadAndDate(
        statusUpload: Int,
        date: String? = null
    ): Int


}