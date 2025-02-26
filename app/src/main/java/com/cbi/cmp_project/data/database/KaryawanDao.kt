package com.cbi.cmp_project.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.KemandoranModel
import com.cbi.cmp_project.data.model.TransporterModel
import com.google.android.gms.common.api.Status

@Dao
abstract class KaryawanDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertAll(karyawan: List<KaryawanModel>)

    @Query("DELETE FROM karyawan")
    abstract fun deleteAll()

    @Transaction
    open suspend fun updateOrInsertKaryawan(karyawan: List<KaryawanModel>) {

        val count = getCount()
        if (count > 0) {
            deleteAll()
        }
        insertAll(karyawan)
    }

    @Query("SELECT COUNT(*) FROM karyawan")
    abstract suspend fun getCount(): Int

    @Query("SELECT * FROM karyawan WHERE kemandoran_id = :filteredId")
    abstract fun getKaryawanByCriteria(
        filteredId: Int
    ): List<KaryawanModel>

    @Query(
        """
    SELECT * FROM karyawan 
    WHERE id IN (:idKaryawan)
    """
    )
    abstract fun getPemuatByIdList(
        idKaryawan: List<String>,
    ): List<KaryawanModel>

    @Query(
        """
        UPDATE karyawan 
        SET date_absen = :date_absen
        AND status_absen = :status_absen
        WHERE id IN (:idKaryawan)
        """
    )
    abstract suspend fun updateKaryawan(
        date_absen: String,
        status_absen: String,
        idKaryawan: List<String>
    ): Int
}
