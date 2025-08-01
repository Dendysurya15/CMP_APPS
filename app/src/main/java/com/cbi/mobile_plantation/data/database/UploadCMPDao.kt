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

    @Query("SELECT COUNT(*) FROM upload_cmp WHERE tracking_id = :trackingId and nama_file =:nama_file")
    abstract suspend fun getTrackingIdCount(trackingId: String, nama_file: String): Int

    @Query("UPDATE upload_cmp SET status = :status WHERE tracking_id = :trackingId")
    abstract suspend fun updateStatus(trackingId: String, status: Int)

    @Query("SELECT table_ids FROM upload_cmp WHERE tracking_id = :trackingId")
    abstract suspend fun getTableIdsByTrackingId(trackingId: String): String?

    @Query("DELETE FROM upload_cmp")
    abstract suspend fun dropAllData()

    @Query("SELECT * FROM upload_cmp")
    abstract suspend fun getAllData(): List<UploadCMPModel>

    @Query("""
    DELETE FROM upload_cmp 
    WHERE table_ids LIKE '%' || :absensiId || '%'
       AND table_ids LIKE '%"absensi"%'
""")
    abstract suspend fun deleteUploadCmpByAbsensiId(absensiId: Int): Int

}