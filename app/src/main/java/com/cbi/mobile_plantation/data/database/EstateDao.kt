package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.mobile_plantation.data.model.EstateModel

@Dao
abstract class EstateDao {

    @Transaction
    open suspend fun updateOrInsertEstate(estate: List<EstateModel>) {
        val count = getCount()
        if (count > 0) {
            deleteAll()
        }
        insertAll(estate)
    }

    @Query("SELECT COUNT(*) FROM estate")
    abstract suspend fun getCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(estate: List<EstateModel>)

    @Query("DELETE FROM estate")
    abstract fun deleteAll()

    // In EstateDao.kt
    @Query("SELECT * FROM estate")
    abstract suspend fun getAllEstates(): List<EstateModel>
}