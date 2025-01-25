package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.markertph.data.model.RegionalModel

@Dao
abstract class RegionalDao {

    @Query("SELECT * FROM regional")
    abstract fun getAllRegional(): List<RegionalModel>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(regionals: List<RegionalModel>)

    @Query("DELETE FROM regional")
    abstract fun deleteAll()

    @Transaction
    open fun updateOrInsertRegional(regionals: List<RegionalModel>) {
        // Ensure the delete and insert operations run in a single transaction
        deleteAll()
        insertAll(regionals)
    }
}
