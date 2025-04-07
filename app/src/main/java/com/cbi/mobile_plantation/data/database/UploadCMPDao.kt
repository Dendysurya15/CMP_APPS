package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cbi.mobile_plantation.data.model.AbsensiModel
import com.cbi.mobile_plantation.data.model.UploadCMPModel

@Dao
abstract class UploadCMPDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertNewData(espbData: UploadCMPModel)

    @Query("SELECT COUNT(*) FROM upload_cmp WHERE tracking_id = :trackingId")
    abstract suspend fun getTrackingIdCount(trackingId: Int): Int

    @Query("UPDATE upload_cmp SET status = :status WHERE tracking_id = :trackingId")
    abstract suspend fun updateStatus(trackingId: Int, status: Int)

    @Query("SELECT table_ids FROM upload_cmp WHERE tracking_id = :trackingId")
    abstract suspend fun getTableIdsByTrackingId(trackingId: Int): String?

    @Query("DELETE FROM upload_cmp")
    abstract suspend fun dropAllData()

    @Query("SELECT * FROM upload_cmp")
    abstract suspend fun getAllData(): List<UploadCMPModel>

}