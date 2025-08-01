package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.data.model.BlokModel
import com.cbi.mobile_plantation.data.model.MillModel

@Dao
abstract class BlokDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(blok: List<BlokModel>)

    @Transaction
    open suspend fun updateOrInsertBlok(blok: List<BlokModel>) {

        val count = getCount()
        if (count > 0) {
            deleteAll()
        }
        insertAll(blok)
    }

    @Query("SELECT * FROM blok")
    abstract suspend fun getAll(): List<BlokModel>

    @Query("DELETE FROM blok")
    abstract fun deleteAll()

    @Query("SELECT COUNT(*) FROM blok")
    abstract suspend fun getCount(): Int

    @Query("DELETE FROM blok")
    abstract suspend fun dropAllData()


    @Query(
        """
    SELECT * FROM blok
    WHERE id_ppro IN (:idListBlok)
    
    """
    )
    abstract  fun getDataByIdInBlok(
        idListBlok: List<Int>
    ): List<BlokModel>
}