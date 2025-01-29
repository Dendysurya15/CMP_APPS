package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.markertph.data.model.BlokModel
import com.cbi.markertph.data.model.DeptModel
import com.cbi.markertph.data.model.DivisiModel

@Dao
abstract class BlokDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(wilayah: List<BlokModel>)

    @Query("DELETE FROM blok")
    abstract fun deleteAll()

    @Transaction
    open fun updateOrInsertBlok(blok: List<BlokModel>) {
        insertAll(blok)
    }

    @Query(
        """
    SELECT * FROM blok 
    WHERE regional = :idRegional 
    AND dept = :idEstate 
    AND (
        (divisi IS NOT NULL AND divisi != '' AND divisi = :idDivisi)
        OR 
        (divisi IS NULL OR divisi = '') AND dept_abbr = :estateAbbr
    )
    """
    )
    abstract fun getBlokByCriteria(
        idRegional: Int,
        idEstate: Int,
        idDivisi: Int,
        estateAbbr: String
    ): List<BlokModel>

}
