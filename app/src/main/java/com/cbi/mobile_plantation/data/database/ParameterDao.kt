package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.mobile_plantation.data.model.MillModel
import com.cbi.mobile_plantation.data.model.ParameterModel

@Dao
abstract class ParameterDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertParameter(mill: List<ParameterModel>)

    @Transaction
    open suspend fun updateOrInsertParameter(parameter: List<ParameterModel>) {

        val count = getCount()
        if (count > 0) {
            deleteAll()
        }
        insertParameter(parameter)
    }

    @Query("SELECT param_val FROM parameter WHERE id LIKE '%kode_inspeksi%'")
    abstract suspend fun getParameterInspeksiJson(): String?

    @Query("DELETE FROM parameter")
    abstract fun deleteAll()

    @Query("SELECT COUNT(*) FROM parameter")
    abstract suspend fun getCount(): Int

}