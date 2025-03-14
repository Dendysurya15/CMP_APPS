package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.mobile_plantation.data.model.TransporterModel

@Dao
abstract class TransporterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(transporter: List<TransporterModel>)

    @Transaction
    open suspend fun InsertTransporter(transporter: List<TransporterModel>) {

        val count = getCount()
        if (count > 0) {
            deleteAll()
        }
        insertAll(transporter)
    }

    @Query("DELETE FROM transporter")
    abstract fun deleteAll()

    @Query("SELECT COUNT(*) FROM transporter")
    abstract suspend fun getCount(): Int

    @Query(
        """
    SELECT * FROM transporter 
    WHERE id = :idTransporter
    """
    )
    abstract fun getTransporterById(
        idTransporter: Int,
    ): List<TransporterModel>

    @Query("SELECT nama FROM transporter WHERE id = :transporterId LIMIT 1")
    abstract suspend fun getTransporterNameById(transporterId: Int): String?
}