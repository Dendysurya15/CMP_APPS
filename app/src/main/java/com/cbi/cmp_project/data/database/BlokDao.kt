package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.markertph.data.model.BlokModel
import com.cbi.markertph.data.model.DeptModel

@Dao
abstract class BlokDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(wilayah: List<BlokModel>)

    @Query("DELETE FROM blok")
    abstract fun deleteAll()

    @Transaction
    open fun updateOrInsertBlok(blok: List<BlokModel>) {
        deleteAll()
        insertAll(blok)
    }
}
