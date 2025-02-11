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
abstract class DivisiDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(divisi: List<DivisiModel>)

    @Query("DELETE FROM divisi")
    abstract fun deleteAll()

    @Transaction
    open fun updateOrInsertDivisi(divisi: List<DivisiModel>) {

        insertAll(divisi)
    }

//    @Query("SELECT * FROM blok WHERE dept = :idEstate GROUP BY divisi")
//    abstract fun getDivisiByCriteria(idEstate: Int): List<BlokModel>

}
