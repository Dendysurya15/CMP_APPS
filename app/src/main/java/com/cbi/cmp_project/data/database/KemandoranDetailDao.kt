package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.cmp_project.data.model.KemandoranDetailModel
import com.cbi.markertph.data.model.WilayahModel

@Dao
abstract class KemandoranDetailDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(wilayah: List<KemandoranDetailModel>)

    @Query("DELETE FROM kemandoran_detail")
    abstract fun deleteAll()

    @Transaction
    open fun updateOrInsertKemandoranDetail(kemandoran_detail: List<KemandoranDetailModel>) {
        deleteAll()
        insertAll(kemandoran_detail)
    }
}
