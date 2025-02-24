package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.cmp_project.data.model.MillModel
import com.cbi.markertph.data.model.TPHNewModel

@Dao
abstract class MillDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(mill: List<MillModel>)

    @Transaction
    open suspend fun updateOrInsertMill(mill: List<MillModel>) {

        val count = getCount()
        if (count > 0) {
            deleteAll()
        }
        insertAll(mill)
    }

    //get all mill
    @Query("SELECT * FROM mill")
    abstract suspend fun getAll(): List<MillModel>

    @Query("DELETE FROM mill")
    abstract fun deleteAll()


    @Query("SELECT COUNT(*) FROM mill")
    abstract suspend fun getCount(): Int

    @Query(
        """
    SELECT * FROM mill 
    WHERE id = :idMill
    """
    )
    abstract fun getMillById(
        idMill: Int,
    ): List<MillModel>
}