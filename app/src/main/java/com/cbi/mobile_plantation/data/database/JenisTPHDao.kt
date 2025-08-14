package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.mobile_plantation.data.model.JenisTPHModel

import com.cbi.mobile_plantation.data.model.MillModel
import com.cbi.mobile_plantation.utils.AppUtils

@Dao
abstract class JenisTPHDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(jenisTPH: List<JenisTPHModel>)

    @Transaction
    open suspend fun updateOrInsertJenisTPH(jenisTPH: List<JenisTPHModel>) {

        val count = getCount()
        if (count > 0) {
            deleteAll()
        }
        insertAll(jenisTPH)
    }

    @Query("SELECT * FROM jenis_tph")
    abstract suspend fun getAll(): List<JenisTPHModel>

    @Query("DELETE FROM jenis_tph")
    abstract fun deleteAll()

    @Query("SELECT COUNT(*) FROM jenis_tph")
    abstract suspend fun getCount(): Int

    @Query("SELECT * FROM " + AppUtils.DatabaseTables.JENIS_TPH)
    abstract suspend fun getAllJenisTPH(): List<JenisTPHModel>
}