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

    @Query("SELECT * FROM pemanen_face WHERE nik = :nik LIMIT 1")
    suspend fun getByNik(nik: String): PemanenFaceEntity?

    @Query("SELECT * FROM pemanen_face WHERE status_upload = 0")
    suspend fun getPendingUpload(): List<PemanenFaceEntity>

    @Query("UPDATE pemanen_face SET status_upload = :status WHERE nik IN (:niks)")
    suspend fun updateStatusUpload(niks: List<String>, status: Int)

    @Query("SELECT COUNT(*) FROM pemanen_face WHERE embedding LIKE :prefix")
    suspend fun getCountByEmbeddingPrefix(prefix: String): Int

    @Query("SELECT COUNT(*) FROM pemanen_face")
    suspend fun getCount(): Int

    @Query("DELETE FROM pemanen_face WHERE nik = :nik")
    suspend fun deleteByNik(nik: String)
}
