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
    abstract fun insertAll(dept: List<DeptModel>)

    @Query("DELETE FROM dept")
    abstract fun deleteAll()

    @Query("SELECT * FROM dept WHERE  id = :estateId")
    abstract fun getDeptByCriteria(estateId: String): List<DeptModel>

    @Transaction
    open fun updateOrInsertDept(dept: List<DeptModel>) {

        insertAll(dept)
    }


}
