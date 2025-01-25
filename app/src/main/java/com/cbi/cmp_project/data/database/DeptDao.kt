package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.markertph.data.model.DeptModel
import com.cbi.markertph.data.model.WilayahModel

@Dao
abstract class DeptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(wilayah: List<DeptModel>)

    @Query("DELETE FROM dept")
    abstract fun deleteAll()

    @Transaction
    open fun updateOrInsertDept(dept: List<DeptModel>) {
        deleteAll()
        insertAll(dept)
    }
}
