package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.mobile_plantation.data.model.TPHBlokInfo
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.data.repository.DatasetRepository


data class DepartmentInfo(
    val dept: String,
    val dept_abbr: String
)

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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTPHAsistensi(tph: List<TPHNewModel>)

    @Query("SELECT * FROM tph WHERE dept = :idEstate GROUP BY divisi")
    abstract fun getDivisiByCriteria(idEstate: Int): List<TPHNewModel>

    //getDivisiAbbrByTphId
    @Query("SELECT divisi_abbr FROM tph WHERE id = :id")
    abstract suspend fun getDivisiAbbrByTphId(id: Int): String?

    //getDivisiAbbrByTphId
    @Query("SELECT company_abbr FROM tph WHERE id = :id")
    abstract suspend fun geCompanyAbbrByTphId(id: Int): String?

    @Query("SELECT COUNT(*) FROM tph")
    abstract suspend fun getCount(): Int

    @Query("""
        SELECT 
            nomor as tphNomor,
            blok_kode as blokKode
        FROM tph
        
        WHERE id = :id
    """)
    abstract suspend fun getTPHAndBlokInfo(id: Int): TPHBlokInfo?

    @Query("SELECT DISTINCT dept, dept_abbr FROM tph WHERE dept IS NOT NULL AND dept_abbr IS NOT NULL")
    abstract suspend fun getDistinctDeptInfo(): List<DepartmentInfo>

    @Query(
        """
    SELECT * FROM tph 
    WHERE dept = :idEstate 
    AND divisi = :idDivisi
    """
    )
    abstract fun getLatLonByDivisi(
        idEstate: Int,
        idDivisi: Int,
    ): List<TPHNewModel>

    // If you need the values separately, keep these queries as well
    @Query("SELECT nomor FROM tph WHERE id = :id")
    abstract suspend fun getTPHNomor(id: Int): String?

    @Query("SELECT blok_kode FROM tph WHERE id = :id")
    abstract suspend fun getBlokKode(id: Int): String?

    @Query(
        """
    SELECT * FROM tph 
    WHERE dept = :idEstate 
    AND  divisi = :idDivisi
    AND tahun = :tahunTanam 
    AND blok = :idBlok
    """
    )
    abstract fun getTPHByCriteria(
        idEstate: Int, idDivisi:Int, tahunTanam : String,  idBlok :Int
    ): List<TPHNewModel>

    @Query(
        """
    SELECT * FROM tph 
    WHERE dept = :idEstate 
    AND divisi = :idDivisi
    GROUP BY blok
    """
    )
    abstract fun getBlokByCriteria(
        idEstate: Int,
        idDivisi: Int,
    ): List<TPHNewModel>

    @Query(
        """
    SELECT * FROM tph
    WHERE blok IN (:idListBlok)
    GROUP BY blok
    """
    )
    abstract  fun getBlokById(
        idListBlok: List<Int>
    ): List<TPHNewModel>

    @Query("SELECT * FROM tph WHERE id IN (:tphIds)")
    abstract suspend fun getTPHsByIds(tphIds: List<Int>): List<TPHNewModel>

}