package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.mobile_plantation.data.model.TPHBlokInfo
import com.cbi.markertph.data.model.TPHNewModel
import com.cbi.mobile_plantation.data.repository.DatasetRepository
import com.cbi.mobile_plantation.utils.AppLogger


data class DepartmentInfo(
    val dept: String,
    val dept_abbr: String
)


data class TPHBlokInfo(
    val tphNomor: String,
    val blokKode: String,
    val blokId: Int  // Add this field
)

@Dao
abstract class TPHDao {

    @Query("SELECT luas_area FROM tph WHERE id = :id")
    abstract suspend fun getLuasAreaByTphId(id: Int): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(tph: List<TPHNewModel>)

    @Query("DELETE FROM tph")
    abstract fun deleteAll()

    // New method to delete TPH records by department ID
    @Query("DELETE FROM tph WHERE dept = :deptId")
    abstract suspend fun deleteByDept(deptId: Int)

    // New method to check if records for a specific department exist
    @Query("SELECT COUNT(*) FROM tph WHERE dept = :deptId")
    abstract suspend fun getCountByDept(deptId: Int): Int

    @Transaction
    open suspend fun updateOrInsertTPH(tph: List<TPHNewModel>) {
        AppLogger.d("TPH Upsert - Starting with ${tph.size} records")

        if (tph.isEmpty()) return

        val startTime = System.currentTimeMillis()
        val batchSize = 2000 // Optimal for 100K records
        val batches = tph.chunked(batchSize)

        var totalProcessed = 0

        try {
            for ((batchIndex, batch) in batches.withIndex()) {
                // This single line does everything: UPDATE existing or INSERT new
                upsertBatch(batch)

                totalProcessed += batch.size
                val progress = (totalProcessed * 100.0 / tph.size).toInt()
                AppLogger.d("Batch ${batchIndex + 1}/${batches.size} - $totalProcessed/${tph.size} ($progress%)")
            }

            val totalTime = System.currentTimeMillis() - startTime
            AppLogger.d("SUCCESS: ${tph.size} records in ${totalTime}ms")

        } catch (e: Exception) {
            AppLogger.e("FAILED: ${e.message}")
            throw e
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun upsertBatch(tph: List<TPHNewModel>)

    @Query("SELECT * FROM tph WHERE blok = :blockId LIMIT 1")
    abstract suspend fun getTPHByBlockId(blockId: Int): TPHNewModel?

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

    @Query(
        """
    SELECT 
        nomor as tphNomor,
        blok_kode as blokKode,
        blok as blokId
    FROM tph
    WHERE id = :id
"""
    )
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

    @Query(
        """
SELECT * FROM tph 
WHERE dept = :idEstate 
AND divisi = :idDivisi
AND id IN (:tphIds)
"""
    )
    abstract fun getLatLonByDivisiAndTPHIds(
        idEstate: Int,
        idDivisi: Int,
        tphIds: List<Int>
    ): List<TPHNewModel>

    // If you need the values separately, keep these queries as well
    @Query("SELECT nomor FROM tph WHERE id = :id")
    abstract suspend fun getNomorTPHbyId(id: Int): String?

    @Query("SELECT blok_kode FROM tph WHERE id = :id")
    abstract suspend fun getBlokKode(id: Int): String?

    @Query("SELECT blok FROM tph WHERE id = :id")
    abstract suspend fun getBlokIdbyIhTph(id: Int): Int?

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
        idEstate: Int, idDivisi: Int, tahunTanam: String, idBlok: Int
    ): List<TPHNewModel>

    @Query("SELECT dept_abbr, divisi_abbr, blok, blok_ppro FROM tph WHERE id = :id")
    abstract suspend fun getTPHDetailsByID(id: Int): TPHDetails?

    data class TPHDetails(
        val dept_abbr: String?,
        val divisi_abbr: String?,
    )

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
    abstract fun getBlokById(
        idListBlok: List<Int>
    ): List<TPHNewModel>

    @Query("SELECT * FROM tph WHERE id IN (:tphIds)")
    abstract suspend fun getTPHsByIds(tphIds: List<Int>): List<TPHNewModel>

    //get blok_kode by tphid
    @Query("SELECT blok_kode FROM tph WHERE id = :tphId")
    abstract suspend fun getBlokKodeByTphId(tphId: Int): String?

}