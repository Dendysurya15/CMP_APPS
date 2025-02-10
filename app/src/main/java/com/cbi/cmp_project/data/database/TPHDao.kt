package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.cmp_project.data.model.TPHBlokInfo
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.markertph.data.model.WilayahModel

@Dao
abstract class TPHDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(tph: List<TPHNewModel>)

    @Query("DELETE FROM tph")
    abstract fun deleteAll()

    @Transaction
    open suspend fun updateOrInsertTPH(tph: List<TPHNewModel>) {
        val count = getCount()
        if (count > 0) {
            deleteAll()
        }
        insertAll(tph)
    }

    @Query("SELECT COUNT(*) FROM tph")
    abstract suspend fun getCount(): Int

    @Query("""
        SELECT 
            t.nomor as tphNomor,
            b.kode as blokKode
        FROM tph t
        LEFT JOIN blok b ON t.blok = b.id
        WHERE t.id = :id
    """)
    abstract suspend fun getTPHAndBlokInfo(id: Int): TPHBlokInfo?

    // If you need the values separately, keep these queries as well
    @Query("SELECT nomor FROM tph WHERE id = :id")
    abstract suspend fun getTPHNomor(id: Int): String?

    @Query("SELECT b.kode FROM tph t LEFT JOIN blok b ON t.blok = b.id WHERE t.id = :id")
    abstract suspend fun getBlokKode(id: Int): String?

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