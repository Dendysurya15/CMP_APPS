package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.mobile_plantation.data.model.KendaraanModel
import com.cbi.mobile_plantation.data.model.TransporterModel

@Dao
abstract class KendaraanDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(transporter: List<KendaraanModel>)

    @Transaction
    open suspend fun InsertKendaraan(kendaraan: List<KendaraanModel>) {

        val count = getCount()
        if (count > 0) {
            deleteAll()
        }
        insertAll(kendaraan)
    }

    @Query("SELECT COUNT(*) FROM kendaraan")
    abstract suspend fun getCount(): Int

    @Query("DELETE FROM kendaraan")
    abstract fun deleteAll()
}