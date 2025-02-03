package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.markertph.data.model.WilayahModel

@Dao
abstract class WilayahDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(wilayah: List<WilayahModel>)

    @Query("DELETE FROM wilayah")
    abstract fun deleteAll()

    @Transaction
    open fun updateOrInsertWilayah(wilayah: List<WilayahModel>) {

        insertAll(wilayah)
    }
}
