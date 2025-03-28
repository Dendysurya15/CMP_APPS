package com.cbi.mobile_plantation.data.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cbi.mobile_plantation.data.model.KaryawanModel

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

    data class KaryawanKemandoranData(
        @ColumnInfo(name = "karyawan_id") val karyawanId: Int,
        @ColumnInfo(name = "nama_karyawan") val namaKaryawan: String,
        @ColumnInfo(name = "nik") val nik: String,
        @ColumnInfo(name = "kemandoran_id") val kemandoranId: Int,
        @ColumnInfo(name = "kemandoran_nama") val kemandoranNama: String,
        @ColumnInfo(name = "kode_kemandoran") val kodeKemandoran: String
    )


    @Query("""
    SELECT karyawan.id AS karyawan_id, 
           karyawan.nama AS nama_karyawan, 
           karyawan.nik AS nik,  
           karyawan.kemandoran_id, 
           kemandoran.nama AS kemandoran_nama,
           kemandoran.kode AS kode_kemandoran
    FROM karyawan 
    JOIN kemandoran ON karyawan.kemandoran_id = kemandoran.id 
    WHERE karyawan.id IN (:filteredIds)
""")
    abstract  suspend fun getKaryawanKemandoranList(filteredIds: List<String>): List<KaryawanKemandoranData>

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

