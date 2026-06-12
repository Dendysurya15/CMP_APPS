package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.cbi.mobile_plantation.data.model.PemanenFaceEntity

@Dao
interface PemanenFaceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: PemanenFaceEntity)

    @Query("SELECT * FROM pemanen_face")
    suspend fun getAll(): List<PemanenFaceEntity>

    @Query("SELECT COUNT(*) FROM pemanen_face")
    suspend fun getCount(): Int

    @Query("DELETE FROM pemanen_face WHERE karyawan_id = :karyawanId")
    suspend fun deleteByKaryawanId(karyawanId: Int)
}
