package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cbi.cmp_project.data.model.InspectionPathModel

@Dao
abstract class InspectionPathDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(path: InspectionPathModel): Long

    @Query("DELETE FROM inspeksi_path WHERE id = :id")
    abstract fun deleteByID(id: String): Int
}