package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.mobile_plantation.data.model.InspectionPathModel
import com.cbi.mobile_plantation.data.model.PathWithInspectionTphRelations

@Dao
abstract class InspectionPathDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(path: InspectionPathModel): Long

    @Query("DELETE FROM inspeksi_path WHERE id IN (:ids)")
    abstract fun deleteByID(ids: List<String>): Int

    @Transaction
    @Query("""
        SELECT DISTINCT ip.* 
        FROM inspeksi_path ip
        LEFT JOIN inspeksi i ON ip.id = i.id_path
        WHERE i.archive = :archive
        ORDER BY i.created_date DESC
    """)
    abstract suspend fun getInspectionPathsWithTphAndCount(archive: Int): List<PathWithInspectionTphRelations>

    @Transaction
    @Query("SELECT * FROM inspeksi_path WHERE id = :pathId")
    abstract suspend fun getInspectionPathWithTphAndCount(pathId: String): PathWithInspectionTphRelations
}