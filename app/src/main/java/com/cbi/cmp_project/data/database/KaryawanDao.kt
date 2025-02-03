package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.KemandoranDetailModel
import com.cbi.cmp_project.data.model.KemandoranModel

@Dao
abstract class KaryawanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(karyawan: List<KaryawanModel>)

    @Query("DELETE FROM kemandoran")
    abstract fun deleteAll()

    @Transaction
    open fun updateOrInsertKaryawan(karyawan: List<KaryawanModel>) {

        insertAll(karyawan)
    }

    @Query("SELECT * FROM karyawan WHERE nik IN (:filteredId)")
    abstract fun getKaryawanByCriteria(
        filteredId: Array<String>
    ): List<KaryawanModel>
}
