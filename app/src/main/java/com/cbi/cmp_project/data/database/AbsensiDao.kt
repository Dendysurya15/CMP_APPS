package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SupportSQLiteQuery
import com.cbi.cmp_project.data.model.AbsensiKemandoranRelations
import com.cbi.cmp_project.data.model.AbsensiModel
import com.cbi.cmp_project.data.model.ESPBEntity
import com.cbi.cmp_project.data.model.PanenEntityWithRelations
import com.cbi.cmp_project.utils.AppLogger

@Dao
abstract class AbsensiDao {


    @Insert
    abstract fun insert(espb: AbsensiModel): Long

    //    @Query("SELECT * FROM espb_table WHERE  DATE(created_at) = DATE('now', 'localtime')")
    @Query("SELECT * FROM absensi")
    abstract fun getAllDataAbsensi(): List<AbsensiKemandoranRelations>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAbsensiData(absensiData: AbsensiModel)

    @Query("""
    SELECT COUNT(*) FROM absensi 
    WHERE date_absen = :dateAbsen 
    AND (
        EXISTS (
            SELECT 1 FROM absensi 
            WHERE date_absen = :dateAbsen 
            AND (
                ',' || karyawan_msk_id || ',' LIKE '%,' || :karyawanMskId || ',%'
            )
        )
    )
""")
    abstract fun checkIfExists(dateAbsen: String, karyawanMskId: String): Int

    @Transaction
    open suspend fun insertWithTransaction(espb: AbsensiModel): Result<Long> {
        return try {
            val id = insert(espb)  // Uses the single entity insert
            Result.success(id)
        } catch (e: Exception) {
            e.printStackTrace()
            AppLogger.e("insertWithTransaction failed", e.toString())
            Result.failure(e)
        }
    }

}