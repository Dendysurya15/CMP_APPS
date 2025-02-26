package com.cbi.cmp_project.data.repository

import android.content.Context
import com.cbi.cmp_project.data.database.AppDatabase
import com.cbi.cmp_project.data.model.AbsensiModel
import com.cbi.cmp_project.data.model.KaryawanModel
import com.cbi.cmp_project.data.model.KemandoranModel

class AbsensiRepository(context: Context) {

    private val database = AppDatabase.getDatabase(context)
    private val absensiDao = database.absensiDao()
    private val karyawanDao = database.karyawanDao()
    private val kemandoranDao = database.kemandoranDao()

    suspend fun saveDataAbsensi(
        kemandoran_id: String,
        date_absen: String,
        created_by: Int,
        karyawan_msk_id: String,
        karyawan_tdk_msk_id: String,
        foto: String,
        komentar: String,
        asistensi: Int,
        lat: Double,
        lon: Double,
        info:String,
        archive: Int,
    ): Result<Long> {
        val absensiModel = AbsensiModel(
            kemandoran_id = kemandoran_id,
            date_absen = date_absen,
            created_by = created_by,
            karyawan_msk_id = karyawan_msk_id,
            karyawan_tdk_msk_id = karyawan_tdk_msk_id,
            foto = foto,
            komentar = komentar,
            asistensi = asistensi,
            lat = lat,
            lon = lon,
            info = info,
            archive = archive
        )
        return absensiDao.insertWithTransaction(absensiModel)
    }

    suspend fun updateKaryawanAbsensi(
        date_absen: String,
        status_absen: String,
        karyawan_msk_id: List<String>,
    ): Int {
        return karyawanDao.updateKaryawan(date_absen, status_absen, karyawan_msk_id)
    }

    suspend fun updateKemandoranAbsensi(
        date_absen: String,
        status_absen: String,
        kemandoran_id: String,
    ): Int {
        return kemandoranDao.updateKemandoran(date_absen, status_absen, kemandoran_id)
    }
}