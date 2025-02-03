package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.markertph.data.model.WilayahModel

@Dao
abstract class TPHDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(tph: List<TPHNewModel>)

    @Query("DELETE FROM wilayah")
    abstract fun deleteAll()

    @Transaction
    open fun updateOrInsertTPH(tph: List<TPHNewModel>) {

        insertAll(tph)
    }

    @Query(
        """
    SELECT * FROM tph 
    WHERE regional = :idRegional 
    AND dept = :idEstate 
    AND (
        (divisi IS NOT NULL AND divisi != '' AND divisi = :idDivisi)
        OR 
        (divisi IS NULL OR divisi = '') AND dept_abbr = :estateAbbr
    )
    AND tahun = :tahunTanam 
    AND blok = :idBlok
    """
    )
    abstract fun getTPHByCriteria(
        idRegional:Int, idEstate: Int, idDivisi:Int, estateAbbr :String,tahunTanam : String,  idBlok :Int
    ): List<TPHNewModel>
}