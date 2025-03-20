package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.mobile_plantation.data.model.InspectionPathModel
import com.cbi.mobile_plantation.data.model.PathWithInspectionRelations

@Dao
abstract class InspectionPathDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(path: InspectionPathModel): Long

    @Query("DELETE FROM inspeksi_path WHERE id = :id")
    abstract fun deleteByID(id: String): Int

    @Transaction
    @Query("SELECT * FROM inspeksi_path")
    abstract fun getAllSavedWithRelations(): List<PathWithInspectionRelations>
}