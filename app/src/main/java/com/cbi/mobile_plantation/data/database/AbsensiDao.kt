package com.cbi.mobile_plantation.data.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import com.cbi.mobile_plantation.data.model.AbsensiKemandoranRelations
import com.cbi.mobile_plantation.data.model.AbsensiModel
import com.cbi.mobile_plantation.data.model.ESPBEntity
import com.cbi.mobile_plantation.utils.AppLogger
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
    @Query("SELECT * FROM absensi WHERE archive = 0 AND date(date_absen) = date('now', 'localtime')")
    abstract fun getAllDataAbsensi(): List<AbsensiKemandoranRelations>

    @Delete
    abstract fun deleteAll(espb: List<AbsensiModel>)

    @Query("DELETE FROM absensi")
    abstract suspend fun dropAllData()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAbsensiData(absensiData: AbsensiModel)

    @Query("SELECT COUNT(*) FROM absensi WHERE archive = 1 AND date(date_absen) = date('now', 'localtime')")
    abstract suspend fun getCountArchiveAbsensi(): Int

    @Query("SELECT COUNT(*) FROM absensi WHERE archive = 0 AND date(date_absen) = date('now', 'localtime')")
    abstract suspend fun getCountAbsensi(): Int

    @Transaction
    @Query("SELECT * FROM absensi WHERE archive = 0 AND date(date_absen) = date('now', 'localtime')")
    abstract fun getAllActiveAbsensiWithRelations(): List<AbsensiKemandoranRelations>

    @Transaction
    @Query("SELECT * FROM absensi WHERE archive = 1 AND date(date_absen) = date('now', 'localtime')")
    abstract fun getAllArchivedAbsensiWithRelations(): List<AbsensiKemandoranRelations>

    @Query("UPDATE absensi SET archive = 1 WHERE id = :id")
    abstract fun archiveAbsensiByID(id: Int): Int

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