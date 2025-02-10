package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.markertph.data.model.BlokModel
import com.cbi.markertph.data.model.DeptModel

@Dao
abstract class KemandoranDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(wilayah: List<KemandoranModel>)

    @Query("DELETE FROM kemandoran")
    abstract fun deleteAll()

    @Transaction
    open suspend fun updateOrInsertKemandoran(kemandoran: List<KemandoranModel>) {
        val count = getCount()
        if (count > 0) {
            deleteAll()
        }
        insertAll(kemandoran)
    }

    @Query("SELECT COUNT(*) FROM kemandoran")
    abstract suspend fun getCount(): Int

    @Query(
        """
    SELECT * FROM kemandoran 
    WHERE dept = :idEstate 
    AND (
        (divisi IS NOT NULL AND divisi != '' AND divisi IN (:idDivisiArray))
        OR 
        (divisi IS NULL OR divisi = '') AND dept_abbr = :estateAbbr
    )
    """
    )
    abstract fun getKemandoranByCriteria(
        idEstate: Int,
        idDivisiArray: List<Int>,
        estateAbbr: String
    ): List<KemandoranModel>

}
