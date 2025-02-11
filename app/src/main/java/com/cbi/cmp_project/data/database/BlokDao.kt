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

    @Query("SELECT * FROM blok WHERE dept = :idEstate GROUP BY divisi")
    abstract fun getDivisiByCriteria(idEstate: Int): List<BlokModel>


    @Transaction
    open suspend fun updateOrInsertBlok(blok: List<BlokModel>) {
        val count = getCount()
        if (count > 0) {
            deleteAll()
        }
        insertAll(blok)
    }

    @Query("SELECT COUNT(*) FROM blok")
    abstract suspend fun getCount(): Int

    @Query(
        """
    SELECT * FROM blok 
    WHERE dept = :idEstate 
    AND divisi = :idDivisi
    """
    )
    abstract fun getBlokByCriteria(
        idEstate: Int,
        idDivisi: Int,
    ): List<BlokModel>

}
